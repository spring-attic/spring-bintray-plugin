package io.spring.gradle.bintray

import nebula.test.IntegrationSpec

class SpringBintrayPluginIntegSpec extends IntegrationSpec {
    def 'publish a multi-module project to JCenter'() {
        when:
        def props = new Properties()
        props.load(getClass().getResourceAsStream('/bintray.properties'))

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
        def result = runTasksSuccessfully('bintrayPublish')
        println(result.standardOutput)
        println(result.standardError)
    }
}
