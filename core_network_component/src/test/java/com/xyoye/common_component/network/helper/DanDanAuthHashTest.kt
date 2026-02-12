package com.xyoye.common_component.network.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class DanDanAuthHashTest {
    @Test
    fun loginHashMatchesSwaggerExample() {
        val hash =
            DanDanAuthHash.loginHash(
                appId = "dandanplay",
                password = "test2",
                unixTimestampSec = 666666666,
                userName = "test1",
                appSecret = "FFFFF",
            )

        assertEquals("0f4239a22d4c2775c665cad348e864bc", hash)
    }

    @Test
    fun registerHashMatchesSwaggerExample() {
        val hash =
            DanDanAuthHash.registerHash(
                appId = "dandanplay",
                email = "test3@example.com",
                password = "test2",
                screenName = "弹弹",
                unixTimestampSec = 666666666,
                userName = "test1",
                appSecret = "FFFFF",
            )

        assertEquals("5d4643c36fe3e1a49c6e109ff997bf3c", hash)
    }

    @Test
    fun resetPasswordHashMatchesSwaggerExample() {
        val hash =
            DanDanAuthHash.resetPasswordHash(
                appId = "dandanplay",
                email = "test3@example.com",
                unixTimestampSec = 666666666,
                userName = "test1",
                appSecret = "FFFFF",
            )

        assertEquals("9ad393281eac6da9b4beb9ce68f35d0c", hash)
    }

    @Test
    fun findMyIdHashMatchesSwaggerExample() {
        val hash =
            DanDanAuthHash.findMyIdHash(
                appId = "dandanplay",
                email = "test3@example.com",
                unixTimestampSec = 666666666,
                appSecret = "FFFFF",
            )

        assertEquals("9e09e648a4489af515dac520aa39af75", hash)
    }
}

