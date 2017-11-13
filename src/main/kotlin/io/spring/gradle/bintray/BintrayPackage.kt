package io.spring.gradle.bintray

import java.io.Serializable

/**
 * Answers the "where in bintray are these artifacts going" question
 *
 * In this context "subject" could be an organization name or a Bintray user.
 *
 * @author Jon Schneider
 */
data class BintrayPackage(val subject: String, val repo: String, val name: String): Serializable