package com.xyoye.storage_component.ui.dialog.scanlogin.provider

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.network.repository.Cloud115Repository
import com.xyoye.common_component.storage.cloud115.net.Cloud115Headers
import com.xyoye.common_component.utils.QrCodeHelper
import com.xyoye.common_component.utils.dp2px
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeSession
import com.xyoye.storage_component.R
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanApiException
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailure
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailureCategory
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanPollResult
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanQrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Cloud115ScanSession(
    val uid: String,
    val time: Long,
    val sign: String,
)

data class Cloud115ScanLoginResult(
    val cookieHeader: String,
    val userId: String,
    val userName: String?,
    val avatarUrl: String?,
    val loginApp: String,
)

class Cloud115ScanProvider(
    private val activity: Activity,
    private val storageKey: String,
    private val loginAppProvider: () -> String,
) : ScanProvider<Cloud115ScanSession, Bitmap, Cloud115ScanLoginResult> {
    override val providerId: String = "cloud115"

    private val repository = Cloud115Repository(storageKey)

    override suspend fun fetchQrCode(): Result<ScanQrPayload<Cloud115ScanSession, Bitmap>> =
        runCatching {
            val token =
                repository
                    .qrcodeToken()
                    .getOrElse { throw it }
            val session = token.data ?: throw ScanApiException("获取二维码失败：响应数据为空", "TOKEN_EMPTY_DATA")

            val uid = session.uid?.trim().orEmpty()
            val sign = session.sign?.trim().orEmpty()
            val time = session.time
            if (uid.isBlank() || sign.isBlank() || time <= 0L) {
                throw ScanApiException("获取二维码失败：响应数据异常", "TOKEN_INVALID_DATA")
            }

            val bitmap = buildQrCodeBitmap(session)
            ScanQrPayload(
                session =
                    Cloud115ScanSession(
                        uid = uid,
                        time = time,
                        sign = sign,
                    ),
                qrData = bitmap,
                hintMessage = "请使用 115 App 扫码确认",
                pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
            )
        }

    override suspend fun poll(session: Cloud115ScanSession): Result<ScanPollResult<Cloud115ScanLoginResult>> =
        runCatching {
            val statusResp =
                repository
                    .qrcodeStatus(
                        uid = session.uid,
                        time = session.time,
                        sign = session.sign,
                    ).getOrElse { throw it }

            when (statusResp.data?.status) {
                0 -> ScanPollResult.Waiting("等待扫码…")
                1 -> ScanPollResult.Waiting("已扫码，请在 115 App 确认")
                2 -> {
                    val loginApp = loginAppProvider.invoke().trim().ifEmpty { DEFAULT_LOGIN_APP }
                    val loginResp =
                        repository
                            .qrcodeLogin(uid = session.uid, app = loginApp, persistAuth = false)
                            .getOrElse { throw it }

                    val cookieHeader = Cloud115Headers.buildCookieHeader(loginResp.data?.cookie).trim()
                    val userId = loginResp.data?.userId?.toString()?.trim().orEmpty()
                    if (cookieHeader.isBlank() || userId.isBlank()) {
                        throw ScanApiException("登录返回数据异常，请重试", "LOGIN_INVALID_DATA")
                    }

                    val avatarUrl =
                        loginResp.data?.face?.faceLarge
                            ?: loginResp.data?.face?.faceMedium
                            ?: loginResp.data?.face?.faceSmall

                    ScanPollResult.Success(
                        payload =
                            Cloud115ScanLoginResult(
                                cookieHeader = cookieHeader,
                                userId = userId,
                                userName = loginResp.data?.userName,
                                avatarUrl = avatarUrl,
                                loginApp = loginApp,
                            ),
                        successMessage = "授权成功",
                    )
                }

                -1 ->
                    ScanPollResult.Failure(
                        ScanFailure(
                            userMessage = "二维码已过期，请刷新",
                            category = ScanFailureCategory.API,
                            debugCode = "QR_EXPIRED",
                            retryable = true,
                        ),
                    )

                -2 ->
                    ScanPollResult.Failure(
                        ScanFailure(
                            userMessage = "已取消，请重试",
                            category = ScanFailureCategory.API,
                            debugCode = "QR_CANCELLED",
                            retryable = true,
                        ),
                    )

                else -> {
                    val message = statusResp.data?.msg?.trim().orEmpty()
                    ScanPollResult.Waiting(
                        message = if (message.isBlank()) "状态异常，请重试" else message,
                    )
                }
            }
        }

    private suspend fun buildQrCodeBitmap(session: Cloud115QRCodeSession): Bitmap =
        withContext(Dispatchers.IO) {
            val content = session.qrcode?.trim().orEmpty()
            if (content.isNotBlank()) {
                return@withContext QrCodeHelper
                    .createQrCode(
                        context = activity,
                        content = content,
                        sizePx = dp2px(220),
                        logoResId = R.mipmap.ic_logo,
                        bitmapColor =
                            com.xyoye.core_ui_component.R.color.text_black
                                .toResColor(activity),
                        errorContext = "生成 115 Cloud 授权二维码失败",
                    ) ?: throw ScanApiException("生成二维码失败，请稍后重试", "QR_BITMAP_CREATE_FAILED")
            }

            val uid = session.uid?.trim().orEmpty()
            if (uid.isBlank()) {
                throw ScanApiException("获取二维码失败：uid 为空", "QR_UID_EMPTY")
            }

            val body =
                repository
                    .qrcodeImage(uid)
                    .getOrElse { throw it }

            body.use {
                BitmapFactory.decodeStream(it.byteStream())
            } ?: throw ScanApiException("二维码图片解码失败，请重试", "QR_IMAGE_DECODE_FAILED")
        }

    private companion object {
        private const val DEFAULT_LOGIN_APP: String = "tv"
        private const val DEFAULT_POLL_INTERVAL_MS: Long = 2_000L
    }
}
