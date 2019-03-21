/*
 * Copyright 2017-2018 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.gradle.bintray

import nebula.test.IntegrationSpec

class SpringBintrayPluginIntegSpec extends IntegrationSpec {
    def 'publish a multi-module project to JCenter'() {
        when:
        def propsInput = getClass().getResourceAsStream('/bintray.properties')

        def props = new Properties()
        if(propsInput)
            props.load(propsInput)

        buildFile << """\
            allprojects {
                group = 'io.spring.gradle.bintray.test'
                version = '${new Date().format('yyyyMMdd.HH.mm.ss')}'
            }
            
            subprojects {
                apply plugin: 'java'
                apply plugin: 'maven-publish'
                ${applyPlugin(SpringBintrayPlugin)}

                bintray {
                    repo = 'jars'
                    org = 'spring'
        
                    bintrayUser = '${props.getProperty('bintrayUser')}'
                    bintrayKey = '${props.getProperty('bintrayKey')}'
                    
                    gpgPassphrase = '${props.getProperty('gpgPassphrase')}'

                    publication = 'mavenJava'

                    websiteUrl = "https://github.com/spring-gradle-plugins/spring-bintray-plugin"
                    vcsUrl = "https://github.com/spring-gradle-plugins/spring-bintray-plugin.git"
                    issueTrackerUrl = "https://github.com/spring-gradle-plugins/spring-bintray-plugin/issues"
        
                    licenses = ['Apache-2.0']
                }
                
                publishing {
                    publications {
                        mavenJava(MavenPublication) {
                            from components.java
                        }
                    }
                }
            }
        """.stripMargin()

        def core = addSubproject('spring-bintray-plugin-integtest-core')
        def a = new File(core, 'src/main/java/io/spring/bintray/test/A.java')
        a.getParentFile().mkdirs()
        a << '''\
            package io.spring.bintray.test;
            public class A {}
        '''.stripMargin()

        addSubproject('spring-bintray-plugin-integtest-test')

        def atest = new File(core, 'src/main/java/io/spring/bintray/test/ATest.java')
        atest.getParentFile().mkdirs()
        atest << '''\
            package io.spring.bintray.test;
            public class ATest {}
        '''.stripMargin()

        then:

        if(propsInput) {
            def result = runTasksSuccessfully('bintrayPublish')
            println(result.standardOutput)
        }
        else {
            println("Skipping integration test. Provide a bintray.properties in integTest/resources that includes bintrayUser, bintrayKey, and gpgPassphrase to run.")
        }
    }
}
