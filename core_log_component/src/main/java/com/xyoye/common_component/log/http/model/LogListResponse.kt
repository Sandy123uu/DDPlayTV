package com.xyoye.common_component.log.http.model

data class LogListResponse(
    val items: List<LogRecord>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)
