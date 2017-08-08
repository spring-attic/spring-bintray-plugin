package io.spring.bintray

import io.spring.bintray.task.AbstractBintrayTask
import nebula.test.ProjectSpec

class BintrayProjectSpec extends ProjectSpec {
    def loadKeys(AbstractBintrayTask task) {
        def props = new Properties()
        props.load(BintrayProjectSpec.getResourceAsStream('/bintray.properties'))

        task.bintrayUser = props.get('bintrayUser')
        task.bintrayKey = props.get('bintrayKey')
    }
}
