package com.xyoye.common_component.storage.cloud115.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Cloud115TokenParserTest {
    @Test
    fun parse_success_plainCookie() {
        val result = Cloud115TokenParser.parse("UID=123; CID=abc; SEID=def; KID=ghi").getOrThrow()
        assertEquals("123", result.userId)
        assertEquals("UID=123; CID=abc; SEID=def; KID=ghi", result.cookieHeader)
    }

    @Test
    fun parse_success_cookiePrefix_caseInsensitive_andSpaces() {
        val input = "Cookie: uid = 123 ; cid= abc; SEID =def  "
        val result = Cloud115TokenParser.parse(input).getOrThrow()
        assertEquals("123", result.userId)
        assertEquals("UID=123; CID=abc; SEID=def", result.cookieHeader)
    }

    @Test
    fun parse_ignoresUnknownCookiePairs() {
        val input = "foo=bar; UID=123; CID=abc; SEID=def; other=zzz"
        val result = Cloud115TokenParser.parse(input).getOrThrow()
        assertEquals("UID=123; CID=abc; SEID=def", result.cookieHeader)
    }

    @Test
    fun parse_fails_onMissingFields() {
        val failure = Cloud115TokenParser.parse("UID=123; CID=abc").exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message?.contains("SEID") == true)
    }

    @Test
    fun parse_fails_onInvalidUid() {
        val failure = Cloud115TokenParser.parse("UID=abc; CID=1; SEID=2").exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message?.contains("UID") == true)
    }

    @Test
    fun parse_fails_onBlankInput() {
        val failure = Cloud115TokenParser.parse("  ").exceptionOrNull()
        assertFalse(failure == null)
    }
}
