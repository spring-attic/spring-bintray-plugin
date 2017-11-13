package io.spring.gradle.bintray.task

import io.spring.gradle.bintray.BintrayPackage
import nebula.test.ProjectSpec
import org.gradle.api.publish.maven.MavenPublication

class CreateVersionTaskSpec extends ProjectSpec {
    def 'task is up-to-date when version already exists'() {
        when:
        BintrayPackage pkg = new BintrayPackage('spring', 'jars', 'io.spring.cloud')
        CreateVersionTask task = project.tasks.create('bintrayCreateVersion', CreateVersionTask)
        task.pkg = pkg
        task.publication = [getVersion: {'1.2.0.RELEASE'}] as MavenPublication
        task.postConfigure()

        then:
        task.outputs.upToDateSpec.isSatisfiedBy(task)
    }
}
