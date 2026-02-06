package com.xyoye.common_component.bilibili.repository

import com.xyoye.data_component.data.bilibili.BilibiliGaiaVgateRegisterData

class BilibiliRiskRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    suspend fun gaiaVgateRegister(vVoucher: String): Result<BilibiliGaiaVgateRegisterData> = core.gaiaVgateRegister(vVoucher)

    suspend fun gaiaVgateValidate(
        challenge: String,
        token: String,
        validate: String,
        seccode: String
    ): Result<String> =
        core.gaiaVgateValidate(
            challenge = challenge,
            token = token,
            validate = validate,
            seccode = seccode,
        )
}
