package com.xyoye.player.utils

import com.xyoye.common_component.config.PlayerConfig
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.network.helper.OkHttpTlsPolicy
import com.xyoye.common_component.network.helper.ProxyOkHttpClientFactory
import com.xyoye.common_component.network.helper.UnsafeTlsApi
import com.xyoye.common_component.utils.getFileName
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.net.ssl.SSLException
import kotlin.random.Random

/**
 * Created by xyoye on 2021/5/2.
 */

class VlcProxyServer private constructor() : NanoHTTPD(randomPort()) {
    private lateinit var url: String
    private var headers: Map<String, String> = emptyMap()

    private val removeHeaderKeys = setOf("host", "remote-addr", "http-client-ip")
    private val persistentHeaderKeys = setOf("referer", "cookie", "authorization", "user-agent")
    private val logTag = "VlcProxyServer"

    private object Holder {
        val instance = VlcProxyServer()
    }

    companion object {
        // 随机端口
        private fun randomPort() = Random.nextInt(30000, 40000)

        @JvmStatic
        fun getInstance() = Holder.instance
    }

    private val strictClient: OkHttpClient by lazy {
        ProxyOkHttpClientFactory.create(OkHttpTlsPolicy.Strict)
    }

    @OptIn(UnsafeTlsApi::class)
    private val unsafeTrustAllClient: OkHttpClient by lazy {
        ProxyOkHttpClientFactory.create(OkHttpTlsPolicy.UnsafeTrustAll)
    }

    @Volatile
    private var hasLoggedUnsafeFallback: Boolean = false

    override fun serve(session: IHTTPSession?): Response {
        session ?: return super.serve(session)

        val proxyResponse = getProxyResponse(session)
        val response =
            newFixedLengthResponse(
                Status.lookup(proxyResponse.code) ?: Status.OK,
                proxyResponse.header("Content-Type"),
                proxyResponse.body?.byteStream(),
                proxyResponse.body?.contentLength() ?: 0,
            )
        val headers = proxyResponse.headers

        for (index in 0 until headers.size) {
            val key = headers.name(index)
            val value = headers.value(index)
            response.addHeader(key, value)
        }

        return response
    }

    fun getInputStreamUrl(
        url: String,
        headers: Map<String, String>
    ): String {
        this.url = url
        this.headers = headers.toMap()
        val encodeFileName = URLEncoder.encode(getFileName(url), "utf-8")
        return "http://127.0.0.1:$listeningPort/$encodeFileName"
    }

    private fun getProxyResponse(session: IHTTPSession): okhttp3.Response {
        val requestBuilder = Request.Builder()
        var ifMatchValue: String? = null
        var hasIfRangeHeader = false
        val sessionPersistentHeaders = mutableMapOf<String, String>()

        session.headers.forEach { (key, value) ->
            when {
                key.equals("if-match", true) -> {
                    ifMatchValue = value
                    requestBuilder.header(key, value)
                }
                key.equals("if-range", true) -> {
                    hasIfRangeHeader = true
                    requestBuilder.header(key, value)
                }
                shouldRemoveHeader(key) -> return@forEach
                shouldPersistHeader(key) -> {
                    sessionPersistentHeaders[key] = value
                }
                else -> requestBuilder.header(key, value)
            }
        }

        headers.forEach { (key, value) ->
            when {
                key.equals("if-match", true) -> {
                    ifMatchValue = value
                    requestBuilder.header(key, value)
                }
                key.equals("if-range", true) -> {
                    hasIfRangeHeader = true
                    requestBuilder.header(key, value)
                }
                shouldRemoveHeader(key) -> return@forEach
                else -> requestBuilder.header(key, value)
            }
        }

        if (!hasIfRangeHeader && !ifMatchValue.isNullOrBlank()) {
            requestBuilder.header("If-Range", ifMatchValue!!)
        }

        // 优先使用外部传入的持久化请求头，再用会话内的补全缺失
        persistentHeaderKeys.forEach { key ->
            headers[key]?.let { requestBuilder.header(key, it) }
            sessionPersistentHeaders[key]?.let { requestBuilder.header(key, it) }
        }

        val request = requestBuilder.url(url).build()

        return try {
            strictClient.newCall(request).execute()
        } catch (e: Exception) {
            if (!isUserAllowedUnsafeTlsFallback(e)) {
                throw e
            }

            logUnsafeTlsFallbackOnce(request, e)
            unsafeTrustAllClient.newCall(request).execute()
        }
    }

    private fun isUserAllowedUnsafeTlsFallback(error: Throwable): Boolean {
        if (!PlayerConfig.isVlcProxyAllowInsecureTls()) {
            return false
        }

        var cursor: Throwable? = error
        var depth = 0
        while (cursor != null && depth < 8) {
            if (cursor is SSLException) {
                return true
            }
            cursor = cursor.cause
            depth++
        }
        return false
    }

    private fun logUnsafeTlsFallbackOnce(
        request: Request,
        error: Throwable
    ) {
        if (hasLoggedUnsafeFallback) return
        hasLoggedUnsafeFallback = true

        val urlHash =
            request.url
                .toString()
                .hashCode()
                .toString()
        LogFacade.w(
            LogModule.PLAYER,
            logTag,
            "TLS verification failed, fallback to UNSAFE_TRUST_ALL (user opted-in)",
            context =
                mapOf(
                    "urlHash" to urlHash,
                    "error" to (error.javaClass.simpleName ?: "unknown"),
                ),
        )
    }

    private fun shouldRemoveHeader(headerKey: String): Boolean = removeHeaderKeys.any { headerKey.equals(it, true) }

    private fun shouldPersistHeader(headerKey: String): Boolean = persistentHeaderKeys.any { headerKey.equals(it, true) }
}
