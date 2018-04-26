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
package io.spring.gradle.bintray.task;

import io.spring.gradle.bintray.SpringBintrayExtension
import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach

/**
 * Base class for all bintray task tests
 */
abstract class AbstractBintrayTaskSetup {
	protected val project: Project = ProjectBuilder.builder().withName("mock-project").build()
	private val ext = project.extensions.create("bintray", SpringBintrayExtension::class.java)

	@BeforeEach
	fun gitAndExtensionProperties() {
		Git.init().setDirectory(project.projectDir).call().use { git ->
			val config = git.repository.config
			config.setString("remote", "origin", "url", "git@github.com:mock/mock-project.git")
			config.save()
		}

		ext.bintrayUser = "doesnt.matter"
		ext.bintrayKey = "doesnt.matter"
		ext.org = "spring"
		ext.repo = "jars"
		ext.packageDescription = "some description"
		ext.licenses = listOf("apache2")
		ext.labels = listOf("label")
	}
}
