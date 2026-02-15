package com.xyoye.common_component.log.http.query

import com.xyoye.common_component.log.http.model.ErrorResponse
import com.xyoye.common_component.log.http.model.LogSourceFilter
import com.xyoye.common_component.log.model.LogLevel
import fi.iki.elonen.NanoHTTPD

data class HttpLogQuery(
    val startMs: Long?,
    val endMs: Long?,
    val levels: Set<LogLevel>?,
    val tag: String?,
    val keyword: String?,
    val source: LogSourceFilter,
    val limit: Int,
    val cursor: String?,
)

data class HttpLogStreamQuery(
    val levels: Set<LogLevel>?,
    val tag: String?,
    val keyword: String?,
    val source: LogSourceFilter,
    val cursor: String?,
)

sealed class HttpQueryParseResult<out T> {
    data class Success<T>(val query: T) : HttpQueryParseResult<T>()

    data class Error(val response: ErrorResponse) : HttpQueryParseResult<Nothing>()
}

object HttpLogQueryParser {
    private const val DEFAULT_LIMIT = 200
    private const val MAX_LIMIT = 500
    private const val MAX_TAG_LENGTH = 128
    private const val MAX_KEYWORD_LENGTH = 256
    private const val MAX_CURSOR_LENGTH = 256

    fun parseLogs(session: NanoHTTPD.IHTTPSession): HttpQueryParseResult<HttpLogQuery> {
        val rawParams = session.parms.orEmpty()
        val rawParameters = session.parameters.orEmpty()

        val startMs =
            runCatching { parseOptionalLong(rawParams["startMs"], minValueInclusive = 0) }.getOrElse {
                return error("startMs must be >= 0")
            }
        val endMs =
            runCatching { parseOptionalLong(rawParams["endMs"], minValueInclusive = 0) }.getOrElse {
                return error("endMs must be >= 0")
            }
        if (startMs != null && endMs != null && startMs > endMs) {
            return error("startMs must be <= endMs")
        }

        val levels = parseLevels(rawParameters["levels"]) ?: return error("levels contains invalid value")

        val tag =
            rawParams["tag"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_TAG_LENGTH) return error("tag too long")
                value
            }
        val keyword =
            rawParams["keyword"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_KEYWORD_LENGTH) return error("keyword too long")
                value
            }

        val source = parseSource(rawParams["source"]) ?: return error("source contains invalid value")
        val limit = parseLimit(rawParams["limit"]) ?: return error("limit must be in 1..$MAX_LIMIT")
        val cursor =
            rawParams["cursor"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_CURSOR_LENGTH) return error("cursor too long")
                if (value.toLongOrNull() == null) return error("cursor contains invalid value")
                value
            }

        return HttpQueryParseResult.Success(
            HttpLogQuery(
                startMs = startMs,
                endMs = endMs,
                levels = levels,
                tag = tag,
                keyword = keyword,
                source = source,
                limit = limit,
                cursor = cursor,
            ),
        )
    }

    fun parseStream(session: NanoHTTPD.IHTTPSession): HttpQueryParseResult<HttpLogStreamQuery> {
        val rawParams = session.parms.orEmpty()
        val rawParameters = session.parameters.orEmpty()

        val levels = parseLevels(rawParameters["levels"]) ?: return error("levels contains invalid value")
        val tag =
            rawParams["tag"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_TAG_LENGTH) return error("tag too long")
                value
            }
        val keyword =
            rawParams["keyword"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_KEYWORD_LENGTH) return error("keyword too long")
                value
            }
        val source = parseSource(rawParams["source"]) ?: return error("source contains invalid value")
        val cursor =
            rawParams["cursor"]?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                if (value.length > MAX_CURSOR_LENGTH) return error("cursor too long")
                if (value.toLongOrNull() == null) return error("cursor contains invalid value")
                value
            }

        return HttpQueryParseResult.Success(
            HttpLogStreamQuery(
                levels = levels,
                tag = tag,
                keyword = keyword,
                source = source,
                cursor = cursor,
            ),
        )
    }

    private fun parseLevels(rawValues: List<String>?): Set<LogLevel>? {
        val values = rawValues.orEmpty().mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        if (values.isEmpty()) return null
        val parsed = LinkedHashSet<LogLevel>(values.size)
        values.forEach { value ->
            val level = runCatching { LogLevel.valueOf(value) }.getOrNull() ?: return null
            parsed.add(level)
        }
        return parsed
    }

    private fun parseSource(raw: String?): LogSourceFilter? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return LogSourceFilter.BOTH
        return runCatching { LogSourceFilter.valueOf(value) }.getOrNull()
    }

    private fun parseLimit(raw: String?): Int? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return DEFAULT_LIMIT
        val parsed = value.toIntOrNull() ?: return null
        if (parsed !in 1..MAX_LIMIT) return null
        return parsed
    }

    private fun parseOptionalLong(
        raw: String?,
        minValueInclusive: Long,
    ): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val parsed = value.toLongOrNull() ?: throw IllegalArgumentException("invalid long")
        if (parsed < minValueInclusive) throw IllegalArgumentException("out of range")
        return parsed
    }

    private fun error(message: String): HttpQueryParseResult.Error =
        HttpQueryParseResult.Error(
            ErrorResponse(
                errorCode = 400,
                errorMessage = message,
            ),
        )
}
