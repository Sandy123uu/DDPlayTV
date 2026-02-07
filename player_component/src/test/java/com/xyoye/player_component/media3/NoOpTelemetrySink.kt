package com.xyoye.player_component.media3

import com.xyoye.data_component.media3.entity.PlaybackSession
import com.xyoye.data_component.media3.entity.RolloutToggleSnapshot
import com.xyoye.player_component.media3.telemetry.Media3TelemetrySink

class NoOpTelemetrySink : Media3TelemetrySink {
    override suspend fun recordStartup(
        session: PlaybackSession,
        snapshot: RolloutToggleSnapshot?,
        autoplay: Boolean
    ) = Unit

    override suspend fun recordFirstFrame(
        session: PlaybackSession,
        latencyMs: Long
    ) = Unit

    override suspend fun recordError(
        session: PlaybackSession,
        throwable: Throwable
    ) = Unit

    override suspend fun recordCastTransfer(
        session: PlaybackSession,
        targetId: String?
    ) = Unit
}
