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
