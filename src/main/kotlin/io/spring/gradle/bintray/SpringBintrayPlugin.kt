package io.spring.gradle.bintray

import io.spring.gradle.bintray.task.AbstractBintrayTask
import io.spring.gradle.bintray.task.CreatePackageTask
import io.spring.gradle.bintray.task.CreateVersionTask
import io.spring.gradle.bintray.task.UploadTask
import org.ajoberstar.grgit.Remote
import org.ajoberstar.grgit.operation.OpenOp
import org.ajoberstar.grgit.operation.RemoteListOp
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

class SpringBintrayPlugin: Plugin<Project> {
    lateinit var ext: SpringBintrayExtension

    override fun apply(project: Project) {
        ext = project.extensions.create("bintray", SpringBintrayExtension::class.java)

        val createPackageTask = project.tasks.create("bintrayCreatePackage", CreatePackageTask::class.java)

        val createVersionTask = project.tasks.create("bintrayCreateVersion", CreateVersionTask::class.java)
        createVersionTask.dependsOn(createPackageTask)

        val uploadTask = project.tasks.create("bintrayUpload", UploadTask::class.java)
        uploadTask.dependsOn(createVersionTask)

        project.afterEvaluate {
            if (ext.org == null || ext.repo == null || ext.publication == null || ext.licenses == null) {
                listOf(createPackageTask, createVersionTask, uploadTask).forEach {
                    it.onlyIf {
                        project.logger.info("bintray.[org, repo, packageName, licenses] are all required")
                        false
                    }
                }
            }
            else {
                configureCreatePackageTask(project)
                configureCreateVersionTask(project)
                configureUploadTask(project)
            }
        }
    }

    private fun configureUploadTask(project: Project) {
        project.tasks.withType(UploadTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.publicationName = ext.publication!!
            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun configureCreateVersionTask(project: Project) {
        project.tasks.withType(CreateVersionTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)

            val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
            if(publication is MavenPublication) {
                t.version = publication.version

                publication.artifacts.forEach { artifact ->
                    val buildDependencies = artifact.buildDependencies
                    when(buildDependencies) {
                        is DefaultTaskDependency -> t.dependsOn(buildDependencies.values)
                    }
                }
            }

            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun configureCreatePackageTask(project: Project) {
        project.tasks.withType(CreatePackageTask::class.java) { t ->
            t.licenses = ext.licenses ?: emptyList()
            t.desc = ext.packageDescription
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.labels = ext.labels

            val githubRemote = findGithubRemote(project)

            t.vcsUrl = ext.vcsUrl ?: githubRemote?.plus(".git") ?: ""
            t.websiteUrl = ext.websiteUrl ?: githubRemote
            t.issueTrackerUrl = ext.issueTrackerUrl ?: githubRemote?.plus("/issues") ?: ""

            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun AbstractBintrayTask.configureBintrayAuth() {
        bintrayUser = ext.bintrayUser ?: project.property("bintrayUser") as String?
        bintrayKey = ext.bintrayKey ?: project.property("bintrayKey") as String?
    }

    private fun findGithubRemote(project: Project): String? {
        try {
            val open = OpenOp()
            open.currentDir = project.rootProject.rootDir

            val git = open.call()

            // Remote URLs will be formatted like one of these:
            //  https://github.com/spring-gradle-plugins/spring-project-plugin.git
            //  git@github.com:spring-gradle-plugins/spring-release-plugin.git
            val listRemote = RemoteListOp(git.repository)
            val repoParts = listRemote.call()
                    .filterIsInstance(Remote::class.java)
                    .sortedWith(Comparator { r1, r2 ->
                        if (r1.name == "origin") -1
                        else if (r2.name == "origin") 1
                        else r1.name.compareTo(r2.name)
                    })
                    .map { """github\.com[/:]([^/]+)/(.+)\.git""".toRegex().find(it.url) }
                    .filterNotNull()
                    .firstOrNull() ?:
                    return null // no remote configured yet, do nothing

            val groups = repoParts.groupValues
            return "https://github.com/${groups[1]}/${groups[2]}"
        } catch(ignored: RepositoryNotFoundException) {
            // do nothing
            return null
        }
    }
}
