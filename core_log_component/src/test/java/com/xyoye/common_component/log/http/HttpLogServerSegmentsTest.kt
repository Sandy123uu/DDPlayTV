package com.xyoye.common_component.log.http

import com.xyoye.common_component.log.http.model.LogSegmentSummary
import com.xyoye.common_component.log.http.model.LogSegmentsResponse
import com.xyoye.common_component.log.http.rate.HttpRateLimiter
import fi.iki.elonen.NanoHTTPD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.InputStream

@RunWith(JUnit4::class)
class HttpLogServerSegmentsTest {
    @Test
    fun segmentsReturnsJsonWhenHandlerExists() {
        val responsePayload =
            LogSegmentsResponse(
                segments =
                    listOf(
                        LogSegmentSummary(
                            fileName = "seg_2000.jsonl",
                            startMs = 2_000L,
                            endMs = null,
                            sizeBytes = 256L,
                            lastModifiedMs = 2_500L,
                            isLatest = true,
                        ),
                    ),
                totalCount = 1,
                totalBytes = 256L,
                latestStartMs = 2_000L,
                oldestStartMs = 2_000L,
            )
        val server = buildServer(segmentsHandler = { responsePayload })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segments",
                    parameters = emptyMap(),
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertEquals("application/json; charset=utf-8", response.mimeType)
        val body = response.data.bufferedReader().use { it.readText() }
        assertTrue(body.contains("\"totalCount\":1"))
        assertTrue(body.contains("\"fileName\":\"seg_2000.jsonl\""))
    }

    @Test
    fun segmentsReturnsMethodNotAllowedWhenMethodIsNotGet() {
        val server = buildServer(segmentsHandler = { EMPTY_RESPONSE })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segments",
                    parameters = emptyMap(),
                    method = NanoHTTPD.Method.POST,
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, response.status)
        assertEquals("GET", response.getHeader("Allow"))
    }

    @Test
    fun segmentsReturnsUnauthorizedWhenTokenMissing() {
        val server = buildServer(segmentsHandler = { EMPTY_RESPONSE })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segments",
                    parameters = emptyMap(),
                    withAuth = false,
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED, response.status)
        val body = response.data.bufferedReader().use { it.readText() }
        assertTrue(body.contains("unauthorized"))
    }

    private fun buildServer(segmentsHandler: () -> LogSegmentsResponse): HttpLogServer =
        HttpLogServer(
            port = 0,
            expectedTokenProvider = { TOKEN },
            rateLimiter = HttpRateLimiter(),
            segmentsHandler = segmentsHandler,
        )

    private fun fakeSession(
        uri: String,
        parameters: Map<String, String>,
        method: NanoHTTPD.Method = NanoHTTPD.Method.GET,
        withAuth: Boolean = true,
    ): NanoHTTPD.IHTTPSession =
        object : NanoHTTPD.IHTTPSession {
            private val headers = mutableMapOf<String, String>()
            private val parameterMap =
                parameters.mapValuesTo(mutableMapOf()) { (_, value) ->
                    mutableListOf(value)
                }

            init {
                if (withAuth) {
                    headers["authorization"] = "Bearer $TOKEN"
                }
            }

            override fun execute() = Unit

            override fun getCookies(): NanoHTTPD.CookieHandler = throw UnsupportedOperationException("not used")

            override fun getHeaders(): MutableMap<String, String> = headers

            override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

            override fun getMethod(): NanoHTTPD.Method = method

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun getParms(): MutableMap<String, String> =
                parameterMap.mapValuesTo(mutableMapOf()) { (_, values) ->
                    values.firstOrNull().orEmpty()
                }

            override fun getParameters(): MutableMap<String, MutableList<String>> = parameterMap

            override fun getQueryParameterString(): String =
                parameterMap.entries.joinToString("&") { (key, values) ->
                    "$key=${values.firstOrNull().orEmpty()}"
                }

            override fun getUri(): String = uri

            override fun parseBody(files: MutableMap<String, String>) = Unit

            override fun getRemoteIpAddress(): String = "192.168.1.2"

            override fun getRemoteHostName(): String = "192.168.1.2"
        }

    private companion object {
        private const val TOKEN = "0123456789abcdef"
        private val EMPTY_RESPONSE =
            LogSegmentsResponse(
                segments = emptyList(),
                totalCount = 0,
                totalBytes = 0L,
                latestStartMs = null,
                oldestStartMs = null,
            )
    }
}
