package com.xyoye.common_component.network.helper

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Local proxy (NanoHTTPD) use-case OkHttpClient factory.
 *
 * - Default is strict TLS (platform trust manager + hostname verifier).
 * - Optional insecure TLS must be explicitly opted-in by the caller (see [OkHttpTlsPolicy.UnsafeTrustAll]).
 * - Includes an in-memory CookieJar for stream workflows that rely on Set-Cookie across requests.
 * - Includes [RedirectAuthorizationInterceptor] to preserve Authorization headers across redirects.
 */
object ProxyOkHttpClientFactory {
    fun create(tlsPolicy: OkHttpTlsPolicy = OkHttpTlsPolicy.Strict): OkHttpClient {
        val cookieStore =
            object : CookieJar {
                private val store = mutableMapOf<String, MutableList<Cookie>>()

                override fun saveFromResponse(
                    url: HttpUrl,
                    cookies: List<Cookie>
                ) {
                    if (cookies.isEmpty()) return
                    store[url.host] = cookies.toMutableList()
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host] ?: emptyList()
            }

        return OkHttpClientFactory.create(
            OkHttpClientConfig(
                tlsPolicy = tlsPolicy,
                cookieJar = cookieStore,
                networkInterceptors = listOf(RedirectAuthorizationInterceptor()),
            ),
        )
    }
}
