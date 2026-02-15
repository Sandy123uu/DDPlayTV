package com.xyoye.common_component.log.http.sse

import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.LogRecord
import com.xyoye.common_component.log.http.model.LogSource
import com.xyoye.common_component.log.http.model.LogSourceFilter
import com.xyoye.common_component.log.model.LogLevel
import java.util.concurrent.CopyOnWriteArrayList

internal class SseHub(
    private val maxClients: Int = DEFAULT_MAX_CLIENTS,
    private val recentRecordsProvider: (Int) -> List<LogRecord>,
    private val onDropLowPriority: (() -> Unit)? = null,
) {
    data class SseFilter(
        val levels: Set<LogLevel>?,
        val tag: String?,
        val keyword: String?,
        val source: LogSourceFilter,
    )

    sealed class OpenResult {
        data class Success(val connection: SseClientConnection) : OpenResult()

        data class Rejected(
            val statusCode: Int,
            val message: String,
        ) : OpenResult()
    }

    private data class Client(
        val filter: SseFilter,
        val connection: SseClientConnection,
    )

    private val adapter = HttpLogJson.adapter(LogRecord::class.java)
    private val clients = CopyOnWriteArrayList<Client>()

    fun clientCount(): Int = clients.size

    fun open(
        filter: SseFilter,
        cursorId: Long?,
    ): OpenResult {
        if (clients.size >= maxClients) {
            return OpenResult.Rejected(statusCode = 429, message = "too many clients")
        }

        lateinit var holder: Client
        val connection =
            SseClientConnection(
                onClosed = { closed ->
                    while (true) {
                        val index = clients.indexOfFirst { it.connection == closed }
                        if (index < 0) break
                        clients.removeAt(index)
                    }
                },
            )
        holder = Client(filter = filter, connection = connection)
        clients.add(holder)

        if (cursorId != null) {
            sendBacklog(holder, cursorId)
        }
        connection.start()
        return OpenResult.Success(connection)
    }

    fun publish(record: LogRecord) {
        val payload = encodeEvent(record)
        clients.forEach { client ->
            if (!matches(record, client.filter)) return@forEach
            val offered =
                if (client.connection.offer(payload)) {
                    true
                } else if (record.level == LogLevel.WARN || record.level == LogLevel.ERROR) {
                    client.connection.offerDropOldest(payload)
                } else {
                    false
                }
            if (!offered) {
                onDropLowPriority?.invoke()
            }
        }
    }

    fun closeAll() {
        val snapshot = clients.toList()
        clients.clear()
        snapshot.forEach { it.connection.close() }
    }

    private fun sendBacklog(
        client: Client,
        cursorId: Long,
    ) {
        val recent = runCatching { recentRecordsProvider(DEFAULT_BACKLOG_LIMIT) }.getOrNull().orEmpty()
        if (recent.isEmpty()) return
        val backlog =
            recent
                .asSequence()
                .filter { it.id > cursorId }
                .filter { matches(it, client.filter) }
                .sortedBy { it.id }
                .toList()
        if (backlog.isEmpty()) return
        backlog.forEach { record ->
            client.connection.offerDropOldest(encodeEvent(record))
        }
    }

    private fun matches(
        record: LogRecord,
        filter: SseFilter,
    ): Boolean {
        if (filter.levels != null && record.level !in filter.levels) return false
        if (filter.tag != null) {
            val tag = record.tag?.trim().orEmpty()
            if (!tag.startsWith(filter.tag, ignoreCase = false)) return false
        }
        if (filter.keyword != null) {
            val keyword = filter.keyword.trim()
            if (keyword.isNotEmpty() && !record.message.contains(keyword, ignoreCase = true)) return false
        }
        return when (filter.source) {
            LogSourceFilter.BOTH -> true
            LogSourceFilter.APP -> record.source == LogSource.APP
            LogSourceFilter.LOGCAT -> record.source == LogSource.LOGCAT
        }
    }

    private fun encodeEvent(record: LogRecord): ByteArray {
        val json = adapter.toJson(record)
        val text =
            buildString {
                append("id: ").append(record.id).append('\n')
                append("event: log\n")
                append("data: ").append(json).append('\n')
                append('\n')
            }
        return text.toByteArray(Charsets.UTF_8)
    }

    private companion object {
        private const val DEFAULT_MAX_CLIENTS = 8
        private const val DEFAULT_BACKLOG_LIMIT = 200
    }
}
