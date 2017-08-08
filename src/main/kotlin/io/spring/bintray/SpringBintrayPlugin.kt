package io.spring.bintray

import io.spring.bintray.task.UploadTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpringBintrayPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("bintray", SpringBintrayExtension::class.java)

        val uploadTask = project.tasks.create("bintrayUpload", UploadTask::class.java)
    }
}