package com.xyoye.common_component.log.http

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
class HttpLogServerSegmentAtTest {
    @Test
    fun segmentAtReturnsBadRequestWhenTimestampMissing() {
        val server = buildServer(segmentAtHandler = { null })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segment-at",
                    parameters = emptyMap(),
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, response.status)
        val body = response.data.bufferedReader().use { it.readText() }
        assertTrue(body.contains("timestampMs required"))
    }

    @Test
    fun segmentAtReturnsNotFoundWhenHandlerHasNoMatch() {
        val server = buildServer(segmentAtHandler = { null })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segment-at",
                    parameters = mapOf("timestampMs" to "1732142400000"),
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.NOT_FOUND, response.status)
        val body = response.data.bufferedReader().use { it.readText() }
        assertTrue(body.contains("segment not found"))
    }

    @Test
    fun segmentAtReturnsSegmentFileWhenMatchFound() {
        val payload =
            HttpLogDownloadPayload(
                inputStream = ByteArrayInputStream("""{"id":1}""".toByteArray(Charsets.UTF_8)),
                fileName = "seg_1732142400000.jsonl",
            )
        val server = buildServer(segmentAtHandler = { payload })
        val response =
            server.serve(
                fakeSession(
                    uri = "/api/v1/logs/segment-at",
                    parameters = mapOf("timestampMs" to "1732142400000"),
                ),
            )

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertEquals("application/x-ndjson; charset=utf-8", response.mimeType)
        assertEquals(
            "attachment; filename=\"seg_1732142400000.jsonl\"",
            response.getHeader("Content-Disposition"),
        )
        val content = response.data.bufferedReader().use { it.readText() }
        assertEquals("""{"id":1}""", content)
    }

    private fun buildServer(segmentAtHandler: (Long) -> HttpLogDownloadPayload?): HttpLogServer =
        HttpLogServer(
            port = 0,
            expectedTokenProvider = { TOKEN },
            rateLimiter = HttpRateLimiter(),
            segmentAtHandler = segmentAtHandler,
        )

    private fun fakeSession(
        uri: String,
        parameters: Map<String, String>,
        method: NanoHTTPD.Method = NanoHTTPD.Method.GET,
    ): NanoHTTPD.IHTTPSession =
        object : NanoHTTPD.IHTTPSession {
            private val headers = mutableMapOf("authorization" to "Bearer $TOKEN")
            private val parameterMap =
                parameters.mapValuesTo(mutableMapOf()) { (_, value) ->
                    mutableListOf(value)
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
    }
}
