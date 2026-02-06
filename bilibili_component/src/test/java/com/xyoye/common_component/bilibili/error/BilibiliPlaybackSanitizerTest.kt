package com.xyoye.common_component.bilibili.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliPlaybackSanitizerTest {
    @Test
    fun sanitizeValueRedactsCookieAndAuthorization() {
        assertEquals("<redacted>", BilibiliPlaybackSanitizer.sanitizeValue("cookie", "SESSDATA=abc"))
        assertEquals("<redacted>", BilibiliPlaybackSanitizer.sanitizeValue("Authorization", "Bearer token"))
    }

    @Test
    fun sanitizeUrlKeepsAllowlistedQueryValuesOnly() {
        val raw = "https://example.com/video.mp4?token=abc&ts=123456&quality=80&expires=9999"
        val sanitized = BilibiliPlaybackSanitizer.sanitizeUrl(raw)

        assertEquals(
            "https://example.com/video.mp4?expires=9999&quality=80&token=<redacted>&ts=123456",
            sanitized,
        )
    }

    @Test
    fun sanitizeUrlHandlesInvalidAndNonHttpScheme() {
        assertEquals("https://:invalid", BilibiliPlaybackSanitizer.sanitizeUrl("https://:invalid"))
        val fileUrl = "file:///sdcard/movie.mp4?token=abc"
        val sanitized = BilibiliPlaybackSanitizer.sanitizeUrl(fileUrl)
        assertEquals(fileUrl, sanitized)
    }
}
