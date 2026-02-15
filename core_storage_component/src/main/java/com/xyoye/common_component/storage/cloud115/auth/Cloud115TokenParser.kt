package com.xyoye.common_component.storage.cloud115.auth

import com.xyoye.common_component.storage.cloud115.net.Cloud115Headers

object Cloud115TokenParser {
    data class ParsedToken(
        val userId: String,
        val cookieHeader: String
    )

    fun parse(input: String?): Result<ParsedToken> =
        runCatching {
            val raw = input?.trim().orEmpty()
            if (raw.isBlank()) {
                throw IllegalArgumentException("token 为空")
            }

            val withoutPrefix = raw.replaceFirst(Regex("^cookie\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            val pairs = parseCookiePairs(withoutPrefix)

            val uid = pairs["UID"]?.trim().orEmpty()
            val cid = pairs["CID"]?.trim().orEmpty()
            val seid = pairs["SEID"]?.trim().orEmpty()
            val kid = pairs["KID"]?.trim()?.takeIf { it.isNotBlank() }

            val missing =
                buildList {
                    if (uid.isBlank()) add("UID")
                    if (cid.isBlank()) add("CID")
                    if (seid.isBlank()) add("SEID")
                }

            if (missing.isNotEmpty()) {
                throw IllegalArgumentException("token 缺少 ${missing.joinToString(separator = "/")}")
            }

            val userId = uid.takeWhile { it.isDigit() }
            if (userId.isBlank()) {
                throw IllegalArgumentException("token 的 UID 格式不正确")
            }

            val cookieHeader =
                Cloud115Headers.buildCookieHeader(
                    uid = uid,
                    cid = cid,
                    seid = seid,
                    kid = kid,
                )
            if (cookieHeader.isBlank()) {
                throw IllegalArgumentException("token 解析失败")
            }

            ParsedToken(
                userId = userId,
                cookieHeader = cookieHeader,
            )
        }

    private fun parseCookiePairs(input: String): Map<String, String> {
        val map = LinkedHashMap<String, String>(8)
        input.split(";").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.isBlank()) return@forEach

            val idx = trimmed.indexOf('=')
            if (idx <= 0 || idx == trimmed.lastIndex) return@forEach

            val key = trimmed.substring(0, idx).trim().uppercase()
            val value = trimmed.substring(idx + 1).trim()

            when (key) {
                "UID", "CID", "SEID", "KID" -> map[key] = value
            }
        }

        return map
    }
}
