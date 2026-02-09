package com.xyoye.storage_component.ui.dialog.scanlogin.provider

import android.app.Activity
import android.net.Uri
import android.os.SystemClock
import com.xyoye.common_component.config.BaiduPanOpenApiConfig
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.common_component.utils.QrCodeHelper
import com.xyoye.common_component.utils.dp2px
import com.xyoye.data_component.data.baidupan.oauth.BaiduPanDeviceCodeResponse
import com.xyoye.data_component.data.baidupan.oauth.BaiduPanOAuthError
import com.xyoye.data_component.data.baidupan.oauth.BaiduPanTokenResponse
import com.xyoye.data_component.data.baidupan.xpan.BaiduPanUinfoResponse
import com.xyoye.storage_component.R
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanApiException
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanConfigException
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailure
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailureCategory
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanPollResult
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanQrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class BaiduPanScanSession(
    val deviceCode: String,
    val expiresAtElapsedMs: Long,
    val pollIntervalMs: Long,
)

data class BaiduPanScanLoginResult(
    val token: BaiduPanTokenResponse,
    val uinfo: BaiduPanUinfoResponse,
)

class BaiduPanScanProvider(
    private val activity: Activity,
) : ScanProvider<BaiduPanScanSession, android.graphics.Bitmap, BaiduPanScanLoginResult> {
    override val providerId: String = "baidupan"

    override suspend fun fetchQrCode(): Result<ScanQrPayload<BaiduPanScanSession, android.graphics.Bitmap>> =
        runCatching {
            if (!BaiduPanOpenApiConfig.isConfigured()) {
                throw ScanConfigException(
                    userMessage = "未配置百度网盘开放平台密钥",
                    debugCode = "CONFIG_MISSING",
                )
            }

            val deviceCode = fetchDeviceCode().getOrElse { throw it }
            if (deviceCode.deviceCode.isBlank() || deviceCode.userCode.isBlank()) {
                throw ScanApiException("获取二维码失败，请稍后重试", "DEVICE_CODE_INVALID")
            }

            val qrCode =
                QrCodeHelper.createQrCode(
                    context = activity,
                    content = buildQrCodeContent(deviceCode),
                    sizePx = dp2px(220),
                    logoResId = R.mipmap.ic_logo,
                    bitmapColor =
                        com.xyoye.core_ui_component.R.color.text_black
                            .toResColor(activity),
                    errorContext = "生成百度网盘授权二维码失败",
                ) ?: throw ScanApiException("生成二维码失败，请稍后重试", "QR_BITMAP_CREATE_FAILED")

            val intervalMs =
                TimeUnit.SECONDS.toMillis(
                    deviceCode.interval.coerceAtLeast(5).toLong(),
                )
            val expiresAtMs =
                SystemClock.elapsedRealtime() +
                    deviceCode.expiresIn.coerceAtLeast(1) * 1000L

            ScanQrPayload(
                session =
                    BaiduPanScanSession(
                        deviceCode = deviceCode.deviceCode,
                        expiresAtElapsedMs = expiresAtMs,
                        pollIntervalMs = intervalMs,
                    ),
                qrData = qrCode,
                hintMessage = "请使用百度网盘 App 扫码授权",
                pollIntervalMs = intervalMs,
            )
        }

    override suspend fun poll(session: BaiduPanScanSession): Result<ScanPollResult<BaiduPanScanLoginResult>> =
        runCatching {
            if (SystemClock.elapsedRealtime() >= session.expiresAtElapsedMs) {
                return@runCatching ScanPollResult.Failure(
                    ScanFailure(
                        userMessage = "二维码已过期，请点击重试",
                        category = ScanFailureCategory.API,
                        debugCode = "QR_EXPIRED",
                        retryable = true,
                    ),
                )
            }

            when (val outcome = fetchTokenByDeviceCode(session.deviceCode).getOrElse { throw it }) {
                is OauthTokenOutcome.Pending ->
                    ScanPollResult.Waiting(
                        message = outcome.message,
                        nextIntervalMs = session.pollIntervalMs,
                    )

                is OauthTokenOutcome.SlowDown ->
                    ScanPollResult.Waiting(
                        message = outcome.message,
                        nextIntervalMs = session.pollIntervalMs + SLOW_DOWN_EXTRA_DELAY_MS,
                    )

                is OauthTokenOutcome.TerminalError ->
                    ScanPollResult.Failure(
                        ScanFailure(
                            userMessage = outcome.message,
                            category = ScanFailureCategory.API,
                            debugCode = "TOKEN_TERMINAL",
                            retryable = true,
                        ),
                    )

                is OauthTokenOutcome.Success -> {
                    val token = outcome.token
                    val uinfo = fetchUinfo(token.accessToken).getOrElse { throw it }
                    ScanPollResult.Success(
                        payload =
                            BaiduPanScanLoginResult(
                                token = token,
                                uinfo = uinfo,
                            ),
                        successMessage = "授权成功",
                    )
                }
            }
        }

    private fun buildQrCodeContent(deviceCode: BaiduPanDeviceCodeResponse): String {
        val userCode = deviceCode.userCode.trim()
        val verificationUrl = deviceCode.verificationUrl?.trim()
        val baseUrl = verificationUrl?.takeIf { it.isNotEmpty() } ?: "https://openapi.baidu.com/device"
        return Uri
            .parse(baseUrl)
            .buildUpon()
            .appendQueryParameter("display", "mobile")
            .appendQueryParameter("code", userCode)
            .build()
            .toString()
    }

    private suspend fun fetchDeviceCode(): Result<BaiduPanDeviceCodeResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.baiduPanService.oauthDeviceCode(
                        baseUrl = Api.BAIDU_ACCOUNT_API,
                        responseType = "device_code",
                        clientId = BaiduPanOpenApiConfig.clientId,
                        scope = DEFAULT_SCOPE,
                    )
                val payload =
                    response.body()?.string()
                        ?: response.errorBody()?.string()
                        ?: ""

                val oauthError = JsonHelper.parseJson<BaiduPanOAuthError>(payload)
                if (oauthError != null && oauthError.error.isNullOrBlank().not()) {
                    throw ScanApiException(
                        userMessage = formatOauthMessage(oauthError.error.orEmpty(), oauthError.errorDescription),
                        debugCode = "DEVICE_CODE_OAUTH_${oauthError.error}",
                    )
                }

                JsonHelper.parseJson<BaiduPanDeviceCodeResponse>(payload)
                    ?: throw ScanApiException("获取二维码失败，请稍后重试", "DEVICE_CODE_PAYLOAD_INVALID")
            }
        }

    private suspend fun fetchTokenByDeviceCode(deviceCode: String): Result<OauthTokenOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.baiduPanService.oauthToken(
                        baseUrl = Api.BAIDU_ACCOUNT_API,
                        grantType = "device_token",
                        code = deviceCode,
                        refreshToken = null,
                        clientId = BaiduPanOpenApiConfig.clientId,
                        clientSecret = BaiduPanOpenApiConfig.clientSecret,
                    )
                val payload =
                    response.body()?.string()
                        ?: response.errorBody()?.string()
                        ?: ""

                val oauthError = JsonHelper.parseJson<BaiduPanOAuthError>(payload)
                if (oauthError != null && oauthError.error.isNullOrBlank().not()) {
                    val error = oauthError.error?.trim().orEmpty()
                    val message = formatOauthMessage(error, oauthError.errorDescription)
                    return@runCatching when (error) {
                        "authorization_pending" -> OauthTokenOutcome.Pending(message)
                        "slow_down" -> OauthTokenOutcome.SlowDown(message)
                        "access_denied" -> OauthTokenOutcome.TerminalError(message)
                        "expired_token" -> OauthTokenOutcome.TerminalError(message)
                        "invalid_client" -> OauthTokenOutcome.TerminalError(message)
                        "invalid_grant" -> OauthTokenOutcome.TerminalError(message)
                        else -> OauthTokenOutcome.TerminalError(message)
                    }
                }

                val token = JsonHelper.parseJson<BaiduPanTokenResponse>(payload)
                if (token == null || token.accessToken.isBlank() || token.refreshToken.isBlank() || token.expiresIn <= 0) {
                    throw ScanApiException("授权返回数据异常，请重试", "TOKEN_PAYLOAD_INVALID")
                }

                OauthTokenOutcome.Success(token)
            }
        }

    private suspend fun fetchUinfo(accessToken: String): Result<BaiduPanUinfoResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.baiduPanService.xpanUinfo(
                        baseUrl = Api.BAIDU_PAN,
                        method = "uinfo",
                        accessToken = accessToken,
                        vipVersion = "v2",
                    )

                if (response.errno != 0) {
                    val detail = response.errmsg?.takeIf { it.isNotBlank() }
                    val message = if (detail.isNullOrBlank()) "获取账号信息失败" else "获取账号信息失败：$detail"
                    throw ScanApiException(message, "UINFO_ERRNO_${response.errno}")
                }
                response
            }
        }

    private fun formatOauthMessage(
        error: String,
        description: String?,
    ): String {
        val hint =
            when (error) {
                "authorization_pending" -> "等待用户扫码确认"
                "slow_down" -> "请求过于频繁，请稍后重试"
                "access_denied" -> "用户拒绝授权"
                "expired_token" -> "二维码已过期，请重新获取"
                "invalid_client" -> "百度网盘开放平台密钥无效"
                "invalid_grant" -> "授权已失效，请重新扫码授权"
                else -> "百度网盘授权失败"
            }

        val detail = description?.takeIf { it.isNotBlank() }
        return if (detail.isNullOrEmpty()) {
            "$hint（error=$error）"
        } else {
            "$hint：$detail（error=$error）"
        }
    }

    private sealed class OauthTokenOutcome {
        data class Success(
            val token: BaiduPanTokenResponse,
        ) : OauthTokenOutcome()

        data class Pending(
            val message: String,
        ) : OauthTokenOutcome()

        data class SlowDown(
            val message: String,
        ) : OauthTokenOutcome()

        data class TerminalError(
            val message: String,
        ) : OauthTokenOutcome()
    }

    private companion object {
        private const val DEFAULT_SCOPE = "basic,netdisk"
        private const val SLOW_DOWN_EXTRA_DELAY_MS = 5_000L
    }
}
