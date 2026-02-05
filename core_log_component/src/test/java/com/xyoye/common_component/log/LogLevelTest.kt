package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogLevelTest {
    @Test
    fun isAtLeastRespectsPriorityOrder() {
        assertTrue(LogLevel.DEBUG.isAtLeast(LogLevel.DEBUG))
        assertTrue(LogLevel.INFO.isAtLeast(LogLevel.DEBUG))
        assertTrue(LogLevel.WARN.isAtLeast(LogLevel.INFO))
        assertTrue(LogLevel.ERROR.isAtLeast(LogLevel.WARN))

        assertFalse(LogLevel.DEBUG.isAtLeast(LogLevel.INFO))
        assertFalse(LogLevel.INFO.isAtLeast(LogLevel.WARN))
        assertFalse(LogLevel.WARN.isAtLeast(LogLevel.ERROR))
    }
}

