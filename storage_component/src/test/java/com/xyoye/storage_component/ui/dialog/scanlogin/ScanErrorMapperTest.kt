package com.xyoye.storage_component.ui.dialog.scanlogin

import com.xyoye.common_component.network.request.NetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ScanErrorMapperTest {
    @Test
    fun mapUnknownHostToNetworkCategory() {
        val failure =
            ScanErrorMapper.map(
                providerId = "cloud115",
                step = ScanStep.FETCH_QR,
                throwable = UnknownHostException("no host"),
            )

        assertEquals(ScanFailureCategory.NETWORK, failure.category)
        assertTrue(failure.retryable)
        assertTrue(failure.debugCode.startsWith("cloud115_fetch_qr_"))
    }

    @Test
    fun mapSslHandshakeToSslCategory() {
        val failure =
            ScanErrorMapper.map(
                providerId = "bilibili",
                step = ScanStep.POLL_STATUS,
                throwable = SSLHandshakeException("ssl failed"),
            )

        assertEquals(ScanFailureCategory.SSL, failure.category)
        assertTrue(failure.retryable)
        assertTrue(failure.debugCode.contains("SSL_HANDSHAKE"))
    }

    @Test
    fun mapNetworkExceptionCodeToHttpCategory() {
        val failure =
            ScanErrorMapper.map(
                providerId = "baidupan",
                step = ScanStep.FETCH_QR,
                throwable = NetworkException(503, "server down", IllegalStateException()),
            )

        assertEquals(ScanFailureCategory.HTTP, failure.category)
        assertEquals("服务暂时不可用（HTTP 503），请稍后重试", failure.userMessage)
        assertTrue(failure.retryable)
    }

    @Test
    fun mapConfigExceptionAsNonRetryable() {
        val failure =
            ScanErrorMapper.map(
                providerId = "baidupan",
                step = ScanStep.FETCH_QR,
                throwable = ScanConfigException("缺少配置", "CONFIG_MISSING"),
            )

        assertEquals(ScanFailureCategory.CONFIG, failure.category)
        assertFalse(failure.retryable)
        assertTrue(failure.debugCode.endsWith("CONFIG_MISSING"))
    }
}
