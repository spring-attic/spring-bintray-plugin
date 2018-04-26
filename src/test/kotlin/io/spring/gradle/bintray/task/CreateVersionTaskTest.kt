package io.spring.gradle.bintray.task

import io.spring.gradle.bintray.BintrayApiMatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateVersionTaskTest: AbstractBintrayTaskSetup() {
	private val task = project.tasks.create("bintrayCreateVersion", CreateVersionTask::class.java)

	@Test
	fun `check if version exists`() {
		project.version = "1.0"
		task.bintrayClient.interceptors = listOf(BintrayApiMatchers.pathEq("/packages/spring/jars/mock-project/versions/1.0", 404))
		assertThat(task.onlyIf.isSatisfiedBy(task)).isTrue()
	}

	@Test
	fun `vcsUrl and githubRepo can be inferred from the project`() {
		task.bintrayClient.interceptors = listOf(
				BintrayApiMatchers.pathEq("/packages/spring/jars/mock-project/versions"),
				BintrayApiMatchers.bodyEq(CreateVersion(project.name, "v1.0"))
		)
		task.createVersion()
	}
}