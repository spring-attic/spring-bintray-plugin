package io.spring.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.spring.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Creates a bintray package
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
    @Input @Optional var publicDownloadNumbers: Boolean = true
    @Input @Optional var githubRepo: String? = null
    @Input @Optional var githubReleaseNotesFile: String? = null

    private val packagePath by lazy { pkg.run { "packages/$org/$repo/$name" } }

    init {
        outputs.upToDateWhen {
            val response = http.newCall(Request.Builder().head().url(pkg.run { "$BINTRAY_API_URL/$packagePath" }).build()).execute()
            response.isSuccessful // if successful, this package already exists
        }
    }

    @TaskAction
    fun createPackage() {
        val (org, repo, packageName) = pkg
        val createPackage = CreatePackage(packageName, desc, licenses, labels, websiteUrl, issueTrackerUrl, vcsUrl, publicDownloadNumbers, githubRepo, githubReleaseNotesFile)

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(createPackage))

        val request = Request.Builder()
                .url("$BINTRAY_API_URL/packages/$org/$repo")
                .post(body)
                .build()

        val response = http.newCall(request).execute()
        if(response.isSuccessful) {
            logger.info("Created Bintray package /$packagePath")
        } else {
            throw GradleException("Could not create package /$packagePath: HTTP ${response.code()} / ${response.body()?.string()}")
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
                                     val publicDownloadNumbers: Boolean,
                                     val githubRepo: String?,
                                     val githubReleaseNotesFile: String?)
}