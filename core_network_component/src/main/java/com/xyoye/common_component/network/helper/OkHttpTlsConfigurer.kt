package com.xyoye.common_component.network.helper

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Applies [OkHttpTlsPolicy] to an [OkHttpClient.Builder].
 *
 * This helper is intentionally "narrow" (TLS only). Timeout/interceptor unification is handled elsewhere.
 */
object OkHttpTlsConfigurer {
    private const val TLS_PROTOCOL = "TLSv1.2"

    @OptIn(UnsafeTlsApi::class)
    fun apply(
        builder: OkHttpClient.Builder,
        tlsPolicy: OkHttpTlsPolicy
    ): OkHttpClient.Builder =
        when (tlsPolicy) {
            OkHttpTlsPolicy.Strict -> builder
            is OkHttpTlsPolicy.PinnedPublicKeys -> applyPinnedPublicKeys(builder, tlsPolicy)
            is OkHttpTlsPolicy.CustomCaCertificates -> applyCustomCaCertificates(builder, tlsPolicy)
            OkHttpTlsPolicy.UnsafeTrustAll -> applyUnsafeTrustAll(builder)
        }

    private fun applyPinnedPublicKeys(
        builder: OkHttpClient.Builder,
        policy: OkHttpTlsPolicy.PinnedPublicKeys
    ): OkHttpClient.Builder {
        val pinnerBuilder = CertificatePinner.Builder()
        policy.sha256Pins.forEach { pin ->
            pinnerBuilder.add(policy.hostname, pin)
        }
        return builder.certificatePinner(pinnerBuilder.build())
    }

    private fun applyCustomCaCertificates(
        builder: OkHttpClient.Builder,
        policy: OkHttpTlsPolicy.CustomCaCertificates
    ): OkHttpClient.Builder {
        val system = systemDefaultTrustManager()
        val custom = trustManagerForCertificates(policy.certificates)
        val trustManager = CompositeX509TrustManager(listOf(custom, system))
        val sslContext =
            SSLContext.getInstance(TLS_PROTOCOL).apply {
                init(null, arrayOf(trustManager), SecureRandom())
            }
        return builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }

    @OptIn(UnsafeTlsApi::class)
    private fun applyUnsafeTrustAll(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val trustManager = TrustAllX509TrustManager()
        val sslContext =
            SSLContext.getInstance(TLS_PROTOCOL).apply {
                init(null, arrayOf(trustManager), SecureRandom())
            }
        return builder
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
    }

    private fun systemDefaultTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        return trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun trustManagerForCertificates(certificates: List<X509Certificate>): X509TrustManager {
        val keyStore =
            KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null)
                certificates.forEachIndexed { index, certificate ->
                    setCertificateEntry("ca-$index", certificate)
                }
            }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        return trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private class CompositeX509TrustManager(
        private val delegates: List<X509TrustManager>
    ) : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
            var last: Exception? = null
            delegates.forEach { manager ->
                try {
                    manager.checkClientTrusted(chain, authType)
                    return
                } catch (e: Exception) {
                    last = e
                }
            }
            throw CertificateException("No trust manager accepted client certificate", last)
        }

        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
            var last: Exception? = null
            delegates.forEach { manager ->
                try {
                    manager.checkServerTrusted(chain, authType)
                    return
                } catch (e: Exception) {
                    last = e
                }
            }
            throw CertificateException("No trust manager accepted server certificate", last)
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            delegates
                .flatMap { it.acceptedIssuers.toList() }
                .distinct()
                .toTypedArray()
    }

    @OptIn(UnsafeTlsApi::class)
    @Suppress("kotlin:S4830")
    private class TrustAllX509TrustManager : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) = Unit

        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
