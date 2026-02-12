package com.xyoye.common_component.network.helper

import com.xyoye.common_component.utils.HashUtils

/**
 * 弹弹play OpenAPI v2 登录/注册相关接口的 hash 计算工具。
 *
 * 规则来源：https://api.dandanplay.net/swagger/v2/swagger.json
 */
internal object DanDanAuthHash {
    internal fun loginHash(
        appId: String,
        password: String,
        unixTimestampSec: Long,
        userName: String,
        appSecret: String
    ): String = HashUtils.md5Hex(appId + password + unixTimestampSec + userName + appSecret)

    internal fun registerHash(
        appId: String,
        email: String,
        password: String,
        screenName: String,
        unixTimestampSec: Long,
        userName: String,
        appSecret: String
    ): String = HashUtils.md5Hex(appId + email + password + screenName + unixTimestampSec + userName + appSecret)

    internal fun resetPasswordHash(
        appId: String,
        email: String,
        unixTimestampSec: Long,
        userName: String,
        appSecret: String
    ): String = HashUtils.md5Hex(appId + email + unixTimestampSec + userName + appSecret)

    internal fun findMyIdHash(
        appId: String,
        email: String,
        unixTimestampSec: Long,
        appSecret: String
    ): String = HashUtils.md5Hex(appId + email + unixTimestampSec + appSecret)
}

