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