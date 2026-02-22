package com.xyoye.common_component.log.store

import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.RetentionTier
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SegmentedJsonlLogStore(
    private val logsDir: File,
    private val retentionTierProvider: () -> RetentionTier,
) : LogStore {
    private val writer = SegmentedJsonlLogWriter(logsDir)

    private val persistencePaused = AtomicBoolean(false)
    private val lastError = AtomicReference<String?>(null)

    override fun append(record: LogRecord) {
        if (persistencePaused.get()) {
            return
        }
        val result = writer.append(record)
        if (!result.success) {
            lastError.set(result.errorMessage)
            persistencePaused.set(true)
            return
        }
        writer.enforceRetention(retentionTierProvider(), force = false)
    }

    override fun clear() {
        persistencePaused.set(false)
        lastError.set(null)
        writer.clearAll()
    }

    override fun snapshot(): LogStoreState {
        val retention = retentionTierProvider()
        val usedBytes = writer.calculateUsedBytes()
        return LogStoreState(
            retention = retention,
            usedBytes = usedBytes,
            persistencePaused = persistencePaused.get(),
            lastError = lastError.get(),
        )
    }

    fun enforceRetentionNow(): Boolean = writer.enforceRetention(retentionTierProvider(), force = true)

    fun pausePersistence(reason: String) {
        lastError.set(reason)
        persistencePaused.set(true)
    }

    fun tryResumePersistence(): Boolean {
        if (!persistencePaused.get()) return true
        persistencePaused.set(false)
        lastError.set(null)
        return true
    }

    fun close() {
        writer.close()
    }
}
