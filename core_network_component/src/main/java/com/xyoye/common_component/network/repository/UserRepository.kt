package com.xyoye.common_component.network.repository

import com.xyoye.common_component.config.DeveloperCredentialStore
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.helper.DanDanAuthHash
import com.xyoye.common_component.network.request.PassThroughException

/**
 * Created by xyoye on 2024/1/6.
 */

object UserRepository : BaseRepository() {
    class MissingDeveloperCredentialException :
        IllegalStateException("未配置开发者凭证（AppId/AppSecret）"),
        PassThroughException

    /**
     * 账号登录
     */
    suspend fun login(
        account: String,
        password: String
    ) = run {
        val (appId, appSecret) = requireDeveloperCredential()

        val unixTimestampSec = System.currentTimeMillis() / 1000
        val hash =
            DanDanAuthHash.loginHash(
                appId = appId,
                password = password,
                unixTimestampSec = unixTimestampSec,
                userName = account,
                appSecret = appSecret,
            )

        login(
            account = account,
            password = password,
            appId = appId,
            timestamp = unixTimestampSec.toString(),
            sign = hash,
        )
    }

    suspend fun login(
        account: String,
        password: String,
        appId: String,
        timestamp: String,
        sign: String
    ) = request()
        .param("userName", account)
        .param("password", password)
        .param("appId", appId)
        .param("unixTimestamp", timestamp.toLongOrNull() ?: timestamp)
        .param("hash", sign)
        .doPost {
            RetrofitManager.danDanService.login(it)
        }

    /**
     * 刷新Token
     */
    suspend fun refreshToken() =
        request()
            .doGet {
                RetrofitManager.danDanService.refreshToken()
            }

    /**
     * 注册账号
     */
    suspend fun register(
        account: String,
        password: String,
        screenName: String,
        email: String
    ) = run {
        val (appId, appSecret) = requireDeveloperCredential()

        val unixTimestampSec = System.currentTimeMillis() / 1000
        val hash =
            DanDanAuthHash.registerHash(
                appId = appId,
                email = email,
                password = password,
                screenName = screenName,
                unixTimestampSec = unixTimestampSec,
                userName = account,
                appSecret = appSecret,
            )

        register(
            account = account,
            password = password,
            screenName = screenName,
            email = email,
            appId = appId,
            timestamp = unixTimestampSec.toString(),
            sign = hash,
        )
    }

    suspend fun register(
        account: String,
        password: String,
        screenName: String,
        email: String,
        appId: String,
        timestamp: String,
        sign: String
    ) = request()
        .param("userName", account)
        .param("password", password)
        .param("screenName", screenName)
        .param("email", email)
        .param("appId", appId)
        .param("unixTimestamp", timestamp.toLongOrNull() ?: timestamp)
        .param("hash", sign)
        .doPost {
            RetrofitManager.danDanService.register(it)
        }

    /**
     * 重置密码
     */
    suspend fun resetPassword(
        account: String,
        email: String
    ) = run {
        val (appId, appSecret) = requireDeveloperCredential()

        val unixTimestampSec = System.currentTimeMillis() / 1000
        val hash =
            DanDanAuthHash.resetPasswordHash(
                appId = appId,
                email = email,
                unixTimestampSec = unixTimestampSec,
                userName = account,
                appSecret = appSecret,
            )

        resetPassword(
            account = account,
            email = email,
            appId = appId,
            timestamp = unixTimestampSec.toString(),
            sign = hash,
        )
    }

    suspend fun resetPassword(
        account: String,
        email: String,
        appId: String,
        timestamp: String,
        sign: String
    ) = request()
        .param("userName", account)
        .param("email", email)
        .param("appId", appId)
        .param("unixTimestamp", timestamp.toLongOrNull() ?: timestamp)
        .param("hash", sign)
        .doPost {
            RetrofitManager.danDanService.resetPassword(it)
        }

    /**
     * 找回账号
     */
    suspend fun retrieveAccount(email: String) =
        run {
            val (appId, appSecret) = requireDeveloperCredential()

            val unixTimestampSec = System.currentTimeMillis() / 1000
            val hash =
                DanDanAuthHash.findMyIdHash(
                    appId = appId,
                    email = email,
                    unixTimestampSec = unixTimestampSec,
                    appSecret = appSecret,
                )

            retrieveAccount(
                email = email,
                appId = appId,
                timestamp = unixTimestampSec.toString(),
                sign = hash,
            )
        }

    suspend fun retrieveAccount(
        email: String,
        appId: String,
        timestamp: String,
        sign: String
    ) = request()
        .param("email", email)
        .param("appId", appId)
        .param("unixTimestamp", timestamp.toLongOrNull() ?: timestamp)
        .param("hash", sign)
        .doPost {
            RetrofitManager.danDanService.retrieveAccount(it)
        }

    /**
     * 修改昵称
     */
    suspend fun updateScreenName(screenName: String) =
        request()
            .param("screenName", screenName)
            .doPost {
                RetrofitManager.danDanService.updateScreenName(it)
            }

    /**
     * 修改密码
     */
    suspend fun updatePassword(
        oldPassword: String,
        newPassword: String
    ) = request()
        .param("oldPassword", oldPassword)
        .param("newPassword", newPassword)
        .doPost {
            RetrofitManager.danDanService.updatePassword(it)
        }

    /**
     * 校验凭证
     */
    suspend fun checkAuthenticate(
        appId: String,
        appSecret: String
    ) = request()
        .doGet {
            RetrofitManager.danDanService.checkAuthenticate(appId, appSecret, 1)
        }

    private fun requireDeveloperCredential(): Pair<String, String> {
        val appId = DeveloperCredentialStore.getAppId()?.trim().orEmpty()
        val appSecret = DeveloperCredentialStore.getAppSecret()?.trim().orEmpty()
        if (appId.isEmpty() || appSecret.isEmpty()) {
            throw MissingDeveloperCredentialException()
        }
        return appId to appSecret
    }
}
