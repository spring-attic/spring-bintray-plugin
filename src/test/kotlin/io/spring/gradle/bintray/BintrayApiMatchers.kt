/**
 * Copyright 2017-2018 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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