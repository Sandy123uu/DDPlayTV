package com.xyoye.common_component.log.http

import com.xyoye.common_component.log.http.auth.HttpAuthResult
import com.xyoye.common_component.log.http.auth.HttpRequestAuth
import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.ErrorResponse
import com.xyoye.common_component.log.http.model.LogListResponse
import com.xyoye.common_component.log.http.model.StatusResponse
import com.xyoye.common_component.log.http.query.HttpLogQuery
import com.xyoye.common_component.log.http.query.HttpLogQueryParser
import com.xyoye.common_component.log.http.query.HttpLogStreamQuery
import com.xyoye.common_component.log.http.query.HttpQueryParseResult
import com.xyoye.common_component.log.http.rate.HttpRateLimiter
import com.xyoye.common_component.log.http.sse.SseHub
import com.xyoye.common_component.log.store.LogQueryResult
import fi.iki.elonen.NanoHTTPD

internal class HttpLogServer(
    port: Int,
    private val expectedTokenProvider: () -> String,
    private val rateLimiter: HttpRateLimiter,
    private val statusProvider: () -> StatusResponse,
    private val logsQueryHandler: (HttpLogQuery) -> LogQueryResult,
    private val pageHandler: (() -> Response)? = null,
    private val streamOpenHandler: ((HttpLogStreamQuery) -> SseHub.OpenResult)? = null,
) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        if (!rateLimiter.tryAcquireRequest()) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "too many requests")
        }
        try {
            val auth = HttpRequestAuth.authorize(session, expectedTokenProvider())
            if (auth is HttpAuthResult.Unauthorized) {
                return jsonResponse(Response.Status.UNAUTHORIZED, auth.response)
            }

            if (session.method != Method.GET) {
                return errorResponse(Response.Status.METHOD_NOT_ALLOWED, 405, "method not allowed")
            }

            return when (session.uri) {
                "/" -> pageHandler?.invoke() ?: errorResponse(Response.Status.NOT_FOUND, 404, "not found")
                "/api/v1/status" -> jsonResponse(Response.Status.OK, statusProvider())
                "/api/v1/logs" -> handleLogs(session)
                "/api/v1/stream" -> handleStream(session)
                else -> errorResponse(Response.Status.NOT_FOUND, 404, "not found")
            }
        } finally {
            rateLimiter.releaseRequest()
        }
    }

    private fun handleLogs(session: IHTTPSession): Response {
        if (!rateLimiter.allowLogsRequest(session.remoteIpAddress.orEmpty())) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "rate limited")
        }
        val parsed = HttpLogQueryParser.parseLogs(session)
        val query =
            when (parsed) {
                is HttpQueryParseResult.Error -> return jsonResponse(Response.Status.BAD_REQUEST, parsed.response)
                is HttpQueryParseResult.Success -> parsed.query
            }
        val result = logsQueryHandler(query)
        return jsonResponse(
            Response.Status.OK,
            LogListResponse(
                items = result.items,
                nextCursor = result.nextCursor,
                hasMore = result.hasMore,
            ),
        )
    }

    private fun handleStream(session: IHTTPSession): Response {
        val opener = streamOpenHandler ?: return errorResponse(Response.Status.NOT_FOUND, 404, "not found")
        val parsed = HttpLogQueryParser.parseStream(session)
        val query =
            when (parsed) {
                is HttpQueryParseResult.Error -> return jsonResponse(Response.Status.BAD_REQUEST, parsed.response)
                is HttpQueryParseResult.Success -> parsed.query
            }
        return when (val open = opener(query)) {
            is SseHub.OpenResult.Rejected -> {
                when (open.statusCode) {
                    429 -> errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, open.message)
                    else -> errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, open.message)
                }
            }
            is SseHub.OpenResult.Success -> {
                val response =
                    newChunkedResponse(
                        Response.Status.OK,
                        MIME_EVENT_STREAM,
                        open.connection.inputStream(),
                    )
                response.addHeader("Cache-Control", "no-cache")
                response.addHeader("Connection", "keep-alive")
                response.addHeader("X-Accel-Buffering", "no")
                response
            }
        }
    }

    private fun jsonResponse(
        status: Response.Status,
        body: Any,
    ): Response {
        val json = HttpLogJson.adapter(body.javaClass).toJson(body)
        val response = newFixedLengthResponse(status, MIME_JSON, json)
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun errorResponse(
        status: Response.Status,
        code: Int,
        message: String,
    ): Response =
        jsonResponse(
            status,
            ErrorResponse(
                errorCode = code,
                errorMessage = message,
            ),
        )

    private companion object {
        private const val MIME_JSON = "application/json; charset=utf-8"
        private const val MIME_EVENT_STREAM = "text/event-stream; charset=utf-8"
    }
}
