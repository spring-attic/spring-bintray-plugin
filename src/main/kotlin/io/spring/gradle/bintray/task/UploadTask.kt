package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.spring.gradle.bintray.BintrayClient
import io.spring.gradle.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

/**
 * Uploads all files related to a package version. Employs the Gradle Worker API to accomplish parallel uploading.
 *
 * @author Jon Schneider
 */
open class UploadTask @Inject constructor(private val workerExecutor: WorkerExecutor) : AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var publicationName: String
    @Input var overrideOnUpload: Boolean = false
    @Input @Optional var gpgPassphrase: String? = null

    private lateinit var publication: MavenPublication

    override fun postConfigure() {
        val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(publicationName)
        if (publication is MavenPublication) {
            this.publication = publication
        } else {
            onlyIf {
                logger.info("'$publicationName' does not refer to a maven publication, skipping")
                false
            }
        }
        super.postConfigure()
    }

    @TaskAction
    fun upload() {
        publication.artifacts.forEach { artifact ->
            workerExecutor.submit(UploadWorker::class.java) { config: WorkerConfiguration ->
                val path =
                        (publication.groupId?.replace('.', '/') ?: "") +
                                "/${publication.artifactId}/${publication.artifactId}-${publication.version}" +
                                (artifact.classifier?.let { "-$it" } ?: "") +
                                ".${artifact.extension}"

                config.isolationMode = IsolationMode.NONE
                config.params(bintrayClient, pkg, publication.version, path, artifact.file, overrideOnUpload, gpgPassphrase ?: "")
            }
        }
    }
}

/**
 * Allows artifacts to be uploaded in parallel, speeding the completion of this task
 */
private class UploadWorker @Inject constructor(val bintrayClient: BintrayClient,
                                               val pkg: BintrayPackage,
                                               val version: String,
                                               val path: String,
                                               val artifact: File,
                                               val overrideOnUpload: Boolean,
                                               val gpgPassphrase: String?) : Runnable {

    @Transient private val mapper = ObjectMapper().registerModule(KotlinModule())
    @Transient private val logger = LoggerFactory.getLogger(UploadWorker::class.java)

    override fun run() {
        val (org, repo, packageName) = pkg

        if(overrideOnUpload || !artifactExists()) {
            logger.info("Uploading $path")

            var requestBuilder = Request.Builder()
                    .header("Content-Type", "*/*")

            if(!gpgPassphrase.isNullOrBlank()) {
                requestBuilder = requestBuilder.header("X-GPG-PASSPHRASE", gpgPassphrase)
            }

            val request = requestBuilder
                    .put(RequestBody.create(MediaType.parse("application/octet-stream"), artifact))
                    .url("${AbstractBintrayTask.BINTRAY_API_URL}/content/$org/$repo/$packageName/$version/$path")
                    .build()

            val response = bintrayClient.http().newCall(request).execute()
            if (!response.isSuccessful) {
                throw GradleException("failed to upload $path: HTTP ${response.code()} / ${response.body()?.string()}")
            }

            response.body()?.let { body ->
                mapper.readValue(body.string(), UploadResponse::class.java).warn?.let { warning ->
                    logger.warn("Upload response for $path contained warning message: '{}'", warning)
                }
            }
        }
    }

    private fun artifactExists(): Boolean {
        val (subject, repo) = pkg

        val request = Request.Builder()
                .head()
                .url("https://dl.bintray.com/$subject/$repo/$path")
                .build()

        val response = bintrayClient.http().newCall(request).execute()

        return if(response.isSuccessful) {
            logger.info("/$subject/$repo/$path already exists, skipping upload")
            true
        } else false
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UploadResponse(val warn: String? = null)