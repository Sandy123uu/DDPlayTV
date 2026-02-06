package com.xyoye.data_component.media3.dto

import com.squareup.moshi.JsonClass
import com.xyoye.data_component.media3.entity.DownloadRequiredAction
import com.xyoye.data_component.media3.entity.Media3Capability
import com.xyoye.data_component.media3.entity.Media3PlaybackState
import com.xyoye.data_component.media3.entity.Media3PlayerEngine
import com.xyoye.data_component.media3.entity.Media3SourceType
import com.xyoye.data_component.media3.entity.PlaybackSessionMetrics
import com.xyoye.data_component.media3.entity.PlayerCapabilityContract
import kotlin.jvm.JvmSuppressWildcards

@JsonClass(generateAdapter = true)
data class PlaybackSessionRequestData(
    val mediaId: String,
    val sourceType: Media3SourceType,
    val autoplay: Boolean = true,
    val requestedCapabilities: List<Media3Capability> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlaybackSessionResponseData(
    val sessionId: String,
    val mediaId: String? = null,
    val sourceType: Media3SourceType? = null,
    val playbackState: Media3PlaybackState,
    val playerEngine: Media3PlayerEngine,
    val capabilityContract: PlayerCapabilityContract,
    val metrics: PlaybackSessionMetrics? = null
)

@JsonClass(generateAdapter = true)
data class CapabilityCommandRequestData(
    val capability: Media3Capability,
    val payload: Map<String, @JvmSuppressWildcards Any?>? = null
)

@JsonClass(generateAdapter = true)
data class CapabilityCommandResponseData(
    val sessionId: String? = null,
    val acceptedAt: Long? = null,
    val resultingState: Media3PlaybackState? = null
)

@JsonClass(generateAdapter = true)
data class DownloadValidationRequestData(
    val downloadId: String,
    val mediaId: String,
    val media3Version: String,
    val lastVerifiedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class DownloadValidationResponseData(
    val downloadId: String,
    val isCompatible: Boolean,
    val requiredAction: DownloadRequiredAction,
    val verificationLogs: List<String> = emptyList()
)
