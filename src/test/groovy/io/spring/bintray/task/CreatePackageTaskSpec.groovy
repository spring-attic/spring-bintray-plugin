package io.spring.bintray.task

import io.spring.bintray.BintrayPackage
import nebula.test.ProjectSpec

class CreatePackageTaskSpec extends ProjectSpec {
    def 'task is up-to-date when package already exists'() {
        when:
        BintrayPackage pkg = new BintrayPackage('spring', 'jars', 'io.spring.cloud')
        CreatePackageTask task = project.tasks.create('bintrayCreatePackage', CreatePackageTask)
        task.pkg = pkg

        then:
        task.outputs.upToDateSpec.isSatisfiedBy(task)
    }
}
