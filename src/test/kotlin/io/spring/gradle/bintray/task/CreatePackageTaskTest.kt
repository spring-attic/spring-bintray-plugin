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

import io.spring.gradle.bintray.BintrayApiMatchers.bodyEq
import io.spring.gradle.bintray.BintrayApiMatchers.pathEq
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class CreatePackageTaskTest: AbstractBintrayTaskSetup() {
	private val task = project.tasks.create("bintrayCreatePackage", CreatePackageTask::class.java)

	@Test
	fun `check if package exists`() {
		task.bintrayClient.interceptors = listOf(pathEq("/packages/spring/jars/mock-project", 404))
		assertThat(task.onlyIf.isSatisfiedBy(task)).isTrue()
	}

	@Test
	fun `vcsUrl and githubRepo can be inferred from the project`() {
		task.bintrayClient.interceptors = listOf(
				pathEq("/packages/spring/jars"),
				bodyEq(CreatePackage(project.name,
						"some description",
						listOf("apache2"),
						listOf("label"),
						"https://github.com/mock/mock-project",
						"https://github.com/mock/mock-project/issues",
						"https://github.com/mock/mock-project.git",
						true))
		)

		task.createPackage()
	}
}