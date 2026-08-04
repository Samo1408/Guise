package com.houvven.guise.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdaterTest {
    @Test
    fun prereleaseBuildChecksBothChannels() {
        assertEquals(
            listOf("latest-prerelease.json", "latest-release.json"),
            updateManifestNames("2.0.0-beta.4"),
        )
    }

    @Test
    fun releaseBuildChecksOnlyReleaseChannel() {
        assertEquals(
            listOf("latest-release.json"),
            updateManifestNames("2.0.0"),
        )
    }

    @Test
    fun releaseCandidateIsTreatedAsPrerelease() {
        assertEquals(
            listOf("latest-prerelease.json", "latest-release.json"),
            updateManifestNames("2.0.0-rc.1"),
        )
    }

    @Test
    fun stableChannelRejectsPrereleaseManifest() {
        assertThrows(IllegalStateException::class.java) {
            validateUpdateChannel(actualPrerelease = true, expectedPrerelease = false)
        }
    }

    @Test
    fun matchingManifestChannelIsAccepted() {
        validateUpdateChannel(actualPrerelease = false, expectedPrerelease = false)
        validateUpdateChannel(actualPrerelease = true, expectedPrerelease = true)
    }
}
