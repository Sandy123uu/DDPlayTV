package com.xyoye.common_component.services

import com.alibaba.android.arouter.facade.template.IProvider
import com.xyoye.data_component.media3.dto.CapabilityCommandResponseData
import com.xyoye.data_component.media3.entity.Media3Capability
import com.xyoye.data_component.media3.entity.Media3SessionBundle
import com.xyoye.data_component.media3.entity.Media3SourceType
import com.xyoye.data_component.media3.entity.PlayerCapabilityContract
import com.xyoye.data_component.media3.entity.RolloutToggleSnapshot
import kotlin.jvm.JvmSuppressWildcards

interface Media3CapabilityProvider : IProvider {
    suspend fun prepareSession(
        mediaId: String,
        sourceType: Media3SourceType,
        requestedCapabilities: List<Media3Capability> = emptyList(),
        autoplay: Boolean = true
    ): Result<Media3SessionBundle>

    suspend fun refreshSession(sessionId: String): Result<Media3SessionBundle>

    suspend fun dispatchCapability(
        sessionId: String,
        capability: Media3Capability,
        payload: Map<String, @JvmSuppressWildcards Any?>? = null
    ): Result<CapabilityCommandResponseData>

    fun cachedCapability(sessionId: String): PlayerCapabilityContract?

    fun cachedToggle(sessionId: String): RolloutToggleSnapshot?
}
