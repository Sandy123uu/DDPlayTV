package com.xyoye.common_component.bilibili.repository

import com.xyoye.common_component.bilibili.BilibiliKeys
import com.xyoye.common_component.bilibili.BilibiliPlaybackPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BilibiliRepositoryFacadeContractTest {
    @Test
    fun facadeExposesDomainRepositories() {
        val repository = BilibiliRepository(storageKey = "contract-test")

        assertNotNull(repository.auth)
        assertNotNull(repository.risk)
        assertNotNull(repository.history)
        assertNotNull(repository.live)
        assertNotNull(repository.playback)
        assertNotNull(repository.danmaku)
    }

    @Test
    fun playbackPreferencesProvideStableDefaultsForFacadeCalls() {
        val preferences = BilibiliPlaybackPreferences()

        assertTrue(preferences.preferredQualityQn > 0)
        assertEquals(BilibiliKeys.ArchiveKey("BV1xx411c7mD", 1), BilibiliKeys.parse(BilibiliKeys.archivePartKey("BV1xx411c7mD", 1)))
    }
}
