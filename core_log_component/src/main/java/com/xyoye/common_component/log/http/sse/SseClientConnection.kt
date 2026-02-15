package com.xyoye.common_component.log.http.sse

import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class SseClientConnection(
    private val queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    private val keepAliveIntervalMs: Long = DEFAULT_KEEPALIVE_INTERVAL_MS,
    private val onClosed: (SseClientConnection) -> Unit,
) {
    private val running = AtomicBoolean(true)
    private val queue = ArrayBlockingQueue<ByteArray>(queueCapacity)

    private val inputStream = PipedInputStream(PIPE_BUFFER_SIZE)
    private val outputStream = PipedOutputStream(inputStream)

    private val writerThread =
        Thread(
            { writeLoop() },
            "HttpLogSseClient",
        ).apply { isDaemon = true }

    fun start() {
        writerThread.start()
    }

    fun inputStream(): InputStream = inputStream

    fun offer(payload: ByteArray): Boolean {
        if (!running.get()) return false
        return queue.offer(payload)
    }

    fun offerDropOldest(payload: ByteArray): Boolean {
        if (!running.get()) return false
        if (queue.offer(payload)) return true
        queue.poll()
        return queue.offer(payload)
    }

    fun close() {
        if (!running.getAndSet(false)) return
        runCatching { outputStream.close() }
        runCatching { inputStream.close() }
        onClosed(this)
    }

    private fun writeLoop() {
        try {
            while (running.get()) {
                val payload = queue.poll(keepAliveIntervalMs, TimeUnit.MILLISECONDS)
                if (payload == null) {
                    outputStream.write(KEEPALIVE_BYTES)
                    outputStream.flush()
                    continue
                }
                outputStream.write(payload)
                outputStream.flush()
            }
        } catch (_: InterruptedException) {
            // ignore
        } catch (_: Exception) {
            // ignore
        } finally {
            close()
        }
    }

    private companion object {
        private const val DEFAULT_QUEUE_CAPACITY = 256
        private const val DEFAULT_KEEPALIVE_INTERVAL_MS = 15_000L
        private const val PIPE_BUFFER_SIZE = 16 * 1024

        private val KEEPALIVE_BYTES = ": keepalive\n\n".toByteArray(Charsets.UTF_8)
    }
}

