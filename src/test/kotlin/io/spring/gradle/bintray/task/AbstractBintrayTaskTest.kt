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

import io.spring.gradle.bintray.SpringBintrayExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.jupiter.api.Test

open class AbstractBintrayTaskTest {
	private val project = ProjectBuilder.builder().withName("mock-project").build()
	private val task = project.tasks.create("bintrayTask", NoopBintrayTask::class.java)

	init {
		project.extensions.getByType(ExtraPropertiesExtension::class.java).run {
			set("bintrayUser", "me")
			set("bintrayKey", "key")
		}

		project.extensions.create("bintray", SpringBintrayExtension::class.java)
	}

	@Test
	fun `load bintray user and key from props`() {
		assertThat(task.bintrayUser()).isEqualTo("me")
		assertThat(task.bintrayKey()).isEqualTo("key")
	}

	open class NoopBintrayTask : AbstractBintrayTask()
}