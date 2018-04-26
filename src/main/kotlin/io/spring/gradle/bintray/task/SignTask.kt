package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class SignTask : AbstractBintrayTask() {
	init {
		onlyIf { bintrayClient.isSuccessful(versionPath) }
	}

	@TaskAction
	fun sign() {
		logger.info("Signing $path")

		val sign = Sign(ext.gpgPassphrase)
		bintrayClient.post("gpg/${pkg.subject}/${pkg.repo}/${pkg.name}/versions/$version", sign).use { response ->
			if (!response.isSuccessful) {
				throw GradleException("failed to sign $path: HTTP ${response.code()} / ${response.body()?.string()}")
			}
		}
	}
}

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class Sign(val passphrase: String? = null)