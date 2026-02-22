package com.xyoye.common_component.log.http.model

internal data class LogSegmentSummary(
    val fileName: String,
    val startMs: Long,
    val endMs: Long?,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val isLatest: Boolean,
)

internal data class LogSegmentsResponse(
    val segments: List<LogSegmentSummary>,
    val totalCount: Int,
    val totalBytes: Long,
    val latestStartMs: Long?,
    val oldestStartMs: Long?,
)
