package com.xyoye.common_component.source.media3

import com.xyoye.data_component.media3.entity.Media3Capability
import com.xyoye.data_component.media3.entity.Media3SourceType

data class Media3LaunchParams(
    val mediaId: String,
    val sourceType: Media3SourceType,
    val requestedCapabilities: List<Media3Capability> = emptyList(),
    val autoplay: Boolean = true
)
