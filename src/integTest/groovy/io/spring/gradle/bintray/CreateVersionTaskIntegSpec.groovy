package io.spring.gradle.bintray

import io.spring.gradle.bintray.task.CreatePackageTask
import io.spring.gradle.bintray.task.CreateVersionTask
import org.gradle.api.publish.maven.MavenPublication

class CreateVersionTaskIntegSpec extends BintrayProjectSpec implements Serializable {
    // CLEANUP: http -a bintrayUser:bintrayKey DELETE https://api.bintray.com/packages/spring/jars/spring-bintray-plugin-test
    def 'create a new version'() {
        setup:
        BintrayPackage pkg = new BintrayPackage('spring', 'jars', 'spring-bintray-plugin-test')
        CreatePackageTask createPackage = project.tasks.create('bintrayCreatePackage', CreatePackageTask)
        createPackage.pkg = pkg
        createPackage.vcsUrl = 'http://github.com/spring-gradle-plugins/spring-bintray-plugin'
        createPackage.licenses = ['Apache-2.0']
        loadKeys(createPackage)
        createPackage.postConfigure()

        if(!createPackage.outputs.upToDateSpec.isSatisfiedBy(createPackage))
            createPackage.execute()

        when:
        CreateVersionTask createVersion = project.tasks.create('bintrayCreateVersion', CreateVersionTask)
        createVersion.pkg = pkg
        createVersion.publication = [getVersion: {'0.1.0'}] as MavenPublication
        loadKeys(createVersion)
        createVersion.postConfigure()

        then: 'the version must not exist yet -- delete it if it does'
        !createVersion.outputs.upToDateSpec.isSatisfiedBy(createVersion)

        when:
        createVersion.execute()

        then: 'the version has been created -- if not, were bintrayUser and bintrayKey provided?'
        createVersion.outputs.upToDateSpec.isSatisfiedBy(createVersion)
    }
}
