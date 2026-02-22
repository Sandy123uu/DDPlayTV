package com.xyoye.common_component.log.http

import android.content.Context
import android.util.Log
import com.xyoye.common_component.config.LogConfig
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.http.logcat.LogcatCollector
import com.xyoye.common_component.log.http.model.HttpDegradeMode
import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.LogSource
import com.xyoye.common_component.log.http.model.RetentionTier
import com.xyoye.common_component.log.http.page.HttpLogPageHandler
import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.common_component.log.store.LogStoreState
import com.xyoye.common_component.log.store.SegmentedJsonlLogStore
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.SocketException
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal interface HttpLogServerConfig {
    fun isHttpLogServerEnabled(): Boolean

    fun putHttpLogServerEnabled(enabled: Boolean)

    fun getHttpLogServerPort(): Int

    fun putHttpLogServerPort(port: Int)

    fun getHttpLogServerToken(): String?

    fun putHttpLogServerToken(token: String)

    fun getHttpLogRetentionDays(): Int

    fun putHttpLogRetentionDays(days: Int)
}

private object MmkvHttpLogServerConfig : HttpLogServerConfig {
    override fun isHttpLogServerEnabled(): Boolean = LogConfig.isHttpLogServerEnabled()

    override fun putHttpLogServerEnabled(enabled: Boolean) {
        LogConfig.putHttpLogServerEnabled(enabled)
    }

    override fun getHttpLogServerPort(): Int = LogConfig.getHttpLogServerPort()

    override fun putHttpLogServerPort(port: Int) {
        LogConfig.putHttpLogServerPort(port)
    }

    override fun getHttpLogServerToken(): String? = LogConfig.getHttpLogServerToken()

    override fun putHttpLogServerToken(token: String) {
        LogConfig.putHttpLogServerToken(token)
    }

    override fun getHttpLogRetentionDays(): Int = LogConfig.getHttpLogRetentionDays()

    override fun putHttpLogRetentionDays(days: Int) {
        LogConfig.putHttpLogRetentionDays(days)
    }
}

data class HttpLogServerState(
    val enabled: Boolean,
    val running: Boolean,
    val requestedPort: Int,
    val boundPort: Int,
    val clientCount: Int,
    val ipAddresses: List<String>,
    val token: String,
    val retention: RetentionTier,
    val storeUsedBytes: Long,
    val logcatAvailable: Boolean,
    val degradeMode: HttpDegradeMode,
    val message: String?,
    val lastError: String?,
)

internal object HttpLogServerManager {
    const val DEFAULT_PORT = 17010

    private const val PIPE_CAPACITY = 1024
    private const val TOKEN_LENGTH_BYTES = 16
    private const val MAX_THROWABLE_TEXT_LENGTH = 2000

    internal var config: HttpLogServerConfig = MmkvHttpLogServerConfig

    private val lock = Any()
    private val tokenRef = AtomicReference("")
    private val lastErrorRef = AtomicReference<String?>(null)
    private val messageRef = AtomicReference<String?>(null)
    private val dropLowPriorityRef = AtomicBoolean(false)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var server: HttpLogServer? = null

    @Volatile
    private var store: SegmentedJsonlLogStore? = null

    @Volatile
    private var logcatCollector: LogcatCollector? = null

    @Volatile
    private var rateLimiter: com.xyoye.common_component.log.http.rate.HttpRateLimiter? = null

    private val pipeQueue = ArrayBlockingQueue<LogRecord>(PIPE_CAPACITY)

