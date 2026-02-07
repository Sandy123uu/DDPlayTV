package com.xyoye.common_component.storage.file.helper

import com.xyoye.data_component.enums.LocalProxyMode
import com.xyoye.data_component.enums.PlayerType

object LocalProxy {
    enum class UpstreamTlsPolicy {
        STRICT,
        UNSAFE_TRUST_ALL,
    }

    data class UpstreamSource(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val contentType: String = "application/octet-stream",
        val contentLength: Long = -1L,
        val tlsPolicy: UpstreamTlsPolicy = UpstreamTlsPolicy.STRICT,
    )

    suspend fun wrapIfNeeded(
        playerType: PlayerType,
        modeValue: Int?,
        upstreamUrl: String,
        upstreamHeaders: Map<String, String>? = null,
        contentLength: Long,
        prePlayRangeMinIntervalMs: Long,
        fileName: String,
        autoEnabled: Boolean,
        upstreamTlsPolicy: UpstreamTlsPolicy = UpstreamTlsPolicy.STRICT,
        onRangeUnsupported: (() -> UpstreamSource?)? = null
    ): String {
        if (upstreamUrl.isBlank()) return upstreamUrl

        val mode = LocalProxyMode.from(modeValue)
        if (mode == LocalProxyMode.OFF) return upstreamUrl
        if (mode == LocalProxyMode.AUTO && !autoEnabled) return upstreamUrl

        val isHttp =
            upstreamUrl.startsWith("http://", ignoreCase = true) ||
                upstreamUrl.startsWith("https://", ignoreCase = true)
        if (!isHttp) return upstreamUrl

        val resolvedLength = contentLength.coerceAtLeast(-1L)
        if (resolvedLength <= 0) return upstreamUrl

        val playServer = HttpPlayServer.getInstance()
        if (playServer.isServingUrl(upstreamUrl)) return upstreamUrl

        val started = playServer.startSync()
        if (!started) return upstreamUrl

        return playServer.generatePlayUrl(
            upstreamUrl = upstreamUrl,
            upstreamHeaders = upstreamHeaders.orEmpty(),
            contentLength = resolvedLength,
            prePlayRangeMinIntervalMs = prePlayRangeMinIntervalMs,
            fileName = fileName.ifBlank { playerType.name.lowercase() },
            upstreamTlsPolicy = upstreamTlsPolicy.toServerPolicy(),
            onRangeUnsupported =
                onRangeUnsupported?.let { supplier ->
                    {
                        supplier.invoke()?.toServerSource()
                    }
                },
        )
    }

    fun isServingUrl(url: String?): Boolean {
        val normalized = url?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            HttpPlayServer.getInstance().isServingUrl(normalized)
        }.getOrDefault(false)
    }

    fun setSeekEnabledIfServing(
        url: String?,
        enabled: Boolean
    ): Boolean {
        if (!isServingUrl(url)) {
            return false
        }
        return runCatching {
            HttpPlayServer.getInstance().setSeekEnabled(enabled)
            true
        }.getOrDefault(false)
    }

    private fun UpstreamTlsPolicy.toServerPolicy(): HttpPlayServer.UpstreamTlsPolicy =
        when (this) {
            UpstreamTlsPolicy.STRICT -> HttpPlayServer.UpstreamTlsPolicy.STRICT
            UpstreamTlsPolicy.UNSAFE_TRUST_ALL -> HttpPlayServer.UpstreamTlsPolicy.UNSAFE_TRUST_ALL
        }

    private fun UpstreamSource.toServerSource(): HttpPlayServer.UpstreamSource =
        HttpPlayServer.UpstreamSource(
            url = url,
            headers = headers,
            contentType = contentType,
            contentLength = contentLength,
            tlsPolicy = tlsPolicy.toServerPolicy(),
        )
}
