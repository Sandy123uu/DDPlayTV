package com.xyoye.common_component.bilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliKeysTest {
    @Test
    fun parseArchivePartKeyReturnsArchiveKeyWithCid() {
        val parsed = BilibiliKeys.parse(BilibiliKeys.archivePartKey("BV1xx411c7mD", 98765))

        assertEquals(BilibiliKeys.ArchiveKey("BV1xx411c7mD", 98765), parsed)
    }

    @Test
    fun parseLiveRoomKeyReturnsLiveKey() {
        val parsed = BilibiliKeys.parse(BilibiliKeys.liveRoomKey(12345))

        assertEquals(BilibiliKeys.LiveKey(roomId = 12345), parsed)
    }

    @Test
    fun parsePgcEpisodeKeyReturnsEpisodeKeyWithOptionalFields() {
        val key = BilibiliKeys.pgcEpisodeKey(epId = 1001, cid = 2002, seasonId = 3003, avid = 4004)
        val parsed = BilibiliKeys.parse(key)

        assertEquals(
            BilibiliKeys.PgcEpisodeKey(
                epId = 1001,
                cid = 2002,
                seasonId = 3003,
                avid = 4004,
            ),
            parsed,
        )
    }

    @Test
    fun parseInvalidOrUnsupportedKeyReturnsNull() {
        assertNull(BilibiliKeys.parse("https://example.com/video"))
        assertNull(BilibiliKeys.parse("bilibili://archive"))
    }
}
