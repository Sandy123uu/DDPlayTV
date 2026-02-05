package com.xyoye.common_component.log.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SensitiveDataSanitizerTest {
    @Test
    fun sanitizeUrlStripsQueryAndFragment() {
        val raw = "https://example.com/a/b.m3u8?token=abc&sign=xyz#frag"
        val safe = SensitiveDataSanitizer.sanitizeUrl(raw, SensitiveDataSanitizer.UrlMode.SAFE)
        assertEquals("https://example.com/a/b.m3u8", safe)
        assertFalse(safe.contains("?"))
        assertFalse(safe.contains("#"))
        assertFalse(safe.contains("abc"))
        assertFalse(safe.contains("xyz"))
    }

    @Test
    fun sanitizeMagnetKeepsOnlyBtihHash() {
        val raw = "magnet:?xt=urn:btih:0123456789ABCDEF&dn=name&tr=udp://tracker"
        val safe = SensitiveDataSanitizer.sanitizeMagnet(raw)
        assertTrue(safe.startsWith("magnet:btih="))
        assertTrue(safe.contains("0123456789ABCDEF"))
        assertFalse(safe.contains("dn="))
        assertFalse(safe.contains("tr="))
    }

    @Test
    fun sanitizeContextRedactsSensitiveKeys() {
        val ctx =
            mapOf(
                "Authorization" to "Bearer secret-token",
                "cookie" to "SESSDATA=abc; bili_jct=def",
                "path" to "/storage/emulated/0/Movies/private.mp4",
                "url" to "https://a.com/v.m3u8?access_token=aaa",
            )
        val safe = SensitiveDataSanitizer.sanitizeContext(ctx)
        assertEquals("<redacted>", safe.getValue("Authorization"))
        assertEquals("<redacted>", safe.getValue("cookie"))
        assertTrue(safe.getValue("path").startsWith("private.mp4#"))
        assertEquals("https://a.com/v.m3u8", safe.getValue("url"))
    }

    @Test
    fun sanitizeFreeTextRedactsBearerAndQueryTokens() {
        val raw = "Authorization: Bearer abcdef token=zzz https://h.com/p?q=1&access_token=aaa"
        val safe = SensitiveDataSanitizer.sanitizeFreeText(raw)
        assertFalse(safe.contains("abcdef"))
        assertFalse(safe.contains("zzz"))
        assertFalse(safe.contains("access_token=aaa"))
        assertTrue(safe.contains("<redacted>"))
        assertTrue(safe.contains("https://h.com/p"))
    }
}

