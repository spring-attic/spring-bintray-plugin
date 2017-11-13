package io.spring.gradle.bintray.task

import io.spring.gradle.bintray.BintrayClient
import io.spring.gradle.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * @author Jon Schneider
 */
open class UploadTask @Inject constructor(val workerExecutor: WorkerExecutor) : AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var publicationName: String

    lateinit var publication: MavenPublication

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
                config.params(bintrayClient, pkg, publication.version, path, artifact.file)
            }
        }
    }
}

private class UploadWorker @Inject constructor(val bintrayClient: BintrayClient,
                                               val pkg: BintrayPackage,
                                               val version: String,
                                               val path: String,
                                               val artifact: File) : Runnable {

    override fun run() {
        val (org, repo, packageName) = pkg

        val request = Request.Builder()
                .header("Content-Type", "*/*")
                .put(RequestBody.create(MediaType.parse("application/octet-stream"), artifact))
                .url("${AbstractBintrayTask.BINTRAY_API_URL}/content/$org/$repo/$packageName/$version/$path")
                .build()

        val response = bintrayClient.http().newCall(request).execute()
        if (!response.isSuccessful) {
            throw GradleException("failed to upload $path: HTTP ${response.code()} / ${response.body()?.string()}")
        }
    }
}