package com.xyoye.common_component.network.helper

import com.xyoye.core_network_component.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * WebDAV 专用 OkHttpClient（严格 TLS：不忽略证书/不忽略主机名校验）。
 *
 * 注意：需要兼容 WebDAV 的重定向授权头（见 [RedirectAuthorizationInterceptor]）。
 */
object WebDavOkHttpClient {
    val client: OkHttpClient by lazy {
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
        val builder =
            OkHttpClient
                .Builder()
                .cookieJar(cookieStore)
                .addNetworkInterceptor(RedirectAuthorizationInterceptor())
        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(LoggerInterceptor().webDav())
        }
        return@lazy builder.build()
    }
}
