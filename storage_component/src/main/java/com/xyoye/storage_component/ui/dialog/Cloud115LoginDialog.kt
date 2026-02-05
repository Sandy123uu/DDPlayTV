package com.xyoye.storage_component.ui.dialog

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.network.RetrofitManager
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.storage.cloud115.net.Cloud115Headers
import com.xyoye.common_component.utils.QrCodeHelper
import com.xyoye.common_component.utils.dp2px
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.common_component.weight.dialog.CommonEditDialog
import com.xyoye.data_component.bean.EditBean
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeLoginResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeSession
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeStatusResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeTokenResp
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.DialogCloud115LoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Cloud115LoginDialog(
    private val activity: Activity,
    private val initialLoginApp: String = DEFAULT_LOGIN_APP,
    private val onLoginSuccess: (result: LoginResult) -> Unit,
    private val onDismiss: (() -> Unit)? = null
) : BaseBottomDialog<DialogCloud115LoginBinding>(activity) {
    data class LoginResult(
        val cookieHeader: String,
        val userId: String,
        val userName: String?,
        val avatarUrl: String?,
        val loginApp: String
    )

    private lateinit var binding: DialogCloud115LoginBinding

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollingJob: Job? = null
    private var selectedApp: String = initialLoginApp.trim().ifEmpty { DEFAULT_LOGIN_APP }

    override fun getChildLayoutId(): Int = R.layout.dialog_cloud115_login

    override fun initView(binding: DialogCloud115LoginBinding) {
        this.binding = binding

        setTitle("115 Cloud 扫码授权")
        setPositiveText("刷新二维码")
        setNegativeText("取消")
        setDialogCancelable(touchCancel = false, backPressedCancel = true)

        setPositiveListener { startLoginFlow() }
        setNegativeListener { dismiss() }

        setOnDismissListener {
            pollingJob?.cancel()
            scope.cancel()
            onDismiss?.invoke()
        }

        binding.loginAppValueTv.text = selectedApp
        binding.loginAppSelectTv.setOnClickListener { showLoginAppDialog() }

        startLoginFlow()
    }

    private fun showLoginAppDialog() {
        val actions =
            listOf(
                SheetActionBean(
                    actionId = "web",
                    actionName = "web",
                    describe = "Web 端",
                ),
                SheetActionBean(
                    actionId = "android",
                    actionName = "android",
                    describe = "Android 端",
                ),
                SheetActionBean(
                    actionId = "ios",
                    actionName = "ios",
                    describe = "iOS 端",
                ),
                SheetActionBean(
                    actionId = "tv",
                    actionName = "tv",
                    describe = "TV 端（默认）",
                ),
                SheetActionBean(
                    actionId = "wechatmini",
                    actionName = "wechatmini",
                    describe = "微信小程序端",
                ),
                SheetActionBean(
                    actionId = "alipaymini",
                    actionName = "alipaymini",
                    describe = "支付宝小程序端",
                ),
                SheetActionBean(
                    actionId = "qandroid",
                    actionName = "qandroid",
                    describe = "安卓端（qandroid）",
                ),
                SheetActionBean(
                    actionId = ACTION_CUSTOM,
                    actionName = "自定义…",
                    describe = "填写其他 app 值",
                ),
            )

        BottomActionDialog(activity, actions, "设备来源（app）") { action ->
            val selected = action.actionId as? String ?: return@BottomActionDialog false
            if (selected == ACTION_CUSTOM) {
                showCustomLoginAppDialog()
                return@BottomActionDialog true
            }

            updateSelectedApp(selected)
            true
        }.show()
    }

    private fun showCustomLoginAppDialog() {
        val host = activity as? AppCompatActivity
        if (host == null) {
            ToastCenter.showWarning("当前页面不支持自定义输入")
            return
        }

        CommonEditDialog(
            activity = host,
            editBean =
                EditBean(
                    title = "自定义设备来源（app）",
                    emptyWarningMsg = "请填写设备来源（app）",
                    hint = "例如：tv",
                    defaultText = selectedApp,
                    inputTips = "建议使用字母/数字/下划线组合（例如 tv、wechatmini）",
                    canInputEmpty = false,
                ),
            inputOnlyDigit = false,
            checkBlock = { app ->
                val trimmed = app.trim()
                if (trimmed.isBlank()) {
                    ToastCenter.showWarning("请填写设备来源（app）")
                    return@CommonEditDialog false
                }

                if (!Regex("^[a-zA-Z0-9_]+$").matches(trimmed)) {
                    ToastCenter.showWarning("仅支持字母/数字/下划线")
                    return@CommonEditDialog false
                }

                true
            },
            callback = { app ->
                updateSelectedApp(app.trim())
            },
        ).show()
    }

    private fun updateSelectedApp(app: String) {
        val trimmed = app.trim().ifEmpty { DEFAULT_LOGIN_APP }
        selectedApp = trimmed
        binding.loginAppValueTv.text = trimmed
    }

    private fun startLoginFlow() {
        pollingJob?.cancel()
        pollingJob =
            scope.launch {
                binding.loadingPb.isVisible = true
                binding.statusTv.text = "正在获取二维码…"

                val token = fetchQrCodeToken().getOrNull()
                val session = token?.data
                val uid = session?.uid?.trim().orEmpty()
                val time = session?.time ?: 0L
                val sign = session?.sign?.trim().orEmpty()

                if (token == null || session == null || uid.isBlank() || time <= 0L || sign.isBlank()) {
                    binding.loadingPb.isVisible = false
                    binding.statusTv.text = "获取二维码失败，请稍后重试"
                    return@launch
                }

                val qrCodeBitmap =
                    buildQrCodeBitmap(session).getOrNull()
                if (qrCodeBitmap == null) {
                    binding.loadingPb.isVisible = false
                    binding.statusTv.text = "生成二维码失败，请稍后重试"
                    return@launch
                }

                binding.qrCodeIv.setImageBitmap(qrCodeBitmap)
                binding.loadingPb.isVisible = false
                binding.statusTv.text = "请使用 115 App 扫码确认"

                pollUntilDone(uid = uid, time = time, sign = sign)
            }
    }

    private suspend fun pollUntilDone(
        uid: String,
        time: Long,
        sign: String
    ) {
        while (scope.coroutineContext[Job]?.isActive == true) {
            val statusResp = fetchQrCodeStatus(uid = uid, time = time, sign = sign).getOrNull()
            val status = statusResp?.data?.status

            if (statusResp == null) {
                binding.statusTv.text = "获取二维码状态失败，请重试"
                return
            }

            when (status) {
                0 -> binding.statusTv.text = "等待扫码…"
                1 -> binding.statusTv.text = "已扫码，请在 115 App 确认"
                2 -> {
                    binding.loadingPb.isVisible = true
                    binding.statusTv.text = "已确认，正在登录…"

                    val loginApp = selectedApp
                    val login = fetchQrCodeLogin(uid = uid, app = loginApp).getOrNull()
                    if (login == null) {
                        binding.loadingPb.isVisible = false
                        binding.statusTv.text = "登录失败，请重试"
                        return
                    }

                    val cookieHeader = Cloud115Headers.buildCookieHeader(login.data?.cookie).trim()
                    val userId =
                        login.data
                            ?.userId
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    if (cookieHeader.isBlank() || userId.isBlank()) {
                        binding.loadingPb.isVisible = false
                        binding.statusTv.text = "登录返回数据异常，请重试"
                        return
                    }

                    val avatarUrl =
                        login.data?.face?.faceLarge
                            ?: login.data?.face?.faceMedium
                            ?: login.data?.face?.faceSmall

                    binding.loadingPb.isVisible = false
                    binding.statusTv.text = "授权成功"
                    delay(300)
                    dismiss()
                    onLoginSuccess.invoke(
                        LoginResult(
                            cookieHeader = cookieHeader,
                            userId = userId,
                            userName = login.data?.userName,
                            avatarUrl = avatarUrl,
                            loginApp = loginApp,
                        ),
                    )
                    return
                }
                -1 -> {
                    binding.statusTv.text = "二维码已过期，请刷新"
                    return
                }
                -2 -> {
                    binding.statusTv.text = "已取消，请重试"
                    return
                }
                else -> {
                    val message =
                        statusResp.data
                            ?.msg
                            ?.trim()
                            .orEmpty()
                    binding.statusTv.text = if (message.isBlank()) "状态异常，请重试" else message
                }
            }

            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun buildQrCodeBitmap(session: Cloud115QRCodeSession): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = session.qrcode?.trim().orEmpty()
                if (content.isNotBlank()) {
                    val bitmap =
                        QrCodeHelper.createQrCode(
                            context = activity,
                            content = content,
                            sizePx = dp2px(220),
                            logoResId = R.mipmap.ic_logo,
                            bitmapColor =
                                com.xyoye.core_ui_component.R.color.text_black
                                    .toResColor(activity),
                            errorContext = "生成 115 Cloud 授权二维码失败",
                        )
                    return@runCatching bitmap
                        ?: throw IllegalStateException("生成二维码失败")
                }

                val uid = session.uid?.trim().orEmpty()
                if (uid.isBlank()) {
                    throw IllegalStateException("二维码 uid 为空")
                }

                val body =
                    RetrofitManager.cloud115Service.qrcodeImage(
                        baseUrl = Api.CLOUD_115_QRCODE_API,
                        uid = uid,
                    )

                body.use {
                    BitmapFactory.decodeStream(it.byteStream())
                }
                    ?: throw IllegalStateException("二维码图片解码失败")
            }
        }

    private suspend fun fetchQrCodeToken(): Result<Cloud115QRCodeTokenResp> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.cloud115Service.qrcodeToken(
                        baseUrl = Api.CLOUD_115_QRCODE_API,
                    )

                if (!isQrCodeSuccess(response.state, response.code)) {
                    throw IllegalStateException("获取二维码失败（code=${response.code} state=${response.state}）")
                }

                response
            }
        }

    private suspend fun fetchQrCodeStatus(
        uid: String,
        time: Long,
        sign: String
    ): Result<Cloud115QRCodeStatusResp> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.cloud115Service.qrcodeStatus(
                        baseUrl = Api.CLOUD_115_QRCODE_API,
                        uid = uid,
                        time = time,
                        sign = sign,
                        timestamp = System.currentTimeMillis().toString(),
                    )

                if (!isQrCodeSuccess(response.state, response.code)) {
                    throw IllegalStateException("获取二维码状态失败（code=${response.code} state=${response.state}）")
                }

                response
            }
        }

    private suspend fun fetchQrCodeLogin(
        uid: String,
        app: String
    ): Result<Cloud115QRCodeLoginResp> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response =
                    RetrofitManager.cloud115Service.qrcodeLogin(
                        baseUrl = Api.CLOUD_115_PASSPORT_API,
                        app = app,
                        account = uid,
                        appInForm = app,
                    )

                if (!isQrCodeSuccess(response.state, response.code)) {
                    throw IllegalStateException("扫码登录失败（code=${response.code} state=${response.state}）")
                }

                response
            }
        }

    private fun isQrCodeSuccess(
        state: Int,
        code: Int
    ): Boolean {
        if (state == 1) {
            return true
        }
        return code == 0 && state != 0
    }

    private companion object {
        private const val DEFAULT_LOGIN_APP: String = "tv"
        private const val ACTION_CUSTOM: String = "__custom__"
        private const val POLL_INTERVAL_MS: Long = 2_000L
    }
}
