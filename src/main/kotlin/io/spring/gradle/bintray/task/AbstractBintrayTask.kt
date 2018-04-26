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