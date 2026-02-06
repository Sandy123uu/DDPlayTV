package com.xyoye.common_component.network.helper

import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class OkHttpClientConfig(
    val tlsPolicy: OkHttpTlsPolicy = OkHttpTlsPolicy.Strict,
    val connectTimeoutSeconds: Long = DEFAULT_CONNECT_TIMEOUT_SECONDS,
    val readTimeoutSeconds: Long = DEFAULT_READ_TIMEOUT_SECONDS,
    val writeTimeoutSeconds: Long = DEFAULT_WRITE_TIMEOUT_SECONDS,
    val callTimeoutSeconds: Long? = null,
    val cookieJar: CookieJar? = null,
    val interceptors: List<Interceptor> = emptyList(),
    val networkInterceptors: List<Interceptor> = emptyList()
) {
    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 15L
        const val DEFAULT_READ_TIMEOUT_SECONDS = 30L
        const val DEFAULT_WRITE_TIMEOUT_SECONDS = 10L
    }
}

object OkHttpClientFactory {
    fun create(config: OkHttpClientConfig = OkHttpClientConfig()): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)

        config.callTimeoutSeconds?.let { builder.callTimeout(it, TimeUnit.SECONDS) }
        config.cookieJar?.let(builder::cookieJar)
        config.interceptors.forEach(builder::addInterceptor)
        config.networkInterceptors.forEach(builder::addNetworkInterceptor)

        OkHttpTlsConfigurer.apply(builder, config.tlsPolicy)

        return builder.build()
    }
}
