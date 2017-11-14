package io.spring.gradle.bintray

import nebula.test.IntegrationTestKitSpec

class PublishTaskIntegSpec extends IntegrationTestKitSpec {
    // CLEANUP: http -a bintrayUser:bintrayKey DELETE https://api.bintray.com/packages/spring/jars/spring-bintray-plugin-test
    def setup() {
        debug = true

        new File(projectDir, 'gradle.properties') << PublishTaskIntegSpec.getResourceAsStream('/bintray.properties')

        settingsFile << '''
            rootProject.name = 'spring-bintray-plugin-test'

            include 'spring-bintray-plugin-test-core'
            include 'spring-bintray-plugin-test-utils'
            
            project(":spring-bintray-plugin-test-core").projectDir = new File(rootProject.projectDir, 'core')
            project(":spring-bintray-plugin-test-utils").projectDir = new File(rootProject.projectDir, 'utils')
        '''

        def coreSource = new File(projectDir, 'core/src/main/java/io/spring/gradle/A.java')
        coreSource.parentFile.mkdirs()
        coreSource << '''
            |package io.spring.gradle;
            |public class A {
            |    public void foo() {
            |    }
            |}
        '''.stripMargin()

        def utilsSource = new File(projectDir, 'utils/src/main/java/io/spring/gradle/B.java')
        utilsSource.parentFile.mkdirs()
        utilsSource << '''
            |package io.spring.gradle;
            |public class B {
            |    public void bar() {
            |        new A().foo();
            |    }
            |}
        '''.stripMargin()

        buildFile << '''
            plugins {
                id 'io.spring.bintray' apply false
                id 'nebula.source-jar' version '5.1.1' apply false
                id 'nebula.javadoc-jar' version '5.1.1' apply false
                id 'nebula.maven-publish' version '5.1.1' apply false
                id 'nebula.maven-apache-license' version '5.1.1' apply false
                id 'nebula.info' version '3.6.0' apply false
                id 'nebula.contacts' version '3.0.1' apply false
                id 'com.dorongold.task-tree' version "1.3"
            }
            
            allprojects {
                group = 'io.spring.gradle\'
                version = '0.1.0\'
            }
            
            subprojects {
                apply plugin: 'java'
                apply plugin: 'io.spring.bintray'
                apply plugin: 'nebula.source-jar'
                apply plugin: 'nebula.javadoc-jar'
                apply plugin: 'nebula.maven-publish'
                apply plugin: 'nebula.maven-apache-license'
                apply plugin: 'nebula.info'
                apply plugin: 'nebula.contacts'
            
                bintray {
                    org = 'spring'
                    repo = 'jars'
                    publication = 'nebula'
                    licenses = ['Apache-2.0']
                    overrideOnUpload = true
                }
            
                contacts {
                    'jkschneider@gmail.com' {
                        moniker 'Jon Schneider'
                        github 'jkschneider'
                    }
                }
            }
            
            project(':spring-bintray-plugin-test-utils') {
                dependencies {
                    compile project(':spring-bintray-plugin-test-core')
                }
            }
        '''.stripMargin()
    }

    def uploadFiles() {
        expect:
        runTasks('bintrayPublish', "-s")
    }
}
