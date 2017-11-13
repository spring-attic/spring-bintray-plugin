package io.spring.gradle.bintray

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit

class SpringBintrayPluginSpec extends ProjectSpec {

    def 'vcsUrl and githubRepo can be inferred from the project'() {
        setup:
        Grgit repo = Grgit.init(dir: projectDir)
        repo.remote.add(name: 'origin', url: 'git@github.com:spring-gradle-plugins/gradle-bintray-plugin.git')

        when:
        project.apply plugin: SpringBintrayPlugin
        def plugin = project.plugins.getPlugin(SpringBintrayPlugin)
        plugin.configureCreatePackageTask(project)

        def createPackage = project.tasks.findByName('bintrayCreatePackage')

        then:
        createPackage.vcsUrl == 'https://github.com/spring-gradle-plugins/gradle-bintray-plugin.git'
        createPackage.githubRepo == 'https://github.com/spring-gradle-plugins/gradle-bintray-plugin.git'
    }
}
