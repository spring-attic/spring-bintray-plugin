package io.spring.gradle.bintray

import java.io.Serializable

/**
 * Answers the "where in bintray are these artifacts going" question
 *
 * In this context "org" could be a Bintray user if you are publishing to that user's
 * personal space.
 *
 * @author Jon Schneider
 */
data class BintrayPackage(val org: String, val repo: String, val name: String): Serializable