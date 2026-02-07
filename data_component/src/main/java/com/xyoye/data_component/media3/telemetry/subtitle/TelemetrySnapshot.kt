package com.xyoye.data_component.media3.telemetry.subtitle

import com.xyoye.data_component.enums.SubtitlePipelineMode
import com.xyoye.data_component.media3.telemetry.subtitle.FallbackEvent

data class TelemetrySnapshot(
    val windowMs: Long? = null,
    val renderedFrames: Int = 0,
    val droppedFrames: Int = 0,
    val vsyncHitRate: Double? = null,
    val cpuPeakPct: Double? = null,
    val mode: SubtitlePipelineMode? = null,
    val lastFallback: FallbackEvent? = null
)
