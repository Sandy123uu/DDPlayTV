package com.xyoye.common_component.log.store

import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.RetentionTier

data class LogStoreState(
    val retention: RetentionTier,
    val usedBytes: Long,
    val persistencePaused: Boolean,
    val lastError: String?,
)

interface LogStore {
    fun append(record: LogRecord)

    fun clear()

    fun snapshot(): LogStoreState
}
