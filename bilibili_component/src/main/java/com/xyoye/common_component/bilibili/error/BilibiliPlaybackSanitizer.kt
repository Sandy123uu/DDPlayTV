package com.xyoye.common_component.bilibili.error

import android.net.Uri

internal object BilibiliPlaybackSanitizer {
    fun sanitizeValue(
        key: String,
        value: String
    ): String {
        if (value.isBlank()) return value
        if (key.contains("cookie", ignoreCase = true) || key.contains("authorization", ignoreCase = true)) {
            return "<redacted>"
        }
        if (key.contains("url", ignoreCase = true)) {
            return sanitizeUrl(value).orEmpty()
        }
        return value.take(300)
    }

    fun sanitizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return "<invalid_url>"
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme != "http" && scheme != "https") {
            return url.take(300)
        }

        val allowValueKeys =
            setOf(
                "expires",
                "expire",
                "deadline",
                "ts",
                "timestamp",
                "qn",
                "quality",
            )

        return buildString {
            append(scheme)
            append("://")
            append(uri.authority.orEmpty())
            append(uri.path.orEmpty())

            val names = runCatching { uri.queryParameterNames }.getOrNull().orEmpty()
            if (names.isNotEmpty()) {
                append("?")
                append(
                    names.sorted().joinToString("&") { name ->
                        val value = uri.getQueryParameter(name).orEmpty()
                        val safeValue =
                            if (allowValueKeys.contains(name.lowercase())) {
                                value.take(120)
                            } else {
                                "<redacted>"
                            }
                        "$name=$safeValue"
                    },
                )
            }
        }
    }
}
