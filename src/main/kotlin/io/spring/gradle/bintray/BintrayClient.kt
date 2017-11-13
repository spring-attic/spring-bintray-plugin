package io.spring.gradle.bintray

import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.io.Serializable
import java.util.concurrent.TimeUnit

/**
 * A serializable client suitable for use in Gradle Workers
 *
 * @author Jon Schneider
 */
data class BintrayClient(val bintrayUser: String, val bintrayKey: String) : Serializable {
    fun http() = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(6, TimeUnit.MINUTES)
            .authenticator({ _, response ->
                val credential = Credentials.basic(bintrayUser, bintrayKey)
                response.request().newBuilder()
                        .header("Authorization", credential)
                        .build()
            })
            .build()
}