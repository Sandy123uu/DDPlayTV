package com.xyoye.common_component.log.http.auth

import com.xyoye.common_component.log.http.model.ErrorResponse
import fi.iki.elonen.NanoHTTPD
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale

sealed class HttpAuthResult {
    data object Ok : HttpAuthResult()

    data class Unauthorized(
        val response: ErrorResponse,
    ) : HttpAuthResult()
}

object HttpRequestAuth {
    private const val HEADER_AUTHORIZATION = "authorization"
    private const val QUERY_TOKEN = "token"
    private const val BEARER_PREFIX = "bearer "

    fun authorize(
        session: NanoHTTPD.IHTTPSession,
        expectedToken: String,
    ): HttpAuthResult {
        val token = resolveToken(session).orEmpty()
        if (expectedToken.isBlank() || token.isBlank() || token != expectedToken) {
            return unauthorized("unauthorized")
        }
        val remoteIp = session.remoteIpAddress?.trim().orEmpty()
        if (!isLanAllowed(remoteIp)) {
            return unauthorized("lan only")
        }
        return HttpAuthResult.Ok
    }

    private fun resolveToken(session: NanoHTTPD.IHTTPSession): String? {
        val authorization = session.headers[HEADER_AUTHORIZATION]?.trim().orEmpty()
        if (authorization.isNotEmpty()) {
            val lower = authorization.lowercase(Locale.US)
            if (lower.startsWith(BEARER_PREFIX)) {
                return authorization.substring(BEARER_PREFIX.length).trim().takeIf { it.isNotEmpty() }
            }
        }
        return session.parms[QUERY_TOKEN]?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun isLanAllowed(remoteIp: String): Boolean {
        if (remoteIp.isBlank()) return false
        val address =
            runCatching { InetAddress.getByName(remoteIp) }.getOrNull()
                ?: return false
        if (address.isLoopbackAddress) return true
        if (address.isSiteLocalAddress) return true
        if (address is Inet6Address && isUlaIpv6(address)) return true
        return false
    }

    private fun isUlaIpv6(address: Inet6Address): Boolean {
        val bytes = address.address ?: return false
        if (bytes.isEmpty()) return false
        return (bytes[0].toInt() and 0xfe) == 0xfc
    }

    private fun unauthorized(message: String): HttpAuthResult.Unauthorized =
        HttpAuthResult.Unauthorized(
            ErrorResponse(
                errorCode = 401,
                errorMessage = message,
            ),
        )
}

