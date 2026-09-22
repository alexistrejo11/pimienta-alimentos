package io.github.alexistrejo.pimienta.pos.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test
    fun newerRemoteWins() {
        assertTrue(VersionCompare.isNewer(remoteVersionCode = 5, localVersionCode = 4))
    }

    @Test
    fun equalIsNotNewer() {
        assertFalse(VersionCompare.isNewer(remoteVersionCode = 4, localVersionCode = 4))
    }

    @Test
    fun olderRemoteIsNotNewer() {
        assertFalse(VersionCompare.isNewer(remoteVersionCode = 3, localVersionCode = 4))
    }
}
