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
    private const val MISSING_TV_LOGIN_CONFIG_MESSAGE =
        "未配置 B 站 TV 登录参数：TV 登录/签名已禁用，请在「个人中心-开发者设置」完成配置，或通过构建参数注入。"

    const val MOBI_APP = "android_tv_yst"
    const val PLATFORM = "android"

    // TV 端扫码登录参数，可为 0
    const val LOCAL_ID = 0

    @Volatile
    private var hasTestCredentialProvider = false

    @Volatile
    private var testCredentialProvider: (() -> AppCredential?)? = null

    data class AppCredential(
        val appKey: String,
        val appSecret: String
    )

    internal fun installCredentialProviderForTest(provider: (() -> AppCredential?)?) {
        testCredentialProvider = provider
        hasTestCredentialProvider = provider != null
    }

    fun isAppCredentialReady(): Boolean = resolveAppCredentialOrNull() != null

    fun resolveAppCredentialOrNull(): AppCredential? {
        val rawCredential =
            if (hasTestCredentialProvider) {
                testCredentialProvider?.invoke()
            } else {
                resolveCredentialFromStore()
            }
        return normalizeCredential(rawCredential)
    }

    private fun resolveCredentialFromStore(): AppCredential? {
        val appKey = BilibiliTvCredentialStore.getAppKey()
        val appSecret = BilibiliTvCredentialStore.getAppSecret()
        if (appKey == null || appSecret == null) {
            return null
        }
        return AppCredential(
            appKey = appKey,
            appSecret = appSecret,
        )
    }

    private fun normalizeCredential(rawCredential: AppCredential?): AppCredential? {
        val appKey = rawCredential?.appKey?.trim().orEmpty()
        val appSecret = rawCredential?.appSecret?.trim().orEmpty()
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
            message = MISSING_TV_LOGIN_CONFIG_MESSAGE,
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
