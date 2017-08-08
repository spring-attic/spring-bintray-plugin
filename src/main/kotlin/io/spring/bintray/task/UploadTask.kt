package io.spring.bintray.task

import io.spring.bintray.BintrayPackage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * @author Jon Schneider
 */
open class UploadTask @Inject constructor(val workerExecutor: WorkerExecutor): AbstractBintrayTask() {
    @Input lateinit var pkg: BintrayPackage
    @Input lateinit var publication: MavenPublication;

    @TaskAction
    fun upload() {
        publication.artifacts.forEach { artifact ->
        }
    }
}