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
 * Sync a published package version to Maven Central
 *
 * @author Jon Schneider
 */
open class MavenCentralSyncTask : AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var version: String
    @Input lateinit var ossrhUser: String
    @Input lateinit var ossrhPassword: String

    override fun postConfigure() {
        onlyIf {
            // version must exist in Bintray prior to syncing
            bintrayClient.http()
                    .newCall(Request.Builder().head().url(pkg.run { "$BINTRAY_API_URL/packages/$subject/$repo/$name/versions/$version" }).build())
                    .execute()
                    .use { response -> response.isSuccessful }
        }
        super.postConfigure()
    }

    @TaskAction
    fun mavenCentralSync() {
        val (org, repo, packageName) = pkg

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mapper.writeValueAsString(MavenCentralSync(ossrhUser, ossrhPassword)))

        val packageVersionPath = "$org/$repo/$packageName/versions/$version"
        val request = Request.Builder()
                .url("$BINTRAY_API_URL/maven_central_sync/$packageVersionPath")
                .post(body)
                .build()

        bintrayClient.http().newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Synced /$packageVersionPath to Maven Central")
            } else {
                throw GradleException("Could not sync /$packageVersionPath to Maven Central: HTTP ${response.code()} / ${response.body()?.string()}")
            }
        }
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class MavenCentralSync(
            val username: String,
            val password: String,
            val close: Int = 1)
}
