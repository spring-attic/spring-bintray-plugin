package io.spring.gradle.bintray

import io.spring.gradle.bintray.task.*
import org.ajoberstar.grgit.Remote
import org.ajoberstar.grgit.operation.OpenOp
import org.ajoberstar.grgit.operation.RemoteListOp
import org.codehaus.groovy.runtime.DefaultGroovyMethods.capitalize
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

/**
 * @author Jon Schneider
 */
class SpringBintrayPlugin: Plugin<Project> {
    lateinit var ext: SpringBintrayExtension

    override fun apply(project: Project) {
        ext = project.extensions.create("bintray", SpringBintrayExtension::class.java)

        val createPackageTask = project.tasks.create("bintrayCreatePackage", CreatePackageTask::class.java)

        val createVersionTask = project.tasks.create("bintrayCreateVersion", CreateVersionTask::class.java)
        createVersionTask.dependsOn(createPackageTask)

        val uploadTask = project.tasks.create("bintrayUpload", UploadTask::class.java)
        uploadTask.dependsOn(createVersionTask)

        // We try to sign on upload, so this task won't be part of the default chain of events.
        // It's here in case you need to sign manually after uploading for whatever reason.
        val signTask = project.tasks.create("bintraySign", SignTask::class.java)
        signTask.dependsOn(uploadTask)

        val publishTask = project.tasks.create("bintrayPublish", PublishTask::class.java)
        publishTask.dependsOn(uploadTask)

        val mavenCentralSyncTask = project.tasks.create("mavenCentralSync", MavenCentralSyncTask::class.java)
        mavenCentralSyncTask.dependsOn(publishTask)

        project.afterEvaluate {
            if (ext.bintrayUser == null || ext.bintrayKey == null || ext.repo == null || ext.publication == null || ext.licenses == null) {
                listOf(createPackageTask, createVersionTask, uploadTask, signTask, publishTask, mavenCentralSyncTask).forEach {
                    it.onlyIf {
                        project.logger.info("bintray.[bintrayUser, bintrayKey, repo, packageName, licenses] are all required")
                        false
                    }
                }
            }
            else {
                configureCreatePackageTask(project)
                configureCreateVersionTask(project)
                configureUploadTask(project)
                configureSignTask(project)
                configurePublishTask(project)
                configureMavenCentralSync(project)
            }
        }
    }

    private fun configureMavenCentralSync(project: Project) {
        project.tasks.withType(MavenCentralSyncTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.version = version(project)

            t.onlyIf {
                if(ext.ossrhUser == null || ext.ossrhPassword == null) {
                    project.logger.info("bintray.[ossrhUser, ossrhPassword] are required to sync to Maven Central")
                    false
                }
                else true
            }

            ext.ossrhUser?.let { t.ossrhUser = it }
            ext.ossrhPassword?.let { t.ossrhPassword = it }

            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun configureSignTask(project: Project) {
        project.tasks.withType(SignTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.version = version(project)

            t.gpgPassphrase = ext.gpgPassphrase
            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun configurePublishTask(project: Project) {
        project.tasks.withType(PublishTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.version = version(project)
            t.configureBintrayAuth()

            t.postConfigure()
        }
    }

    private fun configureUploadTask(project: Project) {
        project.tasks.withType(UploadTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.publicationName = ext.publication!!
            t.overrideOnUpload = ext.overrideOnUpload
            t.gpgPassphrase = ext.gpgPassphrase
            t.configureBintrayAuth()

            val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
            if(publication is MavenPublication) {
                t.dependsOn("generatePomFileFor${publication.name.capitalize()}Publication")
            }

            t.postConfigure()
        }
    }

    private fun configureCreateVersionTask(project: Project) {
        project.tasks.withType(CreateVersionTask::class.java) { t ->
            t.pkg = BintrayPackage(ext.org!!, ext.repo!!, ext.packageName ?: project.name)
            t.version = version(project)

            val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
            if(publication is MavenPublication) {
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
        bintrayUser = ext.bintrayUser ?: project.findProperty("bintrayUser") as String?
        bintrayKey = ext.bintrayKey ?: project.findProperty("bintrayKey") as String?
    }

    private fun version(project: Project): String {
        val publication = project.extensions.getByType(PublishingExtension::class.java).publications.findByName(ext.publication)
        if(publication is MavenPublication) {
            if(publication.version is String)
                return publication.version
        }
        return project.version.toString()
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
