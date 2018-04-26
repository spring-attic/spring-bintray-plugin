package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.spring.gradle.bintray.BintrayClient
import io.spring.gradle.bintray.BintrayPackage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.artifact.DefaultMavenArtifact
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
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
	init {
		onlyIf {
			if (publication == null) {
				logger.info("'${ext.publication}' does not refer to a maven publication, skipping")
				false
			} else true
		}
	}

	@TaskAction
	fun upload() {
		if (publication is DefaultMavenPublication) {
			val mavenPub = publication as DefaultMavenPublication

			val pomFile = mavenPub.asNormalisedPublication().pomFile
			if (!pomFile.exists()) {
				throw GradleException("POM file does not exist: ${pomFile.absolutePath}")
			}

			val artifacts = setOf(DefaultMavenArtifact(pomFile, "pom", null)) +
					mavenPub.artifacts

			artifacts.forEach { artifact ->
				workerExecutor.submit(UploadWorker::class.java) { config: WorkerConfiguration ->
					val path =
							(mavenPub.groupId?.replace('.', '/') ?: "") +
									"/${mavenPub.artifactId}/${mavenPub.version}/${mavenPub.artifactId}-${mavenPub.version}" +
									(artifact.classifier?.let { "-$it" } ?: "") +
									".${artifact.extension}"

					config.isolationMode = IsolationMode.NONE
					config.params(bintrayUser(), bintrayKey(), pkg, mavenPub.version, path, artifact.file, ext.overrideOnUpload,
							ext.gpgPassphrase ?: "")
				}
			}
		} else {
			logger.info("'${ext.publication}' does not refer to a maven publication, skipping")
		}
	}
}

/**
 * Allows artifacts to be uploaded in parallel, speeding the completion of this task
 */
private class UploadWorker @Inject constructor(val bintrayUser: String,
											   val bintrayKey: String,
											   val pkg: BintrayPackage,
											   val version: String,
											   val path: String,
											   val artifact: File,
											   val overrideOnUpload: Boolean,
											   val gpgPassphrase: String?) : Runnable {

	@Transient
	private val mapper = ObjectMapper().registerModule(KotlinModule())

	@Transient
	private val logger = LoggerFactory.getLogger(UploadWorker::class.java)

	override fun run() {
		val bintrayClient = BintrayClient(bintrayUser, bintrayKey)

		if (overrideOnUpload || !artifactExists()) {
			logger.info("Uploading $path")

			try {
				bintrayClient.upload("content/${pkg.subject}/${pkg.repo}/${pkg.name}/$version/$path", artifact, gpgPassphrase).use { response ->
					if (!response.isSuccessful) {
						throw GradleException("failed to upload $path: HTTP ${response.code()} / ${response.body()?.string()}")
					}

					response.body()?.let { body ->
						mapper.readValue(body.string(), UploadResponse::class.java).warn?.let { warning ->
							logger.warn("Upload response for $path contained warning message: '{}'", warning)
						}
					}
				}
			} catch (t: Throwable) {
				throw GradleException("failed to upload $path", t)
			}
		}
	}

	private fun artifactExists(): Boolean {
		val (subject, repo) = pkg

		val request = Request.Builder()
				.head()
				.url("https://dl.bintray.com/$subject/$repo/$path")
				.build()

		val response = OkHttpClient.Builder().build().newCall(request).execute()

		return if (response.isSuccessful) {
			logger.info("/$subject/$repo/$path already exists, skipping upload")
			true
		} else false
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UploadResponse(val warn: String? = null)