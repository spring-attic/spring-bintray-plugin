# Spring Bintray Plugin

## Purpose

This plugin is a modernization of [gradle-bintray-plugin](https://github.com/bintray/gradle-bintray-plugin) which provides more control over the individual stages of execution from creating a package all the way through Maven Central sync. Bintray's plugin places all of this logic in a single task `bintrayUpload` which is executed once on the root project for all subprojects. In the event of partial success, this can leave you high and dry manually exercising REST calls against the Bintray API to finish a release. 

Furthermore, the single-task per project model makes it difficult to configure publication on some projects and not others. Also, it does not allow for project-level decisions like NOT syncing an experimental module to Maven Central.

## Recommended use

Configuration is simpler than `gradle-bintray-plugin`:

```groovy
bintray {
    // The following properties are REQUIRED:
    bintrayUser = //...
    bintrayPassword = //...
    repo = 'jars'
    publication = 'nebula' // the named Gradle MavenPublication identifying the artifacts to publish
    
    // The following properties are OPTIONAL:
    org = 'spring' // uses bintrayUser if you aren't publishing to an organization repository
    labels = ['label1']
    licenses = ['Apache-2.0']
    ossrhUser = // only if syncing to Maven Central
    ossrhPassword = //...
    
    // The following OPTIONAL properties are derived from the `origin` github remote if not explicitly provided:
    websiteUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin'
    issueTrackerUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin/issues'
    vcsUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin.git'
}
```

You may wish to gate publishing of _any subproject_ with some other gradle task on _all projects_. For example, ensuring that all projects in multi-module project test successfully before attempting to publish _any_ project:

```groovy
// in each subproject that applies the bintray plugin
project.rootProject.subprojects.each { p ->
    def check = p.tasks.findByName('check')
    if(check) {
        tasks.bintrayUpload.dependsOn(check)
    }
}
```

## Benefits over `gradle-bintray-plugin`

* Splits Bintray interactions into separate tasks so you can more easily control which projects do what and target particular tasks in the event of Bintray partial failure.
* Logs warnings contained in the response body of uploads (`gradle-bintray-plugin` suppresses these warnings).
* Uses the Gradle Worker API to parallelize uploads.

## Limitations

* Assumes that you are using Bintray's automatic content signing. See the Bintray docs on the [minimum requirements](https://bintray.com/docs/api/#gpg_signing) to support this.