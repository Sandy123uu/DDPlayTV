package com.xyoye.common_component.log.store

import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.RetentionTier
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale

internal data class JsonlAppendResult(
    val success: Boolean,
    val errorMessage: String? = null,
)

internal class SegmentedJsonlLogWriter(
    private val logsDir: File,
    private val segmentMaxBytes: Long = DEFAULT_SEGMENT_MAX_BYTES,
    private val segmentMaxAgeMs: Long = DEFAULT_SEGMENT_MAX_AGE_MS,
    private val cleanupMinIntervalMs: Long = DEFAULT_CLEANUP_MIN_INTERVAL_MS,
) {
    private val adapter = HttpLogJson.adapter(LogRecord::class.java)
    private val lock = Any()

    private var currentSegment: SegmentState? = null
    private var lastCleanupAtMs: Long = 0L

    fun append(record: LogRecord): JsonlAppendResult {
        synchronized(lock) {
            val nowMs = System.currentTimeMillis()
            if (!ensureLogsDir()) {
                return JsonlAppendResult(success = false, errorMessage = "failed to create logs dir")
            }
            val segment = ensureSegment(nowMs) ?: return JsonlAppendResult(success = false, errorMessage = "failed to open segment")
            val json = adapter.toJson(record)
            return runCatching {
                segment.writer.write(json)
                segment.writer.newLine()
                segment.writer.flush()
                segment.bytesWritten += json.length + 1L
                JsonlAppendResult(success = true)
            }.getOrElse { error ->
                JsonlAppendResult(success = false, errorMessage = error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun enforceRetention(
        retention: RetentionTier,
        force: Boolean = false,
    ): Boolean {
        synchronized(lock) {
            val nowMs = System.currentTimeMillis()
            if (!force && nowMs - lastCleanupAtMs < cleanupMinIntervalMs) {
                return false
            }
            lastCleanupAtMs = nowMs
            val segments = listSegmentFilesSortedOldestFirst()
            if (segments.isEmpty()) return false

            val cutoffMs = nowMs - retention.days * DAY_MS
            var deletedAny = false

            segments
                .filter { it.lastModified() in 1 until cutoffMs }
                .forEach { file ->
                    deletedAny = file.delete() || deletedAny
                }

            val remaining = listSegmentFilesSortedOldestFirst()
            var totalBytes = remaining.sumOf { it.length().coerceAtLeast(0L) }
            if (totalBytes <= retention.maxBytes) return deletedAny

            remaining.forEach { file ->
                if (totalBytes <= retention.maxBytes) return@forEach
                val length = file.length().coerceAtLeast(0L)
                if (file.delete()) {
                    totalBytes -= length
                    deletedAny = true
                }
            }
            return deletedAny
        }
    }

    fun calculateUsedBytes(): Long {
        synchronized(lock) {
            if (!logsDir.exists() || !logsDir.isDirectory) return 0L
            return logsDir.listFiles().orEmpty().filter(File::isFile).sumOf { it.length().coerceAtLeast(0L) }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            currentSegment?.close()
            currentSegment = null
            if (!logsDir.exists() || !logsDir.isDirectory) return
            logsDir.listFiles().orEmpty().filter(File::isFile).forEach { it.delete() }
        }
    }

    fun close() {
        synchronized(lock) {
            currentSegment?.close()
            currentSegment = null
        }
    }

    private fun ensureSegment(nowMs: Long): SegmentState? {
        val current = currentSegment
        if (current == null || shouldRotate(current, nowMs)) {
            current?.close()
            currentSegment = null

            val file = File(logsDir, buildSegmentName(nowMs))
            val writer =
                runCatching {
                    BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))
                }.getOrNull() ?: return null
            val state = SegmentState(file = file, createdAtMs = nowMs, writer = writer, bytesWritten = file.length())
            currentSegment = state
            return state
        }
        return current
    }

    private fun shouldRotate(
        segment: SegmentState,
        nowMs: Long,
    ): Boolean {
        if (segment.bytesWritten >= segmentMaxBytes) return true
        val age = nowMs - segment.createdAtMs
        return age >= segmentMaxAgeMs
    }

    private fun ensureLogsDir(): Boolean {
        if (logsDir.exists()) return logsDir.isDirectory
        return logsDir.mkdirs()
    }

    private fun listSegmentFilesSortedOldestFirst(): List<File> =
        logsDir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.lowercase(Locale.US).endsWith(SEGMENT_SUFFIX) }
            .sortedBy { it.lastModified() }

    private fun buildSegmentName(nowMs: Long): String = "${SEGMENT_PREFIX}${nowMs}${SEGMENT_SUFFIX}"

    private data class SegmentState(
        val file: File,
        val createdAtMs: Long,
        val writer: BufferedWriter,
        var bytesWritten: Long,
    ) {
        fun close() {
            runCatching { writer.close() }
        }
    }

    private companion object {
        private const val SEGMENT_PREFIX = "seg_"
        private const val SEGMENT_SUFFIX = ".jsonl"

        private const val DAY_MS = 24L * 60L * 60L * 1000L

        private const val DEFAULT_SEGMENT_MAX_BYTES = 8L * 1024L * 1024L
        private const val DEFAULT_SEGMENT_MAX_AGE_MS = 60L * 60L * 1000L
        private const val DEFAULT_CLEANUP_MIN_INTERVAL_MS = 30_000L
    }
}
