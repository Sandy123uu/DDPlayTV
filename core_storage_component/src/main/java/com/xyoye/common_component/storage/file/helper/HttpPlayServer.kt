package com.xyoye.common_component.storage.file.helper

import com.xyoye.common_component.config.PlayerConfig
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.network.helper.AgentInterceptor
import com.xyoye.common_component.network.helper.OkHttpTlsConfigurer
import com.xyoye.common_component.network.helper.OkHttpTlsPolicy
import com.xyoye.common_component.network.helper.RedirectAuthorizationInterceptor
import com.xyoye.common_component.network.helper.UnsafeTlsApi
import com.xyoye.common_component.network.service.ExtendedService
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.RangeUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * A lightweight local HTTP proxy for players (e.g. libmpv) that struggle with some remote servers.
 * It forwards requests to an upstream URL and provides stable Range responses.
 */
class HttpPlayServer private constructor() : NanoHTTPD(randomPort()) {
    private val logTag = "HttpPlayServer"

    private val startMutex = Mutex()
    private val rangeRetryLock = Any()

    private var upstreamUrl: String? = null
    private var upstreamHeaders: Map<String, String> = emptyMap()
    private var contentType: String = DEFAULT_BINARY_CONTENT_TYPE
    private var contentLength: Long = -1L
    private var upstreamTlsPolicy: UpstreamTlsPolicy = UpstreamTlsPolicy.STRICT

    @Volatile
    private var prePlayRangeMinIntervalMs: Long = 1000L

    @Volatile
    private var rangeRetryDone: Boolean = false
    private var rangeRetrySupplier: (() -> UpstreamSource?)? = null

    @Volatile
    private var seekEnabled: Boolean = false

    private val upstreamRangeLock = Any()

    @Volatile
    private var lastUpstreamRangeAtMs: Long = 0L

    private val maxRangeBytesBeforePlay: Long = 1L * 1024 * 1024
    private val maxRangeBytesAfterPlay: Long = 4L * 1024 * 1024
    private val rangeLogIntervalMs: Long = 1000L

    @Volatile
    private var lastRangeLogAtMs: Long = 0L

    @Volatile
    private var loggedNoRangeRequest: Boolean = false

    companion object {
        private const val DEFAULT_BINARY_CONTENT_TYPE = "application/octet-stream"
        private const val TEXT_PLAIN_CONTENT_TYPE = "text/plain"

        private fun randomPort() = Random.nextInt(20000, 30000)

        @JvmStatic
        fun getInstance(): HttpPlayServer = Holder.instance
    }

    private object Holder {
        val instance: HttpPlayServer by lazy { HttpPlayServer() }
    }

    enum class UpstreamTlsPolicy {
        /**
         * 严格 TLS：不忽略证书校验、不忽略主机名校验（推荐；WebDAV 默认）。
         */
        STRICT,

        /**
         * 不安全 TLS：信任所有证书 + 忽略主机名校验（必须由用户显式开启）。
         */
        UNSAFE_TRUST_ALL
    }

    data class UpstreamSource(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val contentType: String = DEFAULT_BINARY_CONTENT_TYPE,
        val contentLength: Long = -1L,
        val tlsPolicy: UpstreamTlsPolicy = UpstreamTlsPolicy.STRICT
    )

