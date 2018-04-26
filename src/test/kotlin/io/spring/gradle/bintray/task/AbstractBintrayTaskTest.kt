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