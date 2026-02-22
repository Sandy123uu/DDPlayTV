package com.xyoye.common_component.log.http

import com.xyoye.common_component.log.http.auth.HttpAuthResult
import com.xyoye.common_component.log.http.auth.HttpRequestAuth
import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.ErrorResponse
import com.xyoye.common_component.log.http.model.SuccessResponse
import com.xyoye.common_component.log.http.rate.HttpRateLimiter
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

internal data class HttpLogDownloadPayload(
    val inputStream: InputStream,
    val fileName: String,
)

internal class HttpLogServer(
    port: Int,
    private val expectedTokenProvider: () -> String,
    private val rateLimiter: HttpRateLimiter,
    private val pageHandler: (() -> Response)? = null,
    private val downloadHandler: (() -> HttpLogDownloadPayload)? = null,
    private val latestSegmentHandler: (() -> HttpLogDownloadPayload?)? = null,
    private val segmentAtHandler: ((Long) -> HttpLogDownloadPayload?)? = null,
    private val clearLogsHandler: (() -> HttpLogServerState)? = null,
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

            return when (session.uri) {
                "/" -> handlePage(session)
                "/api/v1/logs/download" -> handleDownload(session)
                "/api/v1/logs/latest-segment" -> handleLatestSegment(session)
                "/api/v1/logs/segment-at" -> handleSegmentAt(session)
                "/api/v1/logs/clear" -> handleClearLogs(session)
                else -> errorResponse(Response.Status.NOT_FOUND, 404, "not found")
            }
        } finally {
            rateLimiter.releaseRequest()
        }
    }

    private fun handlePage(session: IHTTPSession): Response {
        val methodError = requireMethod(session, Method.GET)
        if (methodError != null) {
            return methodError
        }
        return pageHandler?.invoke() ?: errorResponse(Response.Status.NOT_FOUND, 404, "not found")
    }

    private fun handleDownload(session: IHTTPSession): Response {
        val methodError = requireMethod(session, Method.GET)
        if (methodError != null) {
            return methodError
        }
        if (!rateLimiter.allowLogsRequest(session.remoteIpAddress.orEmpty())) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "rate limited")
        }
        val payload =
            runCatching {
                downloadHandler?.invoke()
            }.getOrNull() ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "download unavailable")

        val response =
            newChunkedResponse(
                Response.Status.OK,
                MIME_ZIP,
                payload.inputStream,
            )
        response.addHeader("Cache-Control", "no-store")
        response.addHeader("Content-Disposition", "attachment; filename=\"${payload.fileName}\"")
        return response
    }

    private fun handleSegmentAt(session: IHTTPSession): Response {
        val methodError = requireMethod(session, Method.GET)
        if (methodError != null) {
            return methodError
        }
        if (!rateLimiter.allowLogsRequest(session.remoteIpAddress.orEmpty())) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "rate limited")
        }
        val rawTimestamp = session.parameters[QUERY_TIMESTAMP_MS]?.firstOrNull()?.trim().orEmpty()
        if (rawTimestamp.isEmpty()) {
            return errorResponse(Response.Status.BAD_REQUEST, 400, "timestampMs required")
        }
        val timestampMs =
            rawTimestamp.toLongOrNull()?.takeIf { it >= 0L }
                ?: return errorResponse(Response.Status.BAD_REQUEST, 400, "timestampMs invalid")
        val handler = segmentAtHandler ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "segment-at unavailable")
        val payload =
            runCatching {
                handler.invoke(timestampMs)
            }.getOrElse {
                return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "segment-at unavailable")
            } ?: return errorResponse(Response.Status.NOT_FOUND, 404, "segment not found")

        val response =
            newChunkedResponse(
                Response.Status.OK,
                MIME_NDJSON,
                payload.inputStream,
            )
        response.addHeader("Cache-Control", "no-store")
        response.addHeader("Content-Disposition", "attachment; filename=\"${payload.fileName}\"")
        return response
    }

    private fun handleLatestSegment(session: IHTTPSession): Response {
        val methodError = requireMethod(session, Method.GET)
        if (methodError != null) {
            return methodError
        }
        if (!rateLimiter.allowLogsRequest(session.remoteIpAddress.orEmpty())) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "rate limited")
        }
        val handler = latestSegmentHandler ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "latest segment unavailable")
        val payload =
            runCatching {
                handler.invoke()
            }.getOrElse {
                return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "latest segment unavailable")
            } ?: return errorResponse(Response.Status.NOT_FOUND, 404, "latest segment not found")

        val response =
            newChunkedResponse(
                Response.Status.OK,
                MIME_NDJSON,
                payload.inputStream,
            )
        response.addHeader("Cache-Control", "no-store")
        response.addHeader("Content-Disposition", "attachment; filename=\"${payload.fileName}\"")
        return response
    }

    private fun handleClearLogs(session: IHTTPSession): Response {
        val methodError = requireMethod(session, Method.POST)
        if (methodError != null) {
            return methodError
        }
        if (!rateLimiter.allowLogsRequest(session.remoteIpAddress.orEmpty())) {
            return errorResponse(Response.Status.TOO_MANY_REQUESTS, 429, "rate limited")
        }
        val state =
            runCatching {
                clearLogsHandler?.invoke()
            }.getOrNull() ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, 503, "clear unavailable")
        return jsonResponse(
            Response.Status.OK,
            SuccessResponse(message = state.message ?: "logs cleared"),
        )
    }

    private fun requireMethod(
        session: IHTTPSession,
        expected: Method,
    ): Response? {
        if (session.method == expected) {
            return null
        }
        val response = errorResponse(Response.Status.METHOD_NOT_ALLOWED, 405, "method not allowed")
        response.addHeader("Allow", expected.name)
        return response
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
        private const val QUERY_TIMESTAMP_MS = "timestampMs"
        private const val MIME_JSON = "application/json; charset=utf-8"
        private const val MIME_ZIP = "application/zip"
        private const val MIME_NDJSON = "application/x-ndjson; charset=utf-8"
    }
}
