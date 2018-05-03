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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.GradleException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction

/**
 * Sync a published package version to Maven Central
 *
 * @author Jon Schneider
 */
open class MavenCentralSyncTask : AbstractBintrayTask() {
	init {
		onlyIf {
			// version must exist in Bintray prior to syncing
			bintrayClient.headIsSuccessful(versionPath)
		}

		onlyIf {
			when (publication) {
				is MavenPublication -> !MavenCentral.exists(publication as MavenPublication)
				else -> {
					// can't make a determination, but we don't want to proceed with a sync anyway
					logger.warn("Not attempting to sync ${project.name} to Maven Central, because no maven publication could be found on this project")
					false
				}
			}
		}

		onlyIf {
			if (ext.ossrhUser == null || ext.ossrhPassword == null) {
				project.logger.warn("bintray.[ossrhUser, ossrhPassword] are required to sync to Maven Central")
				false
			} else true
		}
	}

	@TaskAction
	fun mavenCentralSync() {
		val sync = MavenCentralSync(ext.ossrhUser!!, ext.ossrhPassword!!)
		val packageVersionPath = "${pkg.subject}/${pkg.repo}/${pkg.name}/versions/$version"
		bintrayClient.post("maven_central_sync/$packageVersionPath", sync).use { response ->
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

object MavenCentral {
	private const val MAVEN_CENTRAL_URI = "https://repo1.maven.org/maven2"

	fun exists(pub: MavenPublication): Boolean = OkHttpClient.Builder().build()
			.newCall(Request.Builder().head()
					.url("$MAVEN_CENTRAL_URI/${pub.groupId.replace('.', '/')}/${pub.artifactId}/${pub.version}")
					.build())
			.execute()
			.use { response -> response.isSuccessful }
}