package com.xyoye.storage_component.ui.dialog.scanlogin.provider

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import com.xyoye.common_component.bilibili.BilibiliApiType
import com.xyoye.common_component.bilibili.login.BilibiliLoginPollResult
import com.xyoye.common_component.bilibili.repository.BilibiliRepository
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.utils.QrCodeHelper
import com.xyoye.common_component.utils.dp2px
import com.xyoye.storage_component.R
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanApiException
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailure
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanFailureCategory
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanPollResult
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanQrPayload

data class BilibiliScanSession(
    val qrcodeKey: String,
    val startElapsedMs: Long,
)

class BilibiliScanProvider(
    private val activity: Activity,
    private val repository: BilibiliRepository,
    private val apiType: BilibiliApiType,
) : ScanProvider<BilibiliScanSession, Bitmap, Unit> {
    override val providerId: String = "bilibili"

    override suspend fun fetchQrCode(): Result<ScanQrPayload<BilibiliScanSession, Bitmap>> =
        runCatching {
            val qr = repository.loginQrCodeGenerate(apiType).getOrElse { throw it }
            if (qr.url.isBlank() || qr.qrcodeKey.isBlank()) {
                throw ScanApiException("获取二维码失败，请稍后重试", "QR_DATA_INVALID")
            }

            val bitmap =
                QrCodeHelper.createQrCode(
                    context = activity,
                    content = qr.url,
                    sizePx = dp2px(220),
                    logoResId = R.mipmap.ic_logo,
                    bitmapColor =
                        com.xyoye.core_ui_component.R.color.text_black
                            .toResColor(activity),
                    errorContext = "生成 Bilibili 登录二维码失败",
                ) ?: throw ScanApiException("生成二维码失败，请稍后重试", "QR_BITMAP_CREATE_FAILED")

            ScanQrPayload(
                session =
                    BilibiliScanSession(
                        qrcodeKey = qr.qrcodeKey,
                        startElapsedMs = SystemClock.elapsedRealtime(),
                    ),
                qrData = bitmap,
                hintMessage = "请使用哔哩哔哩 App 扫码登录",
                pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
            )
        }

    override suspend fun poll(session: BilibiliScanSession): Result<ScanPollResult<Unit>> =
        runCatching {
            if (SystemClock.elapsedRealtime() - session.startElapsedMs > QR_EXPIRE_TIMEOUT_MS) {
                return@runCatching ScanPollResult.Failure(
                    ScanFailure(
                        userMessage = "二维码已过期，请点击重试",
                        category = ScanFailureCategory.API,
                        debugCode = "QR_EXPIRED",
                        retryable = true,
                    ),
                )
            }

            when (val result = repository.loginQrCodePoll(session.qrcodeKey, apiType).getOrElse { throw it }) {
                BilibiliLoginPollResult.WaitingScan -> ScanPollResult.Waiting("等待扫码…")
                BilibiliLoginPollResult.WaitingConfirm -> ScanPollResult.Waiting("已扫码，请在 App 上确认登录")
                BilibiliLoginPollResult.Expired ->
                    ScanPollResult.Failure(
                        ScanFailure(
                            userMessage = "二维码已失效，请点击重试",
                            category = ScanFailureCategory.API,
                            debugCode = "QR_EXPIRED_SERVER",
                            retryable = true,
                        ),
                    )

                BilibiliLoginPollResult.Success -> ScanPollResult.Success(Unit, "登录成功")
                is BilibiliLoginPollResult.Error ->
                    ScanPollResult.Waiting(
                        message = result.message.ifBlank { "登录中…" },
                    )
            }
        }

    private companion object {
        private const val DEFAULT_POLL_INTERVAL_MS: Long = 1_500L
        private const val QR_EXPIRE_TIMEOUT_MS: Long = 2 * 60 * 1_000L
    }
}
