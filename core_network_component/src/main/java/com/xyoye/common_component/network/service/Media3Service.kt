package com.xyoye.common_component.network.service

import com.xyoye.data_component.media3.dto.CapabilityCommandRequestData
import com.xyoye.data_component.media3.dto.CapabilityCommandResponseData
import com.xyoye.data_component.media3.dto.DownloadValidationRequestData
import com.xyoye.data_component.media3.dto.DownloadValidationResponseData
import com.xyoye.data_component.media3.dto.PlaybackSessionRequestData
import com.xyoye.data_component.media3.dto.PlaybackSessionResponseData
import com.xyoye.data_component.media3.entity.TelemetryEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface Media3Service {
    @POST("v1/media3/sessions")
    suspend fun createSession(
        @Body request: PlaybackSessionRequestData
    ): PlaybackSessionResponseData

    @GET("v1/media3/sessions/{sessionId}")
    suspend fun fetchSession(
        @Path("sessionId") sessionId: String
    ): PlaybackSessionResponseData

    @POST("v1/media3/sessions/{sessionId}/commands")
    suspend fun dispatchCommand(
        @Path("sessionId") sessionId: String,
        @Body request: CapabilityCommandRequestData
    ): CapabilityCommandResponseData

    @POST("v1/media3/telemetry")
    suspend fun emitTelemetry(
        @Body event: TelemetryEvent
    ): Response<Unit>

    @POST("v1/media3/downloads/validate")
    suspend fun validateDownload(
        @Body request: DownloadValidationRequestData
    ): DownloadValidationResponseData
}
