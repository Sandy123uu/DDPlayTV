package com.xyoye.common_component.log.store

import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.LogSource
import com.xyoye.common_component.log.http.model.LogSourceFilter
import com.xyoye.common_component.log.http.query.HttpLogQuery
import java.io.File
import java.util.Locale

internal class SegmentedJsonlLogReader(
    private val logsDir: File,
) {
    private val adapter = HttpLogJson.adapter(LogRecord::class.java)

    fun query(query: HttpLogQuery): LogQueryResult {
        if (!logsDir.exists() || !logsDir.isDirectory) {
            return LogQueryResult(items = emptyList(), nextCursor = null, hasMore = false)
        }

        val cursorId = query.cursor?.toLongOrNull() ?: Long.MAX_VALUE
        val limit = query.limit
        val items = ArrayList<LogRecord>(minOf(limit, 500))

        var nextCursor: String? = null
        var hasMore = false

        val files = listSegmentFilesSortedNewestFirst()
        outer@ for (file in files) {
            val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull().orEmpty()
            var index = lines.size - 1
            while (index >= 0) {
                val line = lines[index].trim()
                index--
                if (line.isEmpty()) continue
                val record = runCatching { adapter.fromJson(line) }.getOrNull() ?: continue
                if (record.id >= cursorId) continue
                if (!matches(record, query)) continue

                if (items.size < limit) {
                    items.add(record)
                    if (items.size == limit) {
                        nextCursor = record.id.toString()
                        continue
                    }
                } else {
                    hasMore = true
                    break@outer
                }
            }
        }

        if (items.isNotEmpty() && nextCursor != null && !hasMore) {
            hasMore = hasMoreAfter(files, query, cursorId = nextCursor!!.toLong())
        }

        return LogQueryResult(
            items = items,
            nextCursor = nextCursor,
            hasMore = hasMore,
        )
    }

    private fun hasMoreAfter(
        filesNewestFirst: List<File>,
        query: HttpLogQuery,
        cursorId: Long,
    ): Boolean {
        outer@ for (file in filesNewestFirst) {
            val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull().orEmpty()
            for (idx in lines.size - 1 downTo 0) {
                val line = lines[idx].trim()
                if (line.isEmpty()) continue
                val record = runCatching { adapter.fromJson(line) }.getOrNull() ?: continue
                if (record.id >= cursorId) continue
                if (!matches(record, query)) continue
                return true
            }
        }
        return false
    }

    private fun matches(
        record: LogRecord,
        query: HttpLogQuery,
    ): Boolean {
        if (query.startMs != null && record.timestampMs < query.startMs) return false
        if (query.endMs != null && record.timestampMs > query.endMs) return false
        if (query.levels != null && record.level !in query.levels) return false
        if (query.tag != null) {
            val tag = record.tag?.trim().orEmpty()
            if (!tag.startsWith(query.tag, ignoreCase = false)) return false
        }
        if (query.keyword != null) {
            val keyword = query.keyword.trim()
            if (keyword.isNotEmpty() && !record.message.contains(keyword, ignoreCase = true)) return false
        }
        when (query.source) {
            LogSourceFilter.BOTH -> Unit
            LogSourceFilter.APP -> if (record.source != LogSource.APP) return false
            LogSourceFilter.LOGCAT -> if (record.source != LogSource.LOGCAT) return false
        }
        return true
    }

    private fun listSegmentFilesSortedNewestFirst(): List<File> =
        logsDir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.lowercase(Locale.US).endsWith(SEGMENT_SUFFIX) }
            .sortedByDescending { it.lastModified() }

    private companion object {
        private const val SEGMENT_SUFFIX = ".jsonl"
    }
}
