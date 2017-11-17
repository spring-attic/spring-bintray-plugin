package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.spring.gradle.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Creates a bintray package. Up-to-date when the package already exists.
 *
 * @author Jon Schneider
 */
open class CreatePackageTask : AbstractBintrayTask() {

    @Input lateinit var pkg: BintrayPackage

    // mandatory for OSS projects
    @Input lateinit var licenses: Collection<String>
    @Input lateinit var vcsUrl: String

    // optional
    @Input @Optional var desc: String? = null
    @Input @Optional var labels: Collection<String> = emptyList()
    @Input @Optional var websiteUrl: String? = null
    @Input @Optional var issueTrackerUrl: String? = null

    private val packagePath by lazy { pkg.run { "packages/$subject/$repo/$name" } }

    override fun postConfigure() {
        onlyIf {
            bintrayClient.http().newCall(Request.Builder().head().url(pkg.run { "$BINTRAY_API_URL/$packagePath" }).build())
                    .execute()
                    .use { response ->
                        !response.isSuccessful // if successful, this package already exists
                    }

        }
        super.postConfigure()
    }

    @TaskAction
    fun createPackage() {
        val (org, repo, packageName) = pkg
        val createPackage = CreatePackage(packageName, desc, licenses, labels, websiteUrl, issueTrackerUrl, vcsUrl, true)

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(createPackage))

        val request = Request.Builder()
                .url("$BINTRAY_API_URL/packages/$org/$repo")
                .post(body)
                .build()

        bintrayClient.http().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Created Bintray package /$packagePath")
            } else if (response.code() == 409) {
                logger.info("Bintray package already exists /$packagePath")
            } else {
                throw GradleException("Could not create package /$packagePath: HTTP ${response.code()} / ${response.body()?.string()}")
            }
        }
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class CreatePackage(val name: String,
                                     val desc: String?,
                                     val licenses: Collection<String>,
                                     val labels: Collection<String>,
                                     val websiteUrl: String?,
                                     val issueTrackerUrl: String?,
                                     val vcsUrl: String,
                                     val publicDownloadNumbers: Boolean)
}