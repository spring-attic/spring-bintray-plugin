/**
 * Copyright 2017-2018 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.gradle.bintray

import org.gradle.api.Project

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

	// required for syncing to Maven Central
	var ossrhUser: String? = null
	var ossrhPassword: String? = null

	var publication: String? = null

	var gpgPassphrase: String? = null

	var overrideOnUpload: Boolean = false

	fun bintrayPackage(p: Project): BintrayPackage =
			BintrayPackage(org!!, repo!!, packageName ?: p.name)

	override fun toString(): String {
		return "bintray(org=$org, repo=$repo, packageName=$packageName, packageDescription=$packageDescription, labels=$labels, licenses=$licenses, websiteUrl=$websiteUrl, issueTrackerUrl=$issueTrackerUrl, vcsUrl=$vcsUrl, bintrayUser=$bintrayUser, bintrayKey=$bintrayKey, ossrhUser=$ossrhUser, ossrhPassword=$ossrhPassword, publication=$publication, gpgPassphrase=$gpgPassphrase, overrideOnUpload=$overrideOnUpload)"
	}
}