package com.xyoye.common_component.log.http.model

data class StatusResponse(
    val enabled: Boolean,
    val running: Boolean,
    val requestedPort: Int,
    val boundPort: Int,
    val clientCount: Int,
    val ipAddresses: List<String> = emptyList(),
    val retention: RetentionTier,
    val storeUsedBytes: Long,
    val logcatAvailable: Boolean,
    val degradeMode: HttpDegradeMode,
    val message: String? = null,
)
