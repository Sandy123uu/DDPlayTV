package com.xyoye.player_component.ui.activities.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackPressExitConfirmGuardTest {
    @Test
    fun shouldInterceptOnFirstBackPress() {
        var nowMs = 10_000L
        val guard = BackPressExitConfirmGuard(confirmWindowMs = 1500L) { nowMs }

        assertTrue(guard.shouldInterceptExit())
    }

    @Test
    fun shouldAllowExitOnSecondBackPressWithinWindow() {
        var nowMs = 10_000L
        val guard = BackPressExitConfirmGuard(confirmWindowMs = 1500L) { nowMs }

        assertTrue(guard.shouldInterceptExit())

        nowMs += 800L
        assertFalse(guard.shouldInterceptExit())
    }

    @Test
    fun shouldRequireConfirmAgainAfterWindowTimeout() {
        var nowMs = 10_000L
        val guard = BackPressExitConfirmGuard(confirmWindowMs = 1500L) { nowMs }

        assertTrue(guard.shouldInterceptExit())

        nowMs += 1600L
        assertTrue(guard.shouldInterceptExit())
    }

    @Test
    fun shouldRequireConfirmAfterReset() {
        var nowMs = 10_000L
        val guard = BackPressExitConfirmGuard(confirmWindowMs = 1500L) { nowMs }

        assertTrue(guard.shouldInterceptExit())

        nowMs += 200L
        guard.reset()
        assertTrue(guard.shouldInterceptExit())
    }
}
