package com.xyoye.common_component.log.store

import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.RetentionTier
import com.xyoye.common_component.log.http.query.HttpLogQuery

data class LogQueryResult(
    val items: List<LogRecord>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class LogStoreState(
    val retention: RetentionTier,
    val usedBytes: Long,
    val persistencePaused: Boolean,
    val lastError: String?,
)

interface LogStore {
    fun append(record: LogRecord)

    fun query(query: HttpLogQuery): LogQueryResult

    fun clear()

    fun snapshot(): LogStoreState
}

