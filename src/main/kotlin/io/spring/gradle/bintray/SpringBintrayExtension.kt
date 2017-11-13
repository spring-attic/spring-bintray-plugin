package io.spring.gradle.bintray

open class SpringBintrayExtension {
    // package info
    var org: String? = null
    var repo: String? = null
    var packageName: String? = null
    var packageDescription: String? = null
    var labels: Collection<String> = emptyList()
    var licenses: Collection<String>? = null

    // optional since: derived from the "origin" github remote or the first github remote, if any
    var websiteUrl: String? = null
    var issueTrackerUrl: String? = null
    var vcsUrl: String? = null

    // every put/post request to bintray must be authenticated
    var bintrayUser: String? = null
    var bintrayKey: String? = null

    var publication: String? = null
}