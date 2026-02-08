package com.xyoye.common_component.network.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiSecurityConfigTest {
    @Test
    fun securitySensitiveEndpointsMustUseHttps() {
        val insecure = Api.insecureSecuritySensitiveEndpoints()

        assertTrue("Sensitive endpoints must use HTTPS: $insecure", insecure.isEmpty())
    }

    @Test
    fun baiduAccountEndpointUsesSafeAliasAndExpectedHost() {
        assertEquals("https://openapi.baidu.com/", Api.BAIDU_ACCOUNT_API)
        assertEquals("https://openapi.baidu.com/", Api.securitySensitiveEndpoints().getValue("baidu_account"))
    }

    @Test
    fun sensitiveEndpointKeysAvoidSecretLikeNames() {
        val riskyKeys =
            Api.securitySensitiveEndpoints().keys.filter { key ->
                val lower = key.lowercase()
                lower.contains("auth") || lower.contains("secret") || lower.contains("credential")
            }

        assertTrue("Sensitive endpoint aliases should avoid secret-like names: $riskyKeys", riskyKeys.isEmpty())
    }
}
