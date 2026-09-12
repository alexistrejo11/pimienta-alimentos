package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingModePolicyTest {
    @Test
    fun debugBuildNeverRequiresPinForModeSwitch() {
        assertFalse(TrainingModePolicy.requiresPinForModeSwitch(isDebug = true, isEnrolled = true))
        assertFalse(TrainingModePolicy.requiresPinForModeSwitch(isDebug = true, isEnrolled = false))
    }

    @Test
    fun releaseRequiresPinOnlyWhenEnrolled() {
        assertFalse(TrainingModePolicy.requiresPinForModeSwitch(isDebug = false, isEnrolled = false))
        assertTrue(TrainingModePolicy.requiresPinForModeSwitch(isDebug = false, isEnrolled = true))
    }
}
