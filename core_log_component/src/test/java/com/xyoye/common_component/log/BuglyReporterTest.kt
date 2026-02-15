package com.xyoye.common_component.log

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuglyReporterTest {
    private lateinit var bridge: RecordingBridge

    @Before
    fun setUp() {
        bridge = RecordingBridge()
        BuglyReporter.resetForTest()
        BuglyReporter.setBridgeForTest(bridge)
        BuglyReporter.init(
            context = ApplicationProvider.getApplicationContext(),
            appId = "test_app_id",
            debug = true,
        )
    }

    @After
    fun tearDown() {
        BuglyReporter.resetForTest()
    }

    @Test
    fun updateContext_sanitizesUrl() {
        BuglyReporter.updateContext("play.src", "https://example.com/a/b?token=1#frag")
        assertEquals("https://example.com/a/b", bridge.userData["play.src"])
    }

    @Test
    fun updateContext_dedupesSameValue() {
        BuglyReporter.updateContext("mpv.event", "Prepared")
        BuglyReporter.updateContext("mpv.event", "Prepared")
        assertEquals(1, bridge.callCount["mpv.event"])
    }

    @Test
    fun recordBreadcrumb_writesBcKeysAndSanitizesInlineUrl() {
        BuglyReporter.recordBreadcrumb(
            tag = "mpv",
            message = "setDataSource",
            context = mapOf("url" to "https://example.com/x?token=secret"),
        )
        val bc0 = bridge.userData["bc.0"]
        assertNotNull(bc0)
        assertTrue(bc0!!.contains("https://example.com/x"))
        assertFalse(bc0.contains("token="))
    }

    @Test
    fun updateContext_truncatesLongValue() {
        BuglyReporter.updateContext("mpv.last_error", "a".repeat(500))
        val value = bridge.userData["mpv.last_error"]
        assertNotNull(value)
        assertTrue(value!!.length <= 200)
    }

    private class RecordingBridge : BuglyReporter.BuglyBridge {
        val userData = LinkedHashMap<String, String>()
        val callCount = LinkedHashMap<String, Int>()

        override fun initCrashReport(
            context: android.content.Context,
            appId: String,
            debug: Boolean
        ) {
        }

        override fun postCatchedException(throwable: Throwable) {
        }

        override fun getAppId(): String? = "test_app_id"

        override fun getBuglyVersion(context: android.content.Context): String? = "test"

        override fun putUserData(
            context: android.content.Context,
            key: String,
            value: String
        ) {
            userData[key] = value
            callCount[key] = (callCount[key] ?: 0) + 1
        }

        override fun setUserSceneTag(
            context: android.content.Context,
            tagId: Int
        ) {
        }

        override fun logInfo(
            tag: String,
            message: String
        ) {
        }

        override fun logWarn(
            tag: String,
            message: String
        ) {
        }

        override fun logError(
            tag: String,
            message: String
        ) {
        }
    }
}

