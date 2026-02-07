package com.xyoye.common_component.bilibili.ticket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliTicketSignerTest {
    @Test
    fun signUsesStableHmacForGivenTimestamp() {
        val signed = BilibiliTicketSigner.sign(timestampSec = 1700000000)

        assertEquals("ec02", signed.keyId)
        assertEquals(1700000000, signed.timestampSec)
        assertEquals("bb79f0d980ffbb51597aa1a3e8b55603025cc1322ac766f4c1a98852e6182514", signed.hexsign)
    }

    @Test
    fun signReturnsLowercaseHexSha256() {
        val signed = BilibiliTicketSigner.sign(timestampSec = 1)

        assertEquals(64, signed.hexsign.length)
        assertTrue(signed.hexsign.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
