package io.spring.gradle.bintray

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat

object BintrayApiMatchers {
	fun pathEq(path: String, responseCode: Int = 200) = PathEqualsInterceptor(path, responseCode)
	fun <T : Any> bodyEq(body: T) = BodyEqualsInterceptor(body)
}

class PathEqualsInterceptor(private val path: String, private val responseCode: Int) : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		assertThat(request.url().encodedPath()).isEqualTo(path)

		return Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(responseCode)
				.message("path matcher")
				.body(ResponseBody.create(MediaType.parse("text/plain"), ""))
				.build()
	}
}

class BodyEqualsInterceptor<T : Any>(private val body: T) : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		val copy = request.newBuilder().build()
		val buffer = Buffer()
		copy.body()?.writeTo(buffer)
		val body = ObjectMapper().registerModule(KotlinModule())
				.readValue(buffer.readByteArray(), body.javaClass)

		assertThat(body).isEqualTo(body)

		return Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.build()
	}
}