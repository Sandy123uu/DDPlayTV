package com.xyoye.common_component.network.helper

import com.xyoye.core_network_component.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * WebDAV 专用 OkHttpClient 工厂。
 *
 * - 默认严格 TLS（不忽略证书/不忽略主机名校验）。
 * - 如需兼容自签证书，优先使用 [OkHttpTlsPolicy.CustomCaCertificates] 或 [OkHttpTlsPolicy.PinnedPublicKeys]。
 * - [OkHttpTlsPolicy.UnsafeTrustAll] 仅作为最后手段，且必须由用户显式开启。
 */
object WebDavOkHttpClientFactory {
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

        val builder =
            OkHttpClient
                .Builder()
                .cookieJar(cookieStore)
                .addNetworkInterceptor(RedirectAuthorizationInterceptor())

        OkHttpTlsConfigurer.apply(builder, tlsPolicy)

        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(LoggerInterceptor().webDav())
        }

        return builder.build()
    }
}
