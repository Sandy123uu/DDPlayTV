package com.xyoye.common_component.storage.cloud115.net

import com.xyoye.data_component.data.cloud115.Cloud115Credential

/**
 * 115 Cloud 请求头策略。
 *
 * 目标：
 * - 统一 User-Agent
 * - 统一 Cookie 拼装（UID/CID/SEID/KID）
 * - 提供脱敏工具，避免日志输出敏感信息
 */
object Cloud115Headers {
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_COOKIE = "Cookie"

    // 115Browser/115disk 风格 UA，具体版本不敏感，保持稳定即可
    const val USER_AGENT = "Mozilla/5.0 115Browser/27.0.0.0"

    fun buildCookieHeader(cookie: Cloud115Credential?): String =
        if (cookie == null) {
            ""
        } else {
            buildCookieHeader(
                uid = cookie.uid,
                cid = cookie.cid,
                seid = cookie.seid,
                kid = cookie.kid,
            )
        }

    fun buildCookieHeader(
        uid: String?,
        cid: String?,
        seid: String?,
        kid: String?
    ): String {
        val parts = ArrayList<String>(4)
        uid?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("UID=$it") }
        cid?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("CID=$it") }
        seid?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("SEID=$it") }
        kid?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add("KID=$it") }
        return parts.joinToString(separator = "; ")
    }

    fun redactCookie(cookie: String?): String {
        val raw = cookie?.trim().orEmpty()
        if (raw.isBlank()) {
            return ""
        }

        return raw
            .split(";")
            .mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isBlank()) {
                    return@mapNotNull null
                }
                val idx = trimmed.indexOf('=')
                if (idx <= 0 || idx == trimmed.lastIndex) {
                    return@mapNotNull "<redacted>"
                }
                val key = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                "$key=<redacted len=${value.length}>"
            }.joinToString(separator = "; ")
    }

    fun redactHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (key, value) ->
            when (key) {
                HEADER_COOKIE -> redactCookie(value)
                else -> value
            }
        }
}
