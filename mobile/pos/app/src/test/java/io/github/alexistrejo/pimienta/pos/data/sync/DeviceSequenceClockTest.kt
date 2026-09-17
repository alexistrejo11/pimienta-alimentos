package io.github.alexistrejo.pimienta.pos.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSequenceClockTest {
    @Test
    fun freshDeviceStartsAtOne() {
        assertEquals(1L, DeviceSequenceClock.next(localNext = null, serverLast = null))
    }

    @Test
    fun continuesFromServerAfterReenrollment() {
        assertEquals(7L, DeviceSequenceClock.next(localNext = 1L, serverLast = 6L))
    }

    @Test
    fun keepsLocalWhenAheadOfServer() {
        assertEquals(10L, DeviceSequenceClock.next(localNext = 10L, serverLast = 6L))
    }

    @Test
    fun usesServerWhenLocalMissing() {
        assertEquals(4L, DeviceSequenceClock.next(localNext = null, serverLast = 3L))
    }
}
