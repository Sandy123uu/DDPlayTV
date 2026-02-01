package com.xyoye.common_component.log.tcp

import com.xyoye.common_component.config.LogConfig

/**
 * Runtime manager for [TcpLogServer].
 *
 * - Reads/writes persisted config via [LogConfig]
 * - Owns the running server instance
 * - Keeps an in-memory ring buffer for new connections
 */
object TcpLogServerManager {
    const val DEFAULT_PORT = 17010

    private const val DEFAULT_RING_BUFFER_SIZE = 200

    private val lock = Any()
    private val bufferLock = Any()
    private val ringBuffer = ArrayDeque<String>(DEFAULT_RING_BUFFER_SIZE)

    @Volatile
    private var server: TcpLogServer? = null

    @Volatile
    private var lastError: String? = null

    fun applyFromStorage(): TcpLogServerState {
        val enabled = LogConfig.isTcpLogServerEnabled()
        val storedPort = LogConfig.getTcpLogServerPort()
        val normalizedPort = normalizePort(storedPort)
        if (storedPort != normalizedPort) {
            LogConfig.putTcpLogServerPort(normalizedPort)
        }
        return if (enabled) {
            start(normalizedPort)
        } else {
            stop()
        }
    }

    fun setEnabled(
        enabled: Boolean,
        port: Int = LogConfig.getTcpLogServerPort()
    ): TcpLogServerState {
        val normalizedPort = normalizePort(port)
        LogConfig.putTcpLogServerEnabled(enabled)
        LogConfig.putTcpLogServerPort(normalizedPort)
        return if (enabled) {
            start(normalizedPort)
        } else {
            stop()
        }
    }

    fun snapshot(): TcpLogServerState {
        val enabled = LogConfig.isTcpLogServerEnabled()
        val requestedPort = normalizePort(LogConfig.getTcpLogServerPort())
        val current = server
        val running = current?.isRunning() == true
        val boundPort = if (running) current?.boundPort() ?: -1 else -1
        val clientCount = if (running) current?.clientCount() ?: 0 else 0
        return TcpLogServerState(
            enabled = enabled,
            running = running,
            requestedPort = requestedPort,
            boundPort = boundPort,
            clientCount = clientCount,
            lastError = lastError,
        )
    }

    fun isRunning(): Boolean = server?.isRunning() == true

    fun tryEmit(line: String) {
        val current = server ?: return
        if (!current.isRunning()) return
        appendRingBuffer(line)
        current.tryOffer(line)
    }

    private fun start(port: Int): TcpLogServerState {
        synchronized(lock) {
            lastError = null
            val current = server
            if (current != null && current.isRunning()) {
                val currentPort = current.requestedPort()
                if (currentPort == port) {
                    return snapshot()
                }
                runCatching { current.stop() }
                server = null
            }

            val newServer =
                TcpLogServer(
                    port = port,
                    backlogProvider = { snapshotRingBuffer() },
                )
            runCatching { newServer.start() }.onSuccess {
                server = newServer
            }.onFailure { error ->
                lastError = error.message ?: error::class.java.simpleName
                LogConfig.putTcpLogServerEnabled(false)
                runCatching { newServer.stop() }
                server = null
            }
            return snapshot()
        }
    }

    private fun stop(): TcpLogServerState {
        synchronized(lock) {
            val current = server
            if (current != null) {
                runCatching { current.stop() }
                server = null
            }
            lastError = null
            return snapshot()
        }
    }

    private fun appendRingBuffer(line: String) {
        synchronized(bufferLock) {
            if (ringBuffer.size >= DEFAULT_RING_BUFFER_SIZE) {
                ringBuffer.removeFirst()
            }
            ringBuffer.addLast(line)
        }
    }

    private fun snapshotRingBuffer(): List<String> {
        synchronized(bufferLock) {
            return ringBuffer.toList()
        }
    }

    private fun normalizePort(port: Int): Int {
        if (port == 0) return 0
        if (port in 1..65535) return port
        return DEFAULT_PORT
    }
}
