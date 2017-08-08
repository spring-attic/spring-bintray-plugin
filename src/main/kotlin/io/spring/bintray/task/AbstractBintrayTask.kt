package io.spring.bintray.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input


abstract class AbstractBintrayTask: DefaultTask() {
    companion object {
        val BINTRAY_API_URL = "https://api.bintray.com"
    }

    @Input var bintrayUser: String? = null
    @Input var bintrayKey: String? = null

    init {
        onlyIf { bintrayUser != null && bintrayKey != null }
    }

    protected val http = OkHttpClient.Builder()
            .authenticator({ _, response ->
                val credential = Credentials.basic(bintrayUser, bintrayKey)
                response.request().newBuilder()
                        .header("Authorization", credential)
                        .build()
            })
            .build()

    protected val mapper = ObjectMapper().registerModule(KotlinModule())

    override fun getGroup(): String {
        return "bintray"
    }
}