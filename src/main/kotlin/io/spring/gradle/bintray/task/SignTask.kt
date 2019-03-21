/**
 * Copyright 2017-2018 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.gradle.bintray.task

import com.fasterxml.jackson.annotation.JsonInclude
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class SignTask : AbstractBintrayTask() {
	init {
		onlyIf { bintrayClient.headIsSuccessful(versionPath) }
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