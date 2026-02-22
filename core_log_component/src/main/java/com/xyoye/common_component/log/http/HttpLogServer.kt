package com.xyoye.common_component.log.http

import com.xyoye.common_component.log.http.auth.HttpAuthResult
import com.xyoye.common_component.log.http.auth.HttpRequestAuth
import com.xyoye.common_component.log.http.json.HttpLogJson
import com.xyoye.common_component.log.http.model.ErrorResponse
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
                "/api/v1/logs/download" -> handleDownload(session)
                else -> errorResponse(Response.Status.NOT_FOUND, 404, "not found")
            }
        } finally {
            rateLimiter.releaseRequest()
        }
    }

    private fun handleDownload(session: IHTTPSession): Response {
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
        private const val MIME_ZIP = "application/zip"
    }
}
