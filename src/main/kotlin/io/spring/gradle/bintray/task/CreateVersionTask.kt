/**
 * Copyright 2017-2018 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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