/**
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
package io.spring.gradle.bintray.task

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.publish.maven.MavenPublication
import org.junit.jupiter.api.Test

class MavenCentralSyncTaskTest {
	@Test
	fun existsInMavenCentral() {
		val pub = mock<MavenPublication> {
			on { groupId } doReturn "org.springframework.boot"
			on { artifactId } doReturn  "spring-boot-starter-actuator"
			on { version } doReturn "2.0.0.RELEASE"
		}

		assertThat(MavenCentral.exists(pub)).isTrue()
	}

	@Test
	fun doesNotExistInMavenCentral() {
		val pub = mock<MavenPublication> {
			on { groupId } doReturn "org.springframework.boot"
			on { artifactId } doReturn  "spring-boot-starter-actuator"
			on { version } doReturn "DOES.NOT.EXIST"
		}

		assertThat(MavenCentral.exists(pub)).isFalse()
	}
}