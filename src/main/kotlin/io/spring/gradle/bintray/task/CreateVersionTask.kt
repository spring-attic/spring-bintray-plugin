package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Create a package version. Up-to-date when the version already exists.
 *
 * @author Jon Schneider
 */
open class CreateVersionTask : AbstractBintrayTask() {
	init {
		onlyIf { !bintrayClient.isSuccessful(versionPath) }
	}

	@TaskAction
	fun createVersion() {
		val createVersion = CreateVersion(version, "v$version")

		bintrayClient.post("packages/${pkg.subject}/${pkg.repo}/${pkg.name}/versions", createVersion).use { response ->
			when {
				response.isSuccessful -> logger.info("Created Bintray version $versionPath")
				response.code() == 409 -> logger.info("Bintray version already exists $versionPath")
				else -> throw GradleException("Could not create version $versionPath: HTTP ${response.code()} / ${response.body()?.string()}")
			}
		}
	}
}

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVersion(val name: String, val vcsTag: String)