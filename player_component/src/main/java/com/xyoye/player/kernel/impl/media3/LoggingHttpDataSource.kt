package com.xyoye.player.kernel.impl.media3

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import java.util.concurrent.atomic.AtomicLong

/**
 * 为 HTTP 请求打印 URL / 响应码 / Content-Type，便于定位播放失败（如直链过期）。
 */
@UnstableApi
class LoggingHttpDataSourceFactory : HttpDataSource.Factory {
    private val delegate = DefaultHttpDataSource.Factory()

    fun setUserAgent(userAgent: String): LoggingHttpDataSourceFactory {
        delegate.setUserAgent(userAgent)
        return this
    }

    fun setAllowCrossProtocolRedirects(allow: Boolean): LoggingHttpDataSourceFactory {
        delegate.setAllowCrossProtocolRedirects(allow)
        return this
    }

    override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): LoggingHttpDataSourceFactory {
        delegate.setDefaultRequestProperties(defaultRequestProperties)
        return this
    }

    override fun createDataSource(): HttpDataSource {
        val upstream = delegate.createDataSource()
        return LoggingHttpDataSource(upstream)
    }
}

@UnstableApi
private class LoggingHttpDataSource(
    private val upstream: HttpDataSource
) : HttpDataSource by upstream {
    companion object {
        private const val SUCCESS_OPEN_LOG_SAMPLE_INTERVAL = 20L
        private val successOpenCounter = AtomicLong(0L)
    }

    override fun open(dataSpec: DataSpec): Long =
        try {
            val length = upstream.open(dataSpec)
            val responseCode = runCatching { upstream.responseCode }.getOrNull()
            logOpen(
                uri = dataSpec.uri,
                responseCode = responseCode,
                headers = runCatching { upstream.responseHeaders }.getOrNull(),
                logSuccess = shouldLogSuccessfulOpen(responseCode),
            )
            length
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            logOpen(
                uri = dataSpec.uri,
                responseCode = e.responseCode,
                headers = e.headerFields,
                logSuccess = true,
            )
            throw e
        } catch (e: Exception) {
            Media3Diagnostics.logHttpOpen(dataSpec.uri.toString(), null, null, failed = true)
            throw e
        }

    private fun logOpen(
        uri: Uri?,
        responseCode: Int?,
        headers: Map<String, List<String>>?,
        logSuccess: Boolean
    ) {
        val resolvedHeaders = headers.orEmpty()
        val contentType =
            resolvedHeaders.entries
                .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                ?.value
                ?.firstOrNull()

        Media3Diagnostics.logHttpOpen(
            uri?.toString(),
            responseCode,
            contentType,
            logSuccess = logSuccess,
        )
    }

    private fun shouldLogSuccessfulOpen(responseCode: Int?): Boolean {
        if (responseCode != null && responseCode >= 400) {
            return true
        }
        val count = successOpenCounter.incrementAndGet()
        return count == 1L || count % SUCCESS_OPEN_LOG_SAMPLE_INTERVAL == 0L
    }
}
