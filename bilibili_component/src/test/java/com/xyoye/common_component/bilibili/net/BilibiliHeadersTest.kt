package com.xyoye.common_component.bilibili.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliHeadersTest {
    @Test
    fun withCookieAddsCookieAndKeepsDefaultHeaders() {
        val headers = BilibiliHeaders.withCookie("SESSDATA=abc; bili_jct=token")

        assertEquals(BilibiliHeaders.REFERER, headers[BilibiliHeaders.HEADER_REFERER])
        assertEquals(BilibiliHeaders.USER_AGENT, headers[BilibiliHeaders.HEADER_USER_AGENT])
        assertEquals(BilibiliHeaders.ACCEPT_ENCODING, headers[BilibiliHeaders.HEADER_ACCEPT_ENCODING])
        assertEquals("SESSDATA=abc; bili_jct=token", headers[BilibiliHeaders.HEADER_COOKIE])
    }

    @Test
    fun redactHeadersMasksCookieOnly() {
        val redacted =
            BilibiliHeaders.redactHeaders(
                mapOf(
                    BilibiliHeaders.HEADER_COOKIE to "SESSDATA=abc",
                    BilibiliHeaders.HEADER_REFERER to "https://www.bilibili.com/",
                ),
            )

        assertEquals("Cookie(<redacted>)", redacted[BilibiliHeaders.HEADER_COOKIE])
        assertEquals("https://www.bilibili.com/", redacted[BilibiliHeaders.HEADER_REFERER])
    }

    @Test
    fun withCookieSkipsBlankCookieValue() {
        val headers = BilibiliHeaders.withCookie(cookieHeader = "  ")

        assertFalse(headers.containsKey(BilibiliHeaders.HEADER_COOKIE))
        assertTrue(headers.containsKey(BilibiliHeaders.HEADER_REFERER))
    }
}
