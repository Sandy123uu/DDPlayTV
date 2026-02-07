package com.xyoye.common_component.network.helper

import okhttp3.OkHttpClient

/**
 * WebDAV 专用 OkHttpClient（严格 TLS：不忽略证书/不忽略主机名校验）。
 *
 * 注意：需要兼容 WebDAV 的重定向授权头（见 [RedirectAuthorizationInterceptor]）。
 */
object WebDavOkHttpClient {
    val client: OkHttpClient by lazy {
        WebDavOkHttpClientFactory.create(OkHttpTlsPolicy.Strict)
    }
}