    private val strictClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AgentInterceptor())
            .addNetworkInterceptor(RedirectAuthorizationInterceptor())
            .build()
    }

    @OptIn(UnsafeTlsApi::class)
    private val unsafeTrustAllClient: OkHttpClient by lazy {
        OkHttpTlsConfigurer
            .apply(strictClient.newBuilder(), OkHttpTlsPolicy.UnsafeTrustAll)
            .build()
    }

    private val strictService: ExtendedService by lazy {
        RetrofitManager.createService(Api.PLACEHOLDER, strictClient, ExtendedService::class.java)
    }

    private val unsafeTrustAllService: ExtendedService by lazy {
        RetrofitManager.createService(Api.PLACEHOLDER, unsafeTrustAllClient, ExtendedService::class.java)
    }

    private fun selectExtendedService(tlsPolicy: UpstreamTlsPolicy): ExtendedService =
        when (tlsPolicy) {
            UpstreamTlsPolicy.STRICT -> strictService
            UpstreamTlsPolicy.UNSAFE_TRUST_ALL -> unsafeTrustAllService
        }

    override fun serve(session: IHTTPSession): Response = serveInternal(session, allowRetry = true)

    private data class ServeRequestContext(
        val url: String,
        val rangeHeader: String?,
        val hasRange: Boolean,
        val supportsRange: Boolean,
        val requestedRange: Pair<Long, Long>?,
        val invalidRange: Boolean,
        val isRangeRequest: Boolean,
        val cappedRange: Pair<Long, Long>?,
        val upstreamRangeHeader: String?,
        val urlHash: String,
    )

    private data class NanoResponseBuildResult(
        val nanoResponse: Response,
        val status: Response.Status,
        val responseLength: Long,
        val contentRange: String?,
    )

    private fun serveInternal(
        session: IHTTPSession,
        allowRetry: Boolean
    ): Response {
        val url =
            upstreamUrl
                ?: return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    TEXT_PLAIN_CONTENT_TYPE,
                    "upstream not configured",
                )

        val requestContext = buildServeRequestContext(session, url)
        if (requestContext.invalidRange) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, TEXT_PLAIN_CONTENT_TYPE, "")
        }

        logNoRangeRequestIfNeeded(requestContext, session.headers)

        val response =
            fetchUpstreamResponse(requestContext, session.headers)
                ?: return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    TEXT_PLAIN_CONTENT_TYPE,
                    "upstream request failed",
                )

        if (!response.isSuccessful) {
            return buildUpstreamFailureResponse(requestContext, response)
        }

        val body =
            response.body()
                ?: return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    TEXT_PLAIN_CONTENT_TYPE,
                    "empty upstream body",
                )

        val upstreamContentRangeHeader = response.headers()["Content-Range"]
        val unsupportedRangeResponse =
            handleUnsupportedRange(
                session = session,
                allowRetry = allowRetry,
                requestContext = requestContext,
                response = response,
                body = body,
                upstreamContentRangeHeader = upstreamContentRangeHeader,
            )
        if (unsupportedRangeResponse != null) {
            return unsupportedRangeResponse
        }

        val upstreamContentRange = upstreamContentRangeHeader?.let { parseContentRange(it) }
        val isPartial =
            requestContext.isRangeRequest ||
                response.code() == 206 ||
                upstreamContentRangeHeader != null
        val responseRange = resolveResponseRange(requestContext.cappedRange, upstreamContentRange)
        val nanoResult = buildNanoResponse(body, requestContext.supportsRange, isPartial, responseRange)

        applyRangeHeaders(
            nanoResponse = nanoResult.nanoResponse,
            supportsRange = requestContext.supportsRange,
            isPartial = isPartial,
            contentRange = nanoResult.contentRange,
        )

        logRangeResponse(
            requestContext = requestContext,
            response = response,
            body = body,
            upstreamContentRangeHeader = upstreamContentRangeHeader,
            responseRange = responseRange,
            status = nanoResult.status,
            responseLength = nanoResult.responseLength,
        )

        return nanoResult.nanoResponse
    }

    private fun buildServeRequestContext(
        session: IHTTPSession,
        url: String
    ): ServeRequestContext {
        val rangeHeader = session.headers["range"]
        val hasRange = !rangeHeader.isNullOrBlank()
        val supportsRange = contentLength > 0
        val requestedRange =
            if (supportsRange && hasRange) {
                RangeUtils.parseRange(rangeHeader!!, contentLength)
            } else {
                null
            }
        val invalidRange = supportsRange && hasRange && requestedRange == null
        val isRangeRequest = supportsRange && hasRange && requestedRange != null
        val cappedRange =
            requestedRange?.let { range ->
                val maxBytes = if (seekEnabled) maxRangeBytesAfterPlay else maxRangeBytesBeforePlay
                val start = range.first
                val end = minOf(range.second, start + maxBytes - 1)
                start to end
            }

        return ServeRequestContext(
            url = url,
            rangeHeader = rangeHeader,
            hasRange = hasRange,
            supportsRange = supportsRange,
            requestedRange = requestedRange,
            invalidRange = invalidRange,
            isRangeRequest = isRangeRequest,
            cappedRange = cappedRange,
            upstreamRangeHeader = cappedRange?.let { (start, end) -> "bytes=$start-$end" },
            urlHash = url.hashCode().toString(),
        )
    }

    private fun logNoRangeRequestIfNeeded(
        requestContext: ServeRequestContext,
        headers: Map<String, String>
    ) {
        if (!requestContext.supportsRange || requestContext.hasRange || loggedNoRangeRequest) {
            return
        }
        loggedNoRangeRequest = true
        LogFacade.w(
            LogModule.STORAGE,
            logTag,
            "proxy request without range header",
            context =
                mapOf(
                    "urlHash" to requestContext.urlHash,
                    "contentLength" to contentLength.toString(),
                    "seekEnabled" to seekEnabled.toString(),
                    "clientUa" to (headers["user-agent"] ?: "null"),
                ),
        )
    }

    private fun fetchUpstreamResponse(
        requestContext: ServeRequestContext,
        headers: Map<String, String>
    ): retrofit2.Response<okhttp3.ResponseBody>? {
        val shouldForwardRange = requestContext.supportsRange && requestContext.hasRange
        if (shouldForwardRange) {
            return synchronized(upstreamRangeLock) {
                throttleUpstreamRange()
                fetchUpstream(
                    requestContext.url,
                    buildUpstreamHeaders(
                        clientHeaders = headers,
                        rangeHeader = requestContext.upstreamRangeHeader,
                        forwardRange = true,
                    ),
                )
            }
        }

        return fetchUpstream(
            requestContext.url,
            buildUpstreamHeaders(
                clientHeaders = headers,
                rangeHeader = null,
                forwardRange = false,
            ),
        )
    }

    private fun buildUpstreamFailureResponse(
        requestContext: ServeRequestContext,
        response: retrofit2.Response<okhttp3.ResponseBody>
    ): Response {
        // If upstream rejects probing ranges (403), let mpv fall back to linear playback instead of failing open().
        if (requestContext.supportsRange && requestContext.hasRange && !seekEnabled && response.code() == 403) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, TEXT_PLAIN_CONTENT_TYPE, "")
        }
        return newFixedLengthResponse(
            toStatus(response.code()),
            TEXT_PLAIN_CONTENT_TYPE,
            "upstream http ${response.code()}",
        )
    }

    private fun handleUnsupportedRange(
        session: IHTTPSession,
        allowRetry: Boolean,
        requestContext: ServeRequestContext,
        response: retrofit2.Response<okhttp3.ResponseBody>,
        body: okhttp3.ResponseBody,
        upstreamContentRangeHeader: String?
    ): Response? {
        if (!requestContext.isRangeRequest || (response.code() == 206 && upstreamContentRangeHeader != null)) {
            return null
        }

        if (allowRetry && !rangeRetryDone) {
            synchronized(rangeRetryLock) {
                if (!rangeRetryDone) {
                    val refreshed = runCatching { rangeRetrySupplier?.invoke() }.getOrNull()
                    if (refreshed != null) {
                        rangeRetryDone = true
                        upstreamUrl = refreshed.url
                        upstreamHeaders = refreshed.headers
                        contentType = refreshed.contentType
                        contentLength = refreshed.contentLength
                        upstreamTlsPolicy = refreshed.tlsPolicy
                        return serveInternal(session, allowRetry = false)
                    }
                }
            }
        }

        val upstreamBodyLength = body.contentLength()
        LogFacade.w(
            LogModule.STORAGE,
            logTag,
            "upstream range unsupported",
            context =
                mapOf(
                    "urlHash" to requestContext.urlHash,
                    "rangeHeader" to (requestContext.rangeHeader ?: "null"),
                    "requestedRange" to formatRange(requestContext.requestedRange),
                    "upstreamRange" to (requestContext.upstreamRangeHeader ?: "null"),
                    "upstreamCode" to response.code().toString(),
                    "upstreamContentRange" to (upstreamContentRangeHeader ?: "null"),
                    "upstreamBodyLength" to upstreamBodyLength.toString(),
                    "contentLength" to contentLength.toString(),
                    "seekEnabled" to seekEnabled.toString(),
                ),
        )
        return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR,
            TEXT_PLAIN_CONTENT_TYPE,
            "upstream range unsupported",
        )
    }

    private fun resolveResponseRange(
        cappedRange: Pair<Long, Long>?,
        upstreamContentRange: Pair<Long, Long>?
    ): Pair<Long, Long>? =
        when {
            cappedRange != null && upstreamContentRange != null -> {
                val start = maxOf(cappedRange.first, upstreamContentRange.first)
                val end = minOf(cappedRange.second, upstreamContentRange.second)
                if (start <= end) start to end else upstreamContentRange
            }
            upstreamContentRange != null -> upstreamContentRange
            else -> cappedRange
        }

    private fun buildNanoResponse(
        body: okhttp3.ResponseBody,
        supportsRange: Boolean,
        isPartial: Boolean,
        responseRange: Pair<Long, Long>?
    ): NanoResponseBuildResult {
        val status = if (isPartial) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        val contentRange =
            if (isPartial && responseRange != null) {
                "bytes ${responseRange.first}-${responseRange.second}/$contentLength"
            } else {
                null
            }

        val responseLength =
            when {
                supportsRange && isPartial && responseRange != null ->
                    (responseRange.second - responseRange.first + 1).coerceAtLeast(0)
                supportsRange && !isPartial -> contentLength
                else -> body.contentLength()
            }.takeIf { it > 0 } ?: -1L

        val bodyStream =
            if (isPartial && responseLength > 0) {
                limitStream(body.byteStream(), responseLength)
            } else {
                body.byteStream()
            }

        val nanoResponse =
            if (responseLength > 0) {
                newFixedLengthResponse(status, contentType, bodyStream, responseLength)
            } else {
                newChunkedResponse(status, contentType, bodyStream)
            }

        return NanoResponseBuildResult(
            nanoResponse = nanoResponse,
            status = status,
            responseLength = responseLength,
            contentRange = contentRange,
        )
    }

    private fun applyRangeHeaders(
        nanoResponse: Response,
        supportsRange: Boolean,
        isPartial: Boolean,
        contentRange: String?
    ) {
        if (!supportsRange) {
            return
        }
        nanoResponse.addHeader("Accept-Ranges", "bytes")
        if (isPartial) {
            contentRange?.let { nanoResponse.addHeader("Content-Range", it) }
        }
    }

    private fun logRangeResponse(
        requestContext: ServeRequestContext,
        response: retrofit2.Response<okhttp3.ResponseBody>,
        body: okhttp3.ResponseBody,
        upstreamContentRangeHeader: String?,
        responseRange: Pair<Long, Long>?,
        status: Response.Status,
        responseLength: Long
    ) {
        if (!requestContext.isRangeRequest) {
            return
        }

        val upstreamBodyLength = body.contentLength()
        val suspiciousUpstream = response.code() != 206 && upstreamContentRangeHeader == null
        val sampleLimitBytes = 256L
        val shouldSampleUpstreamBody =
            response.code() == 200 &&
                upstreamContentRangeHeader == null &&
                upstreamBodyLength in 0..sampleLimitBytes
        val upstreamBodySample =
            if (shouldSampleUpstreamBody) {
                peekUpstreamBodySample(response, sampleLimitBytes)
            } else {
                null
            }

        if (suspiciousUpstream || shouldLogRange(nowMs())) {
            val logFn = if (suspiciousUpstream) LogFacade::w else LogFacade::d
            val context =
                mutableMapOf(
                    "urlHash" to requestContext.urlHash,
                    "rangeHeader" to (requestContext.rangeHeader ?: "null"),
                    "requestedRange" to formatRange(requestContext.requestedRange),
                    "cappedRange" to formatRange(requestContext.cappedRange),
                    "upstreamRange" to (requestContext.upstreamRangeHeader ?: "null"),
                    "upstreamCode" to response.code().toString(),
                    "upstreamContentRange" to (upstreamContentRangeHeader ?: "null"),
                    "upstreamBodyLength" to upstreamBodyLength.toString(),
                    "responseRange" to formatRange(responseRange),
                    "responseLength" to responseLength.toString(),
                    "status" to status.toString(),
                    "contentLength" to contentLength.toString(),
                    "seekEnabled" to seekEnabled.toString(),
                )
            upstreamBodySample?.let { context["upstreamBodySample"] = it }
            logFn.invoke(
                LogModule.STORAGE,
                logTag,
                "proxy range response",
                context,
                null,
            )
        }
    }

    fun setSeekEnabled(enabled: Boolean) {
        seekEnabled = enabled
    }

    fun isServingUrl(url: String): Boolean = url.startsWith("http://127.0.0.1:$listeningPort/")

    private fun toStatus(code: Int): Response.Status =
        when (code) {
            200 -> Response.Status.OK
            206 -> Response.Status.PARTIAL_CONTENT
            400 -> Response.Status.BAD_REQUEST
            401 -> Response.Status.UNAUTHORIZED
            403 -> Response.Status.FORBIDDEN
            404 -> Response.Status.NOT_FOUND
            416 -> Response.Status.RANGE_NOT_SATISFIABLE
            500 -> Response.Status.INTERNAL_ERROR
            503 -> Response.Status.SERVICE_UNAVAILABLE
            else -> Response.Status.INTERNAL_ERROR
        }

    private fun parseContentRange(contentRange: String): Pair<Long, Long>? {
        val trimmed = contentRange.trim()
        if (!trimmed.startsWith("bytes", ignoreCase = true)) return null
        val afterUnit = trimmed.substring(5).trimStart()
        val rangePart = afterUnit.removePrefix("=").trimStart()
        val slashIndex = rangePart.indexOf('/')
        val rangeValue = if (slashIndex >= 0) rangePart.substring(0, slashIndex).trim() else rangePart
        if (rangeValue == "*" || rangeValue.isEmpty()) return null
        val dashIndex = rangeValue.indexOf('-')
        if (dashIndex <= 0 || dashIndex == rangeValue.length - 1) return null
        val start = rangeValue.substring(0, dashIndex).toLongOrNull() ?: return null
        val end = rangeValue.substring(dashIndex + 1).toLongOrNull() ?: return null
        if (start < 0 || end < start) return null
        return start to end
    }

    private fun formatRange(range: Pair<Long, Long>?): String = range?.let { "${it.first}-${it.second}" } ?: "null"

    private fun peekUpstreamBodySample(
        response: retrofit2.Response<okhttp3.ResponseBody>,
        maxBytes: Long
    ): String? =
        runCatching {
            val peeked = response.raw().peekBody(maxBytes)
            sanitizeBodySample(peeked.string())
        }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun sanitizeBodySample(sample: String): String {
        if (sample.isBlank()) return sample
        val builder = StringBuilder(sample.length)
        for (ch in sample) {
            when (ch) {
                '\r', '\n', '\t' -> builder.append(' ')
                else -> builder.append(if (ch.code < 0x20) '?' else ch)
            }
        }
        return builder.toString().trim()
    }

    private fun shouldLogRange(nowMs: Long): Boolean {
        val elapsed = nowMs - lastRangeLogAtMs
        if (elapsed < rangeLogIntervalMs) return false
        lastRangeLogAtMs = nowMs
        return true
    }

    private fun limitStream(
        stream: InputStream,
        maxBytes: Long
    ): InputStream {
        if (maxBytes <= 0) return stream
        return object : InputStream() {
            private var remaining = maxBytes
            private var closed = false

            override fun read(): Int {
                if (remaining <= 0) return -1
                val value = stream.read()
                if (value == -1) return -1
                remaining -= 1
                return value
            }

            override fun read(
                buffer: ByteArray,
                offset: Int,
                length: Int
            ): Int {
                if (remaining <= 0) return -1
                val allowed = minOf(remaining.toInt(), length)
                val count = stream.read(buffer, offset, allowed)
                if (count == -1) return -1
                remaining -= count.toLong()
                return count
            }

            override fun available(): Int {
                val available = stream.available()
                return if (remaining < available) remaining.toInt() else available
            }

            override fun close() {
                if (closed) return
                closed = true
                stream.close()
            }
        }
    }

    private fun buildUpstreamHeaders(
        clientHeaders: Map<String, String>,
        rangeHeader: String?,
        forwardRange: Boolean
    ): Map<String, String> {
        val merged = LinkedHashMap<String, String>()
        merged.putAll(upstreamHeaders)
        // Forward a small allowlist of client headers; some upstreams require them (e.g. for Range/auth).
        val existingKeys = merged.keys.map { it.lowercase() }.toSet()

        fun putIfAbsent(
            header: String,
            value: String?
        ) {
            if (value.isNullOrBlank()) return
            if (existingKeys.contains(header.lowercase())) return
            merged[header] = value
        }
        putIfAbsent("User-Agent", clientHeaders["user-agent"])
        putIfAbsent("Referer", clientHeaders["referer"])
        putIfAbsent("Cookie", clientHeaders["cookie"])
        putIfAbsent("Authorization", clientHeaders["authorization"])
        // Ensure byte offsets match the original stream (avoid transparent gzip).
        merged["Accept-Encoding"] = "identity"
        if (forwardRange && !rangeHeader.isNullOrBlank()) {
            merged["Range"] = rangeHeader
        }
        return merged
    }

    private fun fetchUpstream(
        url: String,
        headers: Map<String, String>
    ): retrofit2.Response<okhttp3.ResponseBody>? =
        runCatching {
            selectExtendedService(upstreamTlsPolicy).getResourceResponseCall(url, headers).execute()
        }.getOrElse { throwable ->
            ErrorReportHelper.postCatchedException(
                throwable,
                "HttpPlayServer",
                "上游请求失败 url=$url headers=${headers.keys.joinToString()}",
            )
            null
        }

    private fun throttleUpstreamRange() {
        val now = nowMs()
        val minIntervalMs =
            if (seekEnabled) {
                20L
            } else {
                prePlayRangeMinIntervalMs.coerceIn(0, 2000)
            }
        val elapsed = now - lastUpstreamRangeAtMs
        val waitMs = minIntervalMs - elapsed
        if (waitMs > 0) {
            runCatching { Thread.sleep(waitMs) }
        }
        lastUpstreamRangeAtMs = nowMs()
    }

    private fun nowMs(): Long = System.nanoTime() / 1_000_000L

    suspend fun startSync(timeoutMs: Long = 5000): Boolean {
        return startMutex.withLock {
            if (wasStarted()) {
                return@withLock true
            }
            return@withLock try {
                start()
                val started = awaitCondition(timeoutMs = timeoutMs) { wasStarted() }
                if (!started) {
                    stop()
                }
                started
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedException(
                    e,
                    "HttpPlayServer",
                    "启动播放服务器失败: timeout=${timeoutMs}ms",
                )
                false
            }
        }
    }

    fun generatePlayUrl(
        upstreamUrl: String,
        upstreamHeaders: Map<String, String> = emptyMap(),
        contentType: String = DEFAULT_BINARY_CONTENT_TYPE,
        contentLength: Long = -1L,
        prePlayRangeMinIntervalMs: Long = runCatching { PlayerConfig.getMpvProxyRangeMinIntervalMs() }.getOrDefault(1000).toLong(),
        fileName: String = "video",
        upstreamTlsPolicy: UpstreamTlsPolicy = UpstreamTlsPolicy.STRICT,
        onRangeUnsupported: (() -> UpstreamSource?)? = null
    ): String {
        this.upstreamUrl = upstreamUrl
        this.upstreamHeaders = upstreamHeaders
        this.contentType = contentType
        this.contentLength = contentLength
        this.prePlayRangeMinIntervalMs = prePlayRangeMinIntervalMs
        this.upstreamTlsPolicy = upstreamTlsPolicy
        this.rangeRetryDone = false
        this.rangeRetrySupplier = onRangeUnsupported
        this.seekEnabled = false
        this.lastUpstreamRangeAtMs = 0L
        val encodedFileName = URLEncoder.encode(fileName, "utf-8")
        return "http://127.0.0.1:$listeningPort/$encodedFileName"
    }
}
