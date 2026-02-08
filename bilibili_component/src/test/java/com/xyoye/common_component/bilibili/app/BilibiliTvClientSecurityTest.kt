package com.xyoye.common_component.bilibili.app

import com.xyoye.common_component.bilibili.error.BilibiliException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliTvClientSecurityTest {
    @After
    fun tearDown() {
        BilibiliTvClient.installCredentialProviderForTest(null)
    }

    @Test
    fun resolveCredentialTrimsInputAndReportsReady() {
        BilibiliTvClient.installCredentialProviderForTest {
            BilibiliTvClient.AppCredential(appKey = "  testKey  ", appSecret = "  testSecret  ")
        }

        val credential = BilibiliTvClient.requireAppCredential()

        assertTrue(BilibiliTvClient.isAppCredentialReady())
        assertEquals("testKey", credential.appKey)
        assertEquals("testSecret", credential.appSecret)
    }

    @Test
    fun signThrowsWhenCredentialMissing() {
        BilibiliTvClient.installCredentialProviderForTest { null }

        val error =
            runCatching { BilibiliTvClient.sign(mapOf("aid" to 1)) }
                .exceptionOrNull() as? BilibiliException

        assertNotNull(error)
        assertEquals(-1, error?.code)
        assertTrue(error?.bilibiliMessage?.contains("TV 登录") == true)
    }

    @Test
    fun signUsesRuntimeCredentialInsteadOfHardcodedValue() {
        BilibiliTvClient.installCredentialProviderForTest {
            BilibiliTvClient.AppCredential(appKey = "runtimeKey", appSecret = "runtimeSecret")
        }

        val signed = BilibiliTvClient.sign(mapOf("bvid" to "BV1xx411c7mD"))

        assertEquals("runtimeKey", signed["appkey"])
        assertNotNull(signed["sign"])
    }
}
