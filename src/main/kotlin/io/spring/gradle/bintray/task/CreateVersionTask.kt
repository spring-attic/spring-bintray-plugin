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
import org.gradle.api.tasks.TaskAction

/**
 * Create a package version. Up-to-date when the version already exists.
 *
 * @author Jon Schneider
 */
open class CreateVersionTask: AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var version: String

    private val versionPath by lazy { pkg.run { "$BINTRAY_API_URL/packages/$subject/$repo/$name/versions/$version" } }

    override fun postConfigure() {
        onlyIf {
            bintrayClient.http().newCall(Request.Builder().head().url(versionPath).build())
                    .execute()
                    .use { response -> !response.isSuccessful } // if successful, this version already exists
        }
        super.postConfigure()
    }

    @TaskAction
    fun createVersion() {
        val (subject, repo, name) = pkg
        val createVersion = CreateVersion(version, "v$version")

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(createVersion))

        val request = Request.Builder()
                .url("$BINTRAY_API_URL/packages/$subject/$repo/$name/versions")
                .post(body)
                .build()

        bintrayClient.http().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Created Bintray version $versionPath")
            } else if (response.code() == 409) {
                logger.info("Bintray version already exists $versionPath")
            } else {
                throw GradleException("Could not create version $versionPath: HTTP ${response.code()} / ${response.body()?.string()}")
            }
        }
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class CreateVersion(val name: String,
                                     val vcsTag: String)
}