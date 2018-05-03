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

import io.spring.gradle.bintray.BintrayClient
import io.spring.gradle.bintray.BintrayPackage
import io.spring.gradle.bintray.SpringBintrayExtension
import org.gradle.api.DefaultTask
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

abstract class AbstractBintrayTask : DefaultTask() {
	init {
		onlyIf {
			if(bintrayUser() == null)
				logger.warn("No bintray.bintrayUser defined. Skipping.")
			if(bintrayKey() == null)
				logger.warn("No bintray.bintrayKey defined. Skipping.")
			bintrayUser() != null && bintrayKey() != null
		}
	}

	protected val ext: SpringBintrayExtension by lazy { project.extensions.getByType(SpringBintrayExtension::class.java) }
	protected val pkg: BintrayPackage by lazy { ext.bintrayPackage(project) }

	val bintrayClient: BintrayClient by lazy { BintrayClient(bintrayUser(), bintrayKey()) }

	val publication: Publication? by lazy {
		val pubName = ext.publication
		if(pubName is String) {
			project.extensions
					.findByType(PublishingExtension::class.java)
					?.publications
					?.findByName(pubName)
		} else null
	}

	val version: String by lazy {
		val pub = publication
		if (pub is MavenPublication && pub.version is String) {
			pub.version
		} else project.version.toString()
	}

	val packagePath: String by lazy { pkg.run { "packages/$subject/$repo/$name" } }
	val versionPath: String by lazy { pkg.run { "packages/$subject/$repo/$name/versions/$version" } }

	fun bintrayUser() = ext.bintrayUser ?: project.findProperty("bintrayUser") as String?
	fun bintrayKey() = ext.bintrayKey ?: project.findProperty("bintrayKey") as String?

	override fun getGroup(): String {
		return "bintray"
	}
}