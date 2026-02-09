package com.xyoye.storage_component.ui.dialog

import android.app.Activity
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.common_component.weight.dialog.CommonEditDialog
import com.xyoye.data_component.bean.EditBean
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.DialogCloud115LoginBinding
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanLoginCoordinator
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanUiState
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.Cloud115ScanLoginResult
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.Cloud115ScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.Cloud115ScanSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Cloud115LoginDialog(
    private val activity: Activity,
    private val storageKey: String,
    private val initialLoginApp: String = DEFAULT_LOGIN_APP,
    private val onLoginSuccess: (result: LoginResult) -> Unit,
    private val onDismiss: (() -> Unit)? = null,
) : BaseBottomDialog<DialogCloud115LoginBinding>(activity) {
    data class LoginResult(
        val cookieHeader: String,
        val userId: String,
        val userName: String?,
        val avatarUrl: String?,
        val loginApp: String,
    )

    private lateinit var binding: DialogCloud115LoginBinding

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var selectedApp: String = initialLoginApp.trim().ifEmpty { DEFAULT_LOGIN_APP }
    private var coordinator: ScanLoginCoordinator<Cloud115ScanSession, Bitmap, Cloud115ScanLoginResult>? = null

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
            coordinator?.cancel()
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
        coordinator?.cancel()
        coordinator =
            ScanLoginCoordinator(
                scope = scope,
                provider =
                    Cloud115ScanProvider(
                        activity = activity,
                        storageKey = storageKey,
                        loginAppProvider = { selectedApp },
                    ),
            ) { state ->
                renderState(state)
            }
        coordinator?.start()
    }

    private fun renderState(state: ScanUiState<Bitmap, Cloud115ScanLoginResult>) {
        when (state) {
            is ScanUiState.Loading -> {
                binding.loadingPb.isVisible = true
                binding.statusTv.text = state.message
            }

            is ScanUiState.QrReady -> {
                binding.qrCodeIv.setImageBitmap(state.qrData)
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.hintMessage
            }

            is ScanUiState.Progress -> {
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.message
            }

            is ScanUiState.Success -> {
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.message
                scope.launch {
                    delay(300)
                    dismiss()
                    onLoginSuccess.invoke(
                        LoginResult(
                            cookieHeader = state.payload.cookieHeader,
                            userId = state.payload.userId,
                            userName = state.payload.userName,
                            avatarUrl = state.payload.avatarUrl,
                            loginApp = state.payload.loginApp,
                        ),
                    )
                }
            }

            is ScanUiState.Failure -> {
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.failure.userMessage
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "cloud115 scan login failed",
                    mapOf(
                        "storageKey" to storageKey,
                        "category" to state.failure.category.name,
                        "debugCode" to state.failure.debugCode,
                        "retryable" to state.failure.retryable.toString(),
                    ),
                )
            }
        }
    }

    private companion object {
        private const val DEFAULT_LOGIN_APP: String = "tv"
        private const val ACTION_CUSTOM: String = "__custom__"
        private const val LOG_TAG: String = "cloud115_scan_login"
    }
}
