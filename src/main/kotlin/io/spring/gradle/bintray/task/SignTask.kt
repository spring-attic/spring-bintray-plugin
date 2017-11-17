package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import io.spring.gradle.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

open class SignTask : AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var version: String

    @Input @Optional var gpgPassphrase: String? = null

    override fun postConfigure() {
        onlyIf {
            // version must exist in Bintray prior to signing
            bintrayClient.http()
                    .newCall(Request.Builder().head().url(pkg.run { "$BINTRAY_API_URL/packages/$subject/$repo/$name/versions/$version" }).build())
                    .execute()
                    .use { response -> response.isSuccessful }
        }

        super.postConfigure()
    }

    @TaskAction
    fun sign() {
        val (org, repo, packageName) = pkg

        logger.info("Signing $path")

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(Sign(gpgPassphrase)))

        val request = Request.Builder()
                .url("${AbstractBintrayTask.BINTRAY_API_URL}/gpg/$org/$repo/$packageName/versions/$version")
                .post(body)
                .build()

        bintrayClient.http().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw GradleException("failed to sign $path: HTTP ${response.code()} / ${response.body()?.string()}")
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class Sign(val passphrase: String? = null)