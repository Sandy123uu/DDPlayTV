package com.xyoye.common_component.log.http.model

import com.xyoye.common_component.log.model.LogLevel

data class LogRecord(
    val id: Long,
    val timestampMs: Long,
    val level: LogLevel,
    val source: LogSource,
    val message: String,
    val module: String? = null,
    val tag: String? = null,
    val context: Map<String, String>? = null,
    val thread: String? = null,
    val throwable: String? = null,
)
