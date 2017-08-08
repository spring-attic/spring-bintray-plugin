package io.spring.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.spring.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CreateVersionTask: AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var publication: MavenPublication

    private val versionPath by lazy { pkg.run { "$BINTRAY_API_URL/packages/$org/$repo/$name/versions/${publication.version}" } }

    init {
        outputs.upToDateWhen {
            val response = http.newCall(Request.Builder().head().url(versionPath).build()).execute()
            response.isSuccessful // if successful, this version already exists
        }
    }

    @TaskAction
    fun createVersion() {
        val (org, repo, packageName) = pkg
        val createVersion = CreateVersion(publication.version, "v${publication.version}")

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(createVersion))

        val request = Request.Builder()
                .url("$BINTRAY_API_URL/packages/$org/$repo/$packageName/versions")
                .post(body)
                .build()

        val response = http.newCall(request).execute()
        if(response.isSuccessful) {
            logger.info("Created Bintray package /$versionPath")
        } else {
            throw GradleException("Could not create package /$versionPath: HTTP ${response.code()} / ${response.body()?.string()}")
        }
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class CreateVersion(val name: String,
                                     val vcsTag: String)
}