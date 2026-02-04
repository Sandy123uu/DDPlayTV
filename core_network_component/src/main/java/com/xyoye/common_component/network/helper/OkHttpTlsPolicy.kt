package com.xyoye.common_component.network.helper

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * TLS policy for an [okhttp3.OkHttpClient.Builder].
 *
 * Notes:
 * - [Strict] is always the default.
 * - Prefer [CustomCaCertificates] / [PinnedPublicKeys] over [UnsafeTrustAll].
 */
sealed interface OkHttpTlsPolicy {
    /**
     * Strict TLS verification: use platform defaults (recommended).
     */
    data object Strict : OkHttpTlsPolicy

    /**
     * Trust custom CA certificates in addition to platform defaults.
     *
     * Typical use-case: self-signed certificates or private PKI.
     */
    data class CustomCaCertificates(
        val certificates: List<X509Certificate>
    ) : OkHttpTlsPolicy {
        init {
            require(certificates.isNotEmpty()) { "certificates must not be empty" }
        }

        companion object {
            /**
             * Parse certificates from a PEM/DER input stream.
             */
            fun fromInputStream(inputStream: InputStream): CustomCaCertificates {
                val certificates = parseX509Certificates(inputStream)
                return CustomCaCertificates(certificates)
            }

            /**
             * Parse certificates from a PEM/DER byte array.
             */
            fun fromBytes(bytes: ByteArray): CustomCaCertificates = fromInputStream(ByteArrayInputStream(bytes))
        }
    }

    /**
     * Public key pinning via OkHttp [okhttp3.CertificatePinner].
     *
     * Example pin value: `sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=`
     */
    data class PinnedPublicKeys(
        val hostname: String,
        val sha256Pins: List<String>
    ) : OkHttpTlsPolicy {
        init {
            require(hostname.isNotBlank()) { "hostname must not be blank" }
            require(sha256Pins.isNotEmpty()) { "sha256Pins must not be empty" }
        }
    }

    /**
     * Insecure TLS (trust-all certificates + ignore hostname verification).
     *
     * MUST be protected by explicit user opt-in. Prefer safer alternatives.
     */
    @UnsafeTlsApi
    data object UnsafeTrustAll : OkHttpTlsPolicy
}

private fun parseX509Certificates(inputStream: InputStream): List<X509Certificate> {
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificates = certificateFactory.generateCertificates(inputStream).filterIsInstance<X509Certificate>()
    require(certificates.isNotEmpty()) { "No X509 certificates found" }
    return certificates
}
