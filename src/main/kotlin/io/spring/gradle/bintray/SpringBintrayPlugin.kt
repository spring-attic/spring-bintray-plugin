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
package io.spring.gradle.bintray

import io.spring.gradle.bintray.task.CreatePackageTask
import io.spring.gradle.bintray.task.CreateVersionTask
import io.spring.gradle.bintray.task.MavenCentralSyncTask
import io.spring.gradle.bintray.task.PublishTask
import io.spring.gradle.bintray.task.SignTask
import io.spring.gradle.bintray.task.UploadTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.slf4j.LoggerFactory

/**
 * @author Jon Schneider
 */
class SpringBintrayPlugin : Plugin<Project> {
	lateinit var ext: SpringBintrayExtension
	private val logger = LoggerFactory.getLogger(SpringBintrayPlugin::class.java)

	override fun apply(project: Project) {
		ext = project.extensions.create("bintray", SpringBintrayExtension::class.java)

		val createPackageTask = project.tasks.create("bintrayCreatePackage", CreatePackageTask::class.java)

		val createVersionTask = project.tasks.create("bintrayCreateVersion", CreateVersionTask::class.java)
		createVersionTask.dependsOn(createPackageTask)

		val uploadTask = project.tasks.create("bintrayUpload", UploadTask::class.java)
		uploadTask.dependsOn(createVersionTask)

		// We try to sign on upload, so this task won't be part of the default chain of events.
		// It's here in case you need to sign manually after uploading for whatever reason.
		val signTask = project.tasks.create("bintraySign", SignTask::class.java)
		signTask.dependsOn(uploadTask)

		val publishTask = project.tasks.create("bintrayPublish", PublishTask::class.java)
		publishTask.dependsOn(uploadTask)

		val mavenCentralSyncTask = project.tasks.create("mavenCentralSync", MavenCentralSyncTask::class.java)
		mavenCentralSyncTask.dependsOn(publishTask)

		project.afterEvaluate {
			project.tasks.withType(CreateVersionTask::class.java) { t ->
				println()
				if(ext.publication != null) {
					val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
					if (publication is MavenPublication) {
						publication.artifacts.forEach { artifact ->
							t.dependsOn(artifact.buildDependencies)
						}
					}
				}
			}

			project.tasks.withType(UploadTask::class.java) { t ->
				if(ext.publication != null) {
					val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
					if (publication is MavenPublication) {
						t.dependsOn("generatePomFileFor${publication.name.capitalize()}Publication")
					}
				}
			}

			if (ext.bintrayUser(project) == null || ext.bintrayKey(project) == null || ext.repo == null || ext.publication == null || ext.licenses == null) {
				listOf(createPackageTask, createVersionTask, uploadTask, signTask, publishTask, mavenCentralSyncTask).forEach {
					it.onlyIf {
						project.logger.warn("bintray.[bintrayUser, bintrayKey, repo, publication, licenses] are all required for Bintray publishing")
						false
					}
				}
			}
		}
	}
}
