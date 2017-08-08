package io.spring.bintray

import io.spring.bintray.task.CreatePackageTask

class CreatePackageIntegSpec extends BintrayProjectSpec {
    // CLEANUP: http -a bintrayUser:bintrayKey DELETE https://api.bintray.com/packages/spring/jars/spring-bintray-plugin-test
    def 'create a new package'() {
        when:
        BintrayPackage pkg = new BintrayPackage('spring', 'jars', 'spring-bintray-plugin-test')
        CreatePackageTask task = project.tasks.create('bintrayCreatePackage', CreatePackageTask)
        task.pkg = pkg
        task.vcsUrl = 'http://github.com/spring-gradle-plugins/spring-bintray-plugin'
        task.licenses = ['Apache-2.0']
        loadKeys(task)

        then: 'the package must not exist yet -- delete it if it does'
        !task.outputs.upToDateSpec.isSatisfiedBy(task)

        when:
        task.execute()

        then: 'the package has been created -- if not, were bintrayUser and bintrayKey provided?'
        task.outputs.upToDateSpec.isSatisfiedBy(task)
    }
}
