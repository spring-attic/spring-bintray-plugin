# Spring Bintray Plugin

## Purpose

This plugin is a modernization of [gradle-bintray-plugin](https://github.com/bintray/gradle-bintray-plugin) which provides more control over the individual stages of execution from creating a package all the way through Maven Central sync. Bintray's plugin places all of this logic in a single task `bintrayUpload` which is executed once on the root project for all subprojects. In the event of partial success, this can leave you high and dry manually exercising REST calls against the Bintray API to finish a release. 

Furthermore, the single-task per project model makes it difficult to configure publication on some projects and not others. Also, it does not allow for project-level decisions like NOT syncing an experimental module to Maven Central.

## A task for every stage

These tasks are executed on a per-project basis rather than at the root project. Each task in this list `dependsOn` the prior task, so it is sufficient to call `mavenCentralSync` to perform the entire sequence. Or if you just want to get through the upload phase and publish it from the Bintray UI, call `bintrayUpload`.

1. `bintrayCreatePackage` - Creates a new package only if it doesn't already exist
2. `bintrayCreateVersion` - Creates a new package version only if it doesn't already exist
3. `bintrayUpload` - Uploads artifacts in parallel (only those that don't already exist unless `bintray.overrideOnUpload` is `true`)
4. `bintraySign` - Sign all artifacts in a version. This task is not generally necessary, as the plugin attempts to sign artifacts on upload. If signing fails for some reason on upload, you can invoke this later manually (such as after updating `bintray.gpgPassphrase` that you had wrong initially).
5. `bintrayPublish` - "Publishes" the artifacts in Bintray, effectively making them visible to more than just the original account that uploaded them (generally the public at large).
6. `mavenCentralSync` - Syncs artifacts to Maven Central and closes the corresponding OSSRH repo.

In the event of partial failure in Bintray, you can simply rerun for a single project to kickstart the process again, e.g. `./gradlew :mysubproject:bintrayPublish`. Since each stage is smart enough to check whether it has already been done, you can comfortably start anywhere in the process and know that it will pick up right after the last successful stage.

## Configuration

Configuration is simpler than `gradle-bintray-plugin`.

Here are the required properties:

```groovy
bintray {
    // The following properties are REQUIRED:
    bintrayUser = //...
    bintrayPassword = //...
    repo = 'jars'
    publication = 'nebula' // the named Gradle MavenPublication identifying the artifacts to publish
}
```

Everything else is optional, with sensible defaults:

```groovy
bintray {    
    org = 'spring' // uses bintrayUser if you aren't publishing to an organization repository
    labels = ['label1']
    licenses = ['Apache-2.0']
    ossrhUser = // only if syncing to Maven Central
    ossrhPassword = //...
    gpgPassphrase = // if your repository requires signing with a other-than-Bintray key
    overrideOnUpload = false // should the upload task override existing artifacts?
    
    // The following are derived from the `origin` github remote if not explicitly provided:
    websiteUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin'
    issueTrackerUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin/issues'
    vcsUrl = 'https://github.com/spring-gradle-plugins/spring-bintray-plugin.git'
}
```

## Gating your release

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
* Having `mavenCentralSync` as a separate task allows you to perform validation of your release (if you choose) in a public repository (JCenter) that still allows you to delete artifacts. Maven Central does not allow this generally.