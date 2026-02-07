package com.xyoye.common_component.bilibili.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliAppSignerTest {
    @Test
    fun signAppendsAppKeyAndTsAndCalculatesSign() {
        val signed =
            BilibiliAppSigner.sign(
                params = mapOf("b" to "2", "a" to "1"),
                appKey = "testKey",
                appSec = "secret",
                tsSeconds = 100,
            )

        assertEquals("1", signed["a"])
        assertEquals("2", signed["b"])
        assertEquals("testKey", signed["appkey"])
        assertEquals(100L, signed["ts"])
        assertEquals("201f171a9c4f9ee1e545ca78f1981dec", signed["sign"])
    }

    @Test
    fun signUsesProvidedTsAndOverridesExistingSign() {
        val signed =
            BilibiliAppSigner.sign(
                params = mapOf("a" to "1", "ts" to 42, "sign" to "legacy"),
                appKey = "testKey",
                appSec = "secret",
                tsSeconds = 100,
            )

        assertEquals(42, signed["ts"])
        assertFalse((signed["sign"] as String).equals("legacy", ignoreCase = true))
        assertEquals("b6cc9f7332bc95c7d8a2c4f18c5c35ac", signed["sign"])
    }
}
