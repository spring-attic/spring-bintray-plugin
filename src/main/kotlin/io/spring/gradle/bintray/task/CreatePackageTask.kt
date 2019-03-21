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
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.eclipse.jgit.api.Git
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.IOException

/**
 * Creates a bintray package. Up-to-date when the package already exists.
 *
 * @author Jon Schneider
 */
open class CreatePackageTask : AbstractBintrayTask() {

	init {
		onlyIf { !bintrayClient.headIsSuccessful(packagePath) }
	}

	@TaskAction
	fun createPackage() {
		val githubRemote = findGithubRemote()

		val createPackage = CreatePackage(
				pkg.name,
				ext.packageDescription,
				ext.licenses ?: emptyList(),
				ext.labels,
				ext.websiteUrl ?: githubRemote,
				ext.issueTrackerUrl ?: githubRemote?.plus("/issues") ?: "",
				ext.vcsUrl ?: githubRemote?.plus(".git") ?: "",
				true)

		bintrayClient.post("packages/${pkg.subject}/${pkg.repo}", createPackage).use { response ->
			if (response.isSuccessful) {
				logger.info("Created Bintray package /$packagePath")
			} else if (response.code() == 409) {
				logger.info("Bintray package already exists /$packagePath")
			} else {
				throw GradleException("Could not create package /$packagePath: HTTP ${response.code()} / ${response.body()?.string()}")
			}
		}
	}

	private fun findGithubRemote(): String? {
		try {
			Git.open(project.projectDir).use { git ->
				val config = git.repository.config

				// Remote URLs will be formatted like one of these:
				//  https://github.com/spring-gradle-plugins/spring-project-plugin.git
				//  git@github.com:spring-gradle-plugins/spring-release-plugin.git
				val repoParts = config.getSubsections("remote")
						.map { remoteName -> Remote(remoteName, config.getString("remote", remoteName, "url")) }
						.sortedWith(Comparator { r1, r2 ->
							if (r1.name == "origin") -1
							else if (r2.name == "origin") 1
							else r1.name.compareTo(r2.name)
						})
						.map { """github\.com[/:]([^/]+)/(.+)\.git""".toRegex().find(it.url) }
						.filterNotNull()
						.firstOrNull()
						?: return null // no remote configured yet, do nothing

				val groups = repoParts.groupValues
				return "https://github.com/${groups[1]}/${groups[2]}"
			}
		} catch (ignored: IOException) {
			// do nothing
			return null
		}
	}
}

data class Remote(val name: String, val url: String)

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreatePackage(val name: String,
						 val desc: String?,
						 val licenses: Collection<String>,
						 val labels: Collection<String>,
						 val websiteUrl: String?,
						 val issueTrackerUrl: String?,
						 val vcsUrl: String,
						 val publicDownloadNumbers: Boolean)