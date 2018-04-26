package io.spring.gradle.bintray

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

class BintrayClient(val bintrayUser: String?, val bintrayKey: String?) {
	companion object {
		const val BINTRAY_API_URL = "https://api.bintray.com"
	}

	private val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

	/**
	 * Useful for testing
	 */
	var interceptors = emptyList<Interceptor>()

	private fun httpClient(): OkHttpClient {
		var builder = OkHttpClient.Builder()
				.connectTimeout(30, TimeUnit.SECONDS)
				.readTimeout(3, TimeUnit.MINUTES)
				.writeTimeout(6, TimeUnit.MINUTES)

		interceptors.forEach { builder = builder.addInterceptor(it) }

		return builder
				.authenticator({ _, response ->
					if (bintrayUser != null && bintrayKey != null) {
						val credential = Credentials.basic(bintrayUser, bintrayKey)
						response.request().newBuilder()
								.header("Authorization", credential)
								.build()
					} else response.request().newBuilder().build()
				})
				.build()
	}

	fun get(path: String): Response = httpClient()
			.newCall(Request.Builder().head().url("$BINTRAY_API_URL/$path").build())
			.execute()

	fun isSuccessful(path: String): Boolean = get(path).use { response -> response.isSuccessful }

	fun post(path: String, body: Any): Response {
		val bodyStr = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
				mapper.writeValueAsString(body))

		val request = Request.Builder()
				.url("$BINTRAY_API_URL/$path")
				.post(bodyStr)
				.build()

		return httpClient().newCall(request).execute()
	}

	fun upload(path: String, artifact: File, gpgPassphrase: String?): Response {
		var requestBuilder = Request.Builder()
				.header("Content-Type", "*/*")

		if (!gpgPassphrase.isNullOrBlank()) {
			requestBuilder = requestBuilder.header("X-GPG-PASSPHRASE", gpgPassphrase!!)
		}

		val request = requestBuilder
				.put(RequestBody.create(MediaType.parse("application/octet-stream"), artifact))
				.url("$BINTRAY_API_URL/$path")
				.build()

		return httpClient().newCall(request).execute()
	}
}