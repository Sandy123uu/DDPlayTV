package com.xyoye.common_component.bilibili.app

import com.xyoye.common_component.bilibili.error.BilibiliException
import com.xyoye.common_component.config.BilibiliTvCredentialStore
import com.xyoye.common_component.network.request.RequestParams

/**
 * TV 客户端（云视听小电视）相关固定参数。
 *
 * 参考：`.tmp/bilibili-API-collect/docs/misc/sign/APPKey.md`
 */
object BilibiliTvClient {
    private const val MISSING_CREDENTIAL_MESSAGE =
        "未配置 B 站 TV APP_KEY/APP_SEC：TV 登录/签名已禁用，请在「个人中心-开发者设置」配置，或构建时注入 BILIBILI_TV_APP_KEY/BILIBILI_TV_APP_SECRET"

    const val MOBI_APP = "android_tv_yst"
    const val PLATFORM = "android"

    // TV 端扫码登录参数，可为 0
    const val LOCAL_ID = 0

    data class AppCredential(
        val appKey: String,
        val appSecret: String
    )

    fun isAppCredentialReady(): Boolean = resolveAppCredentialOrNull() != null

    fun resolveAppCredentialOrNull(): AppCredential? {
        val appKey = BilibiliTvCredentialStore.getAppKey()?.trim().orEmpty()
        val appSecret = BilibiliTvCredentialStore.getAppSecret()?.trim().orEmpty()
        if (appKey.isBlank() || appSecret.isBlank()) {
            return null
        }
        return AppCredential(
            appKey = appKey,
            appSecret = appSecret,
        )
    }

    fun missingCredentialException(): BilibiliException =
        BilibiliException.from(
            code = -1,
            message = MISSING_CREDENTIAL_MESSAGE,
        )

    fun requireAppCredential(): AppCredential =
        resolveAppCredentialOrNull()
            ?: throw missingCredentialException()

    fun sign(params: Map<String, Any?>): RequestParams {
        val credential = requireAppCredential()
        return BilibiliAppSigner.sign(
            params = params,
            appKey = credential.appKey,
            appSec = credential.appSecret,
        )
    }
}
