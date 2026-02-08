package com.okamihoro.ddplaytv.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xyoye.common_component.media3.Media3CrashTagger
import com.xyoye.common_component.media3.testing.Media3Dependent
import com.xyoye.data_component.media3.entity.Media3PlayerEngine
import com.xyoye.data_component.media3.entity.Media3RolloutSource
import com.xyoye.data_component.media3.entity.Media3SourceType
import com.xyoye.data_component.media3.entity.Media3ToggleCohort
import com.xyoye.data_component.media3.entity.PlaybackSession
import com.xyoye.data_component.media3.entity.RolloutToggleSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Media3Dependent("Crash reporter tagging must include Media3 cohorts")
@RunWith(AndroidJUnit4::class)
class CrashTaggingTest {
    private val reporter = RecordingCrashReporter()

    private companion object {
        private const val SESSION_TELEMETRY = "session-telemetry"
    }

    @Test
    fun media3MetadataIsStoredForCrashSegmentation() {
        Media3CrashTagger.setReporterForTest(reporter)
        Media3CrashTagger.init()

        val snapshot =
            RolloutToggleSnapshot(
                snapshotId = "snapshot-crash-test",
                value = true,
                source = Media3RolloutSource.REMOTE_CONFIG,
                evaluatedAt = 15L,
                appliesToSession = SESSION_TELEMETRY,
            )
        val session =
            PlaybackSession(
                sessionId = SESSION_TELEMETRY,
                mediaId = "media-telemetry",
                sourceType = Media3SourceType.CAST,
                playerEngine = Media3PlayerEngine.MEDIA3,
                toggleCohort = Media3ToggleCohort.TREATMENT,
            )

        Media3CrashTagger.tagSnapshot(snapshot)
        Media3CrashTagger.tagSession(session)

        assertEquals("true", reporter.userData["media3.toggle_value"])
        assertEquals("REMOTE_CONFIG", reporter.userData["media3.toggle_source"])
        assertEquals(SESSION_TELEMETRY, reporter.userData["media3.snapshot_session"])
        assertEquals("MEDIA3", reporter.userData["media3.player_engine"])
        assertEquals("TREATMENT", reporter.userData["media3.toggle_cohort"])
        assertEquals("CAST", reporter.userData["media3.source_type"])
        assertTrue(reporter.sceneTags.contains(30_003))
    }

    @After
    fun tearDown() {
        Media3CrashTagger.resetReporterForTest()
    }

    private class RecordingCrashReporter : Media3CrashTagger.CrashReporterBridge {
        val userData = linkedMapOf<String, String>()
        val sceneTags = mutableListOf<Int>()

        override fun putUserData(
            key: String,
            value: String
        ) {
            userData[key] = value
        }

        override fun setUserSceneTag(tagId: Int) {
            sceneTags += tagId
        }
    }
}
