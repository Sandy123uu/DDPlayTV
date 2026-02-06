package com.okamihoro.ddplaytv.app.cast

import com.xyoye.data_component.media3.entity.CastTarget
import com.xyoye.data_component.media3.entity.CastTargetType
import com.xyoye.data_component.media3.entity.Media3Capability
import com.xyoye.data_component.media3.entity.Media3PlayerEngine
import com.xyoye.data_component.media3.entity.Media3SourceType
import com.xyoye.data_component.media3.entity.PlaybackSession
import com.xyoye.data_component.media3.entity.PlayerCapabilityContract
import com.xyoye.player_component.media3.fallback.CodecFallbackHandler
import com.xyoye.player_component.media3.mapper.LegacyCapabilityIssue
import com.xyoye.player_component.media3.mapper.LegacyCapabilityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3CastManagerTest {
    @Test
    fun returnsDisabledResult_whenCastSenderDisabled() {
        val manager = Media3CastManager(codecFallbackHandler = CodecFallbackHandler())

        val result =
            manager.prepareCastSession(
                targetId = "living-room",
                session = playbackSession(),
                capability = capabilityContract("living-room"),
                capabilityResult = null,
            )

        assertTrue(result is CastSessionPrepareResult.Disabled)
        val disabled = result as CastSessionPrepareResult.Disabled
        assertEquals("TV 端不支持发起投屏，请在接收端开启投屏接收后再连接", disabled.message)
    }

    @Test
    fun returnsReadyPayload_whenCastSenderEnabled() {
        val manager = Media3CastManager(codecFallbackHandler = CodecFallbackHandler(), castSenderEnabled = true)
        val session = playbackSession()

        val result =
            manager.prepareCastSession(
                targetId = "living-room",
                session = session,
                capability = capabilityContract("living-room"),
                capabilityResult =
                    LegacyCapabilityResult(
                        mediaTracks = emptyList(),
                        subtitleTracks = emptyList(),
                        issues =
                            listOf(
                                LegacyCapabilityIssue(
                                    code = "UNSUPPORTED_CODEC",
                                    message = "Codec h265 not supported on cast target",
                                    blocking = true,
                                ),
                            ),
                    ),
            )

        assertTrue(result is CastSessionPrepareResult.Ready)
        val payload = (result as CastSessionPrepareResult.Ready).payload
        assertEquals("living-room", payload.target.id)
        assertEquals(session.sessionId, payload.sessionId)
        assertTrue(payload.audioOnly)
        assertEquals("Codec h265 not supported on cast target", payload.fallbackMessage)
    }

    @Test
    fun returnsUnsupportedTarget_whenTargetMissing() {
        val manager = Media3CastManager(codecFallbackHandler = CodecFallbackHandler(), castSenderEnabled = true)

        val result =
            manager.prepareCastSession(
                targetId = "missing-target",
                session = playbackSession(),
                capability = capabilityContract("living-room"),
                capabilityResult = null,
            )

        assertTrue(result is CastSessionPrepareResult.UnsupportedTarget)
        val unsupported = result as CastSessionPrepareResult.UnsupportedTarget
        assertEquals("missing-target", unsupported.targetId)
    }

    private fun playbackSession(): PlaybackSession =
        PlaybackSession(
            sessionId = "session-cast",
            mediaId = "media-1",
            sourceType = Media3SourceType.STREAM,
            playerEngine = Media3PlayerEngine.MEDIA3,
        )

    private fun capabilityContract(targetId: String): PlayerCapabilityContract =
        PlayerCapabilityContract(
            sessionId = "session-cast",
            capabilities = listOf(Media3Capability.CAST),
            castTargets =
                listOf(
                    CastTarget(id = targetId, name = "Living Room", type = CastTargetType.CHROMECAST),
                ),
        )
}