    @Volatile
    private var pipeExecutor: ExecutorService? = null

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return
            appContext = context.applicationContext
            ensureStoreLocked()
            ensurePipeLocked()
        }
    }

    fun applyFromStorage(): HttpLogServerState {
        synchronized(lock) {
            ensureTokenLocked()
            ensureStoreLocked()
            ensurePipeLocked()

            val port = normalizePort(config.getHttpLogServerPort())
            config.putHttpLogServerPort(port)
            val days = normalizeRetentionDays(config.getHttpLogRetentionDays())
            config.putHttpLogRetentionDays(days)

            val enabled = config.isHttpLogServerEnabled()
            return if (enabled) {
                startLocked(port)
            } else {
                stopLocked(clearError = true)
            }
        }
    }

    fun setEnabled(
        enabled: Boolean,
        port: Int = config.getHttpLogServerPort(),
    ): HttpLogServerState {
        synchronized(lock) {
            val normalizedPort = normalizePort(port)
            config.putHttpLogServerEnabled(enabled)
            config.putHttpLogServerPort(normalizedPort)
            ensureTokenLocked()
            ensureStoreLocked()
            ensurePipeLocked()

            return if (enabled) {
                startLocked(normalizedPort)
            } else {
                stopLocked(clearError = true)
            }
        }
    }

    fun resetToken(): HttpLogServerState {
        synchronized(lock) {
            val newToken = generateToken()
            config.putHttpLogServerToken(newToken)
            tokenRef.set(newToken)
            lastErrorRef.set(null)
            messageRef.set(null)
            val state = snapshotLocked()
            if (state.running) {
                restartLocked(state.requestedPort)
            }
            return snapshotLocked()
        }
    }

    fun clearLogs(): HttpLogServerState {
        synchronized(lock) {
            store?.clear()
            messageRef.set("logs cleared")
            return snapshotLocked()
        }
    }

    fun setRetentionDays(days: Int): HttpLogServerState {
        synchronized(lock) {
            val normalizedRequested = normalizeRetentionDays(days)
            val context = appContext
            val currentStore = store
            if (context == null || currentStore == null) {
                config.putHttpLogRetentionDays(normalizedRequested)
                messageRef.set("retention updated")
                return snapshotLocked()
            }

            val logsDir = File(context.filesDir, "http_logs")
            val usedBytes = currentStore.snapshot().usedBytes
            val availableForLogs = logsDir.usableSpace.coerceAtLeast(0L) + usedBytes.coerceAtLeast(0L)

            val requestedTier = RetentionTier.forDays(normalizedRequested)
            val resolvedDays =
                when {
                    availableForLogs >= requestedTier.maxBytes -> normalizedRequested
                    normalizedRequested == 30 && availableForLogs >= RetentionTier.Tier14Days.maxBytes -> 14
                    normalizedRequested >= 14 && availableForLogs >= RetentionTier.Tier7Days.maxBytes -> 7
                    else -> 7
                }

            config.putHttpLogRetentionDays(resolvedDays)
            currentStore.enforceRetentionNow()

            if (availableForLogs < RetentionTier.Tier7Days.maxBytes) {
                currentStore.pausePersistence("storage insufficient")
                messageRef.set("persistence paused: storage insufficient")
                return snapshotLocked()
            }

            currentStore.tryResumePersistence()
            messageRef.set(if (resolvedDays != normalizedRequested) "retention fallback: $resolvedDays" else "retention updated")
            return snapshotLocked()
        }
    }

    fun snapshot(): HttpLogServerState {
        synchronized(lock) {
            ensureTokenLocked()
            ensureStoreLocked()
            return snapshotLocked()
        }
    }

    fun expectedToken(): String = tokenRef.get().orEmpty()

    fun ingestAppEvent(event: LogEvent) {
        if (!config.isHttpLogServerEnabled()) return
        val record = buildAppRecord(event)
        offerRecord(record)
    }

    fun ingestLogcatLine(line: String) {
        if (!config.isHttpLogServerEnabled()) return
        val record = buildLogcatRecord(line)
        offerRecord(record)
    }

    fun openDownloadPayload(): HttpLogDownloadPayload {
        synchronized(lock) {
            val context = appContext ?: error("context not ready")
            return HttpLogArchiveExporter.open(logsDir = File(context.filesDir, "http_logs"))
        }
    }

    private fun buildAppRecord(event: LogEvent): LogRecord {
        val sanitizedMessage = SensitiveDataSanitizer.sanitizeFreeText(event.message)
        val sanitizedContext = SensitiveDataSanitizer.sanitizeContext(event.context)
        val throwableText =
            event.throwable?.let { throwable ->
                val stack = Log.getStackTraceString(throwable)
                SensitiveDataSanitizer.sanitizeFreeText(stack).take(MAX_THROWABLE_TEXT_LENGTH)
            }
        return LogRecord(
            id = event.sequenceId,
            timestampMs = event.timestamp,
            level = event.level,
            source = LogSource.APP,
            message = sanitizedMessage,
            module = event.module.code,
            tag = event.tag?.value,
            context = if (sanitizedContext.isEmpty()) null else sanitizedContext,
            thread = event.threadName,
            throwable = throwableText,
        )
    }

    private fun buildLogcatRecord(rawLine: String): LogRecord {
        val parsed = LogcatLineParser.parse(rawLine)
        val id = parsed.id
        val message = SensitiveDataSanitizer.sanitizeFreeText(parsed.message)
        val tag = parsed.tag?.takeIf { it.isNotBlank() }
        val context = parsed.context.takeIf { it.isNotEmpty() }
        return LogRecord(
            id = id,
            timestampMs = parsed.timestampMs,
            level = parsed.level,
            source = LogSource.LOGCAT,
            message = message,
            module = null,
            tag = tag,
            context = context,
            thread = null,
            throwable = null,
        )
    }

    private fun offerRecord(record: LogRecord) {
        val accepted =
            if (pipeQueue.offer(record)) {
                true
            } else if (record.level == LogLevel.WARN || record.level == LogLevel.ERROR) {
                pipeQueue.poll()
                pipeQueue.offer(record)
            } else {
                false
            }
        if (!accepted) {
            dropLowPriorityRef.set(true)
            messageRef.set("dropped logs due to backpressure")
        }
    }

    private fun ensurePipeLocked() {
        if (pipeExecutor != null) return
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "HttpLogPipe").apply { isDaemon = true }
            }
        executor.execute { pipeLoop() }
        pipeExecutor = executor
    }

    private fun pipeLoop() {
        while (true) {
            try {
                val record = pipeQueue.take()
                val currentStore = store
                if (config.isHttpLogServerEnabled()) {
                    if (currentStore != null) {
                        currentStore.append(record)
                    }
                }
            } catch (_: InterruptedException) {
                // ignore
            } catch (e: Exception) {
                lastErrorRef.set(e.message ?: e::class.java.simpleName)
            }
        }
    }

    private fun ensureStoreLocked() {
        if (store != null) return
        val context = appContext ?: return
        val logsDir = File(context.filesDir, "http_logs")
        store =
            SegmentedJsonlLogStore(
                logsDir = logsDir,
                retentionTierProvider = { RetentionTier.forDays(config.getHttpLogRetentionDays()) },
            )
    }

    private fun ensureTokenLocked() {
        val stored = config.getHttpLogServerToken()?.trim().orEmpty()
        if (stored.isNotBlank() && stored.length >= 16) {
            tokenRef.set(stored)
            return
        }
        val token = generateToken()
        config.putHttpLogServerToken(token)
        tokenRef.set(token)
    }

    private fun startLocked(port: Int): HttpLogServerState {
        lastErrorRef.set(null)
        messageRef.set(null)

        val context = appContext
        if (context == null) {
            lastErrorRef.set("context not ready")
            config.putHttpLogServerEnabled(false)
            return snapshotLocked()
        }

        val current = server
        if (current != null && current.wasStarted()) {
            val bound = current.listeningPort
            if (port == current.getListeningPort() || port == bound) {
                ensureLogcatCollectorLocked(start = true)
                return snapshotLocked()
            }
            runCatching { current.stop() }
            server = null
        }

        val limiter = com.xyoye.common_component.log.http.rate.HttpRateLimiter()
        rateLimiter = limiter
        val pageHandler = HttpLogPageHandler(context)
        val newServer =
            HttpLogServer(
                port = port,
                expectedTokenProvider = { expectedToken() },
                rateLimiter = limiter,
                pageHandler = { pageHandler.handle() },
                downloadHandler = { openDownloadPayload() },
            )

        runCatching { newServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
            .onSuccess {
                server = newServer
                ensureLogcatCollectorLocked(start = true)
            }.onFailure { error ->
                lastErrorRef.set(error.message ?: error::class.java.simpleName)
                config.putHttpLogServerEnabled(false)
                runCatching { newServer.stop() }
                server = null
                rateLimiter = null
                ensureLogcatCollectorLocked(start = false)
            }
        return snapshotLocked()
    }

    private fun restartLocked(port: Int) {
        stopLocked(clearError = false)
        startLocked(port)
    }

    private fun stopLocked(clearError: Boolean): HttpLogServerState {
        val current = server
        if (current != null) {
            runCatching { current.stop() }
            server = null
        }
        rateLimiter = null
        ensureLogcatCollectorLocked(start = false)
        if (clearError) {
            lastErrorRef.set(null)
            messageRef.set(null)
            dropLowPriorityRef.set(false)
        }
        return snapshotLocked()
    }

    private fun ensureLogcatCollectorLocked(start: Boolean) {
        val existing = logcatCollector
        if (!start) {
            existing?.stop()
            logcatCollector = null
            return
        }
        if (existing != null && existing.isAvailable()) return
        val collector =
            LogcatCollector(
                onLine = { line ->
                    ingestLogcatLine(line)
                },
            )
        collector.start()
        logcatCollector = collector
    }

    private fun snapshotLocked(): HttpLogServerState {
        val enabled = config.isHttpLogServerEnabled()
        val requestedPort = normalizePort(config.getHttpLogServerPort())
        val currentServer = server
        val running = currentServer?.isAlive() == true
        val boundPort = if (running) currentServer?.listeningPort ?: -1 else -1
        val ipAddresses = resolveLocalIpAddresses()

        val retention = RetentionTier.forDays(config.getHttpLogRetentionDays())
        val storeSnapshot = store?.snapshot() ?: LogStoreState(retention, 0L, false, null)

        val logcatAvailable = logcatCollector?.isAvailable() == true
        val degradeMode = resolveDegradeMode(storeSnapshot, logcatAvailable)
        val message =
            listOfNotNull(
                messageRef.get(),
                storeSnapshot.lastError,
                lastErrorRef.get(),
            ).firstOrNull()

        return HttpLogServerState(
            enabled = enabled,
            running = running,
            requestedPort = requestedPort,
            boundPort = boundPort,
            clientCount = rateLimiter?.activeRequestsCount() ?: 0,
            ipAddresses = ipAddresses,
            token = tokenRef.get().orEmpty(),
            retention = retention,
            storeUsedBytes = storeSnapshot.usedBytes,
            logcatAvailable = logcatAvailable,
            degradeMode = degradeMode,
            message = message,
            lastError = lastErrorRef.get(),
        )
    }

    private fun resolveDegradeMode(
        storeState: LogStoreState,
        logcatAvailable: Boolean,
    ): HttpDegradeMode =
        when {
            storeState.persistencePaused -> HttpDegradeMode.PERSISTENCE_PAUSED
            !logcatAvailable && config.isHttpLogServerEnabled() -> HttpDegradeMode.LOGCAT_PAUSED
            dropLowPriorityRef.get() -> HttpDegradeMode.DROP_LOW_PRIORITY
            else -> HttpDegradeMode.NORMAL
        }

    private fun resolveLocalIpAddresses(): List<String> {
        val ipv4 = mutableListOf<String>()
        val ipv6 = mutableListOf<String>()
        try {
            val element = NetworkInterface.getNetworkInterfaces()
            while (element.hasMoreElements()) {
                val networkInterface = element.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address.isLoopbackAddress || address.isLinkLocalAddress) {
                        continue
                    }
                    val ip = address.hostAddress?.trim().orEmpty()
                    if (ip.isEmpty()) continue
                    when (address) {
                        is Inet4Address -> ipv4.add(ip)
                        is Inet6Address -> ipv6.add(ip)
                    }
                }
            }
        } catch (e: SocketException) {
            LogFacade.w(LogModule.CORE, "HttpLogServer", "resolveLocalIpAddresses failed: ${e.message}")
        }
        return (ipv4 + ipv6).distinct()
    }

    private fun normalizePort(port: Int): Int {
        if (port == 0) return 0
        if (port in 1..65535) return port
        return DEFAULT_PORT
    }

    private fun normalizeRetentionDays(days: Int): Int =
        when (days) {
            14 -> 14
            30 -> 30
            else -> 7
        }

    private fun generateToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(TOKEN_LENGTH_BYTES)
        random.nextBytes(bytes)
        return bytes.joinToString(separator = "") { b -> "%02x".format(Locale.US, b) }
    }

    internal fun resetForTests() {
        synchronized(lock) {
            val current = server
            if (current != null) {
                runCatching { current.stop() }
                server = null
            }
            rateLimiter = null
            ensureLogcatCollectorLocked(start = false)
            store?.close()
            store = null
            pipeExecutor?.shutdownNow()
            pipeExecutor = null
            pipeQueue.clear()
            appContext = null
            config = MmkvHttpLogServerConfig
            tokenRef.set("")
            lastErrorRef.set(null)
            messageRef.set(null)
            dropLowPriorityRef.set(false)
        }
    }

    private object LogcatLineParser {
        private val whitespaceRegex = "\\s+".toRegex()

        data class ParsedLine(
            val id: Long,
            val timestampMs: Long,
            val level: LogLevel,
            val tag: String?,
            val message: String,
            val context: Map<String, String>,
        )

        fun parse(line: String): ParsedLine {
            val nowMs = System.currentTimeMillis()
            val id = com.xyoye.common_component.log.LogSystem.nextSequenceId()

            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return ParsedLine(
                    id = id,
                    timestampMs = nowMs,
                    level = LogLevel.INFO,
                    tag = null,
                    message = "",
                    context = emptyMap(),
                )
            }

            val parts = trimmed.split(whitespaceRegex, limit = 5)
            if (parts.size < 5) {
                return ParsedLine(
                    id = id,
                    timestampMs = nowMs,
                    level = LogLevel.INFO,
                    tag = null,
                    message = trimmed,
                    context = emptyMap(),
                )
            }

            val timestampMs = parts[0].toDoubleOrNull()?.let { (it * 1000).toLong() } ?: nowMs
            val pid = parts[1]
            val tid = parts[2]
            val level = mapLevel(parts[3])
            val tagAndMessage = parts[4]
            val tag = tagAndMessage.substringBefore(':').trim().trimEnd(':')
            val message =
                tagAndMessage.substringAfter(':', missingDelimiterValue = tagAndMessage).trimStart()
            val context =
                mapOf(
                    "pid" to pid,
                    "tid" to tid,
                )
            return ParsedLine(
                id = id,
                timestampMs = timestampMs,
                level = level,
                tag = tag.ifBlank { null },
                message = message.ifBlank { trimmed },
                context = context,
            )
        }

        private fun mapLevel(raw: String): LogLevel =
            when (raw.trim().uppercase(Locale.US).firstOrNull()) {
                'V', 'D' -> LogLevel.DEBUG
                'I' -> LogLevel.INFO
                'W' -> LogLevel.WARN
                'E', 'F', 'A' -> LogLevel.ERROR
                else -> LogLevel.INFO
            }
    }
}
