package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Publishing basically means that other users can see and download the files you have uploaded.
 * Only uploaded files (not repos, packages, versions or user profiles) can ever have the status of unpublished.
 * This gives the uploading user time to change his or her mind about the upload or to verify or test it.
 * After 7 days, unpublished files are deleted from Bintray. Note that since unpublished is the default status
 * of an uploaded file, every file manually uploaded must be separately published. Unpublished files are only
 * visible and accessible on Bintray to the user who uploaded them; once these files are uploaded the right and
 * ability to publish them belongs to this user.
 *
 * Publishing has the secondary effect of getting files into JCenter if the package has been pre-approved for
 * JCenter syncing.
 *
 * @author Jon Schneider
 */
open class PublishTask : AbstractBintrayTask() {
	companion object {
		val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
	}

	init {
		onlyIf {
			// version must exist in Bintray prior to publishing
			bintrayClient.get(versionPath).use { response ->
				// if the version is already published, do nothing
				val body = response.body()?.string()
				response.isSuccessful && (mapper.readValue(body, GetVersion::class.java)?.published?.let { !it }
						?: false)
			}
		}
	}

	@TaskAction
	fun publish() {
		val packageVersionPath = "${pkg.subject}/${pkg.repo}/${pkg.name}/$version"
		bintrayClient.post("content/$packageVersionPath/publish", Publish()).use { response ->
			if (response.isSuccessful) {
				logger.info("Created Bintray package version /$packageVersionPath")
			} else {
				throw GradleException("Could not publish package version /$packageVersionPath: HTTP ${response.code()} / ${response.body()?.string()}")
			}
		}
	}

	@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private data class Publish(
			// causes this request to block waiting for publishing to complete, blocking for the maximum timeout allowed by Bintray
			val publishWaitForSecs: Int = -1)

	@JsonIgnoreProperties(ignoreUnknown = true)
	private data class GetVersion(val published: Boolean)
}