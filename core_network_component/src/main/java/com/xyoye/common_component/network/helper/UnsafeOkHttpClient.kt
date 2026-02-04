package com.xyoye.common_component.network.helper

import okhttp3.OkHttpClient

/**
 * Created by xyoye on 2021/5/2.
 *
 * 忽略证书验证的OkHttpClient
 */
@UnsafeTlsApi
@Deprecated(
    message =
        "Deprecated: use OkHttpTlsPolicy/OkHttpTlsConfigurer (custom CA / pinning) instead. " +
            "Trust-all MUST be gated by explicit user opt-in.",
)
object UnsafeOkHttpClient {
    val client: OkHttpClient by lazy {
        WebDavOkHttpClientFactory.create(OkHttpTlsPolicy.UnsafeTrustAll)
    }
}
