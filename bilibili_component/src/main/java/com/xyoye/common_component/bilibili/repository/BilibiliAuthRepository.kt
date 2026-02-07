package com.xyoye.common_component.bilibili.repository

import com.xyoye.common_component.bilibili.BilibiliApiType
import com.xyoye.common_component.bilibili.login.BilibiliLoginPollResult
import com.xyoye.common_component.bilibili.login.BilibiliLoginQrCode
import com.xyoye.data_component.data.bilibili.BilibiliNavData
import com.xyoye.data_component.data.bilibili.BilibiliQrcodeGenerateData
import com.xyoye.data_component.data.bilibili.BilibiliQrcodePollData

class BilibiliAuthRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    fun isLoggedIn(): Boolean = core.isLoggedIn()

    fun cookieHeaderOrNull(): String? = core.cookieHeaderOrNull()

    fun currentApiType(): BilibiliApiType = core.currentApiType()

    suspend fun nav(): Result<BilibiliNavData> = core.nav()

    suspend fun qrcodeGenerate(): Result<BilibiliQrcodeGenerateData> = core.qrcodeGenerate()

    suspend fun qrcodePoll(qrcodeKey: String): Result<BilibiliQrcodePollData> = core.qrcodePoll(qrcodeKey)

    suspend fun loginQrCodeGenerate(apiType: BilibiliApiType = currentApiType()): Result<BilibiliLoginQrCode> =
        core.loginQrCodeGenerate(apiType)

    suspend fun loginQrCodePoll(
        qrcodeKey: String,
        apiType: BilibiliApiType = currentApiType()
    ): Result<BilibiliLoginPollResult> = core.loginQrCodePoll(qrcodeKey, apiType)

    fun clear() {
        core.clear()
    }
}
