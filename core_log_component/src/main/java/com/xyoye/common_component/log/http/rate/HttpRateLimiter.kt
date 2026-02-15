package com.xyoye.common_component.log.http.rate

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HttpRateLimiter(
    maxConcurrentRequests: Int = DEFAULT_MAX_CONCURRENT_REQUESTS,
    private val logsMinIntervalMs: Long = DEFAULT_LOGS_MIN_INTERVAL_MS,
) {
    private val semaphore = Semaphore(maxConcurrentRequests.coerceAtLeast(1))
    private val activeRequests = AtomicInteger(0)
    private val logsLastAtByIp = ConcurrentHashMap<String, AtomicLong>()

    fun tryAcquireRequest(): Boolean {
        val acquired = semaphore.tryAcquire()
        if (acquired) {
            activeRequests.incrementAndGet()
        }
        return acquired
    }

    fun releaseRequest() {
        semaphore.release()
        activeRequests.decrementAndGet()
    }

    fun activeRequestsCount(): Int = activeRequests.get().coerceAtLeast(0)

    fun allowLogsRequest(remoteIp: String): Boolean {
        val key = remoteIp.trim().ifBlank { "unknown" }
        val nowMs = System.currentTimeMillis()
        val lastRef =
            logsLastAtByIp[key] ?: run {
                val created = AtomicLong(0L)
                logsLastAtByIp.putIfAbsent(key, created) ?: created
            }
        while (true) {
            val last = lastRef.get()
            if (nowMs - last < logsMinIntervalMs) {
                return false
            }
            if (lastRef.compareAndSet(last, nowMs)) {
                return true
            }
        }
    }

    private companion object {
        private const val DEFAULT_MAX_CONCURRENT_REQUESTS = 16
        private const val DEFAULT_LOGS_MIN_INTERVAL_MS = 1000L
    }
}
