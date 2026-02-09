package com.xyoye.storage_component.ui.dialog

import android.app.Activity
import android.graphics.Bitmap
import androidx.core.view.isVisible
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.data_component.data.baidupan.oauth.BaiduPanTokenResponse
import com.xyoye.data_component.data.baidupan.xpan.BaiduPanUinfoResponse
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.DialogBaiduPanLoginBinding
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanLoginCoordinator
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanUiState
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.BaiduPanScanLoginResult
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.BaiduPanScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.BaiduPanScanSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BaiduPanLoginDialog(
    private val activity: Activity,
    private val onLoginSuccess: (token: BaiduPanTokenResponse, uinfo: BaiduPanUinfoResponse) -> Unit,
    private val onDismiss: (() -> Unit)? = null,
) : BaseBottomDialog<DialogBaiduPanLoginBinding>(activity) {
    private lateinit var binding: DialogBaiduPanLoginBinding

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var coordinator: ScanLoginCoordinator<BaiduPanScanSession, Bitmap, BaiduPanScanLoginResult>? = null

    override fun getChildLayoutId(): Int = R.layout.dialog_baidu_pan_login

    override fun initView(binding: DialogBaiduPanLoginBinding) {
        this.binding = binding

        setTitle("百度网盘扫码授权")
        setPositiveText("重试")
        setNegativeText("取消")
        setDialogCancelable(touchCancel = false, backPressedCancel = true)

        setPositiveListener { startLoginFlow() }
        setNegativeListener { dismiss() }

        setOnDismissListener {
            coordinator?.cancel()
            scope.cancel()
            onDismiss?.invoke()
        }

        startLoginFlow()
    }

    private fun startLoginFlow() {
        coordinator?.cancel()
        coordinator =
            ScanLoginCoordinator(
                scope = scope,
                provider = BaiduPanScanProvider(activity),
            ) { state ->
                renderState(state)
            }
        coordinator?.start()
    }

    private fun renderState(state: ScanUiState<Bitmap, BaiduPanScanLoginResult>) {
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
                    onLoginSuccess.invoke(state.payload.token, state.payload.uinfo)
                }
            }

            is ScanUiState.Failure -> {
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.failure.userMessage
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "baidupan scan login failed",
                    mapOf(
                        "category" to state.failure.category.name,
                        "debugCode" to state.failure.debugCode,
                        "retryable" to state.failure.retryable.toString(),
                    ),
                )
            }
        }
    }

    private companion object {
        private const val LOG_TAG: String = "baidupan_scan_login"
    }
}
