package com.xyoye.storage_component.ui.dialog

import android.app.Activity
import android.graphics.Bitmap
import androidx.core.view.isVisible
import com.xyoye.common_component.bilibili.BilibiliApiPreferencesStore
import com.xyoye.common_component.bilibili.BilibiliApiType
import com.xyoye.common_component.bilibili.BilibiliPlaybackPreferencesStore
import com.xyoye.common_component.bilibili.repository.BilibiliRepository
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.weight.dialog.BaseBottomDialog
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.DialogBilibiliLoginBinding
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanLoginCoordinator
import com.xyoye.storage_component.ui.dialog.scanlogin.ScanUiState
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.BilibiliScanProvider
import com.xyoye.storage_component.ui.dialog.scanlogin.provider.BilibiliScanSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BilibiliLoginDialog(
    private val activity: Activity,
    private val library: MediaLibraryEntity,
    private val apiType: BilibiliApiType = BilibiliApiPreferencesStore.read(library).apiType,
    private val onLoginSuccess: () -> Unit,
    private val onDismiss: (() -> Unit)? = null,
) : BaseBottomDialog<DialogBilibiliLoginBinding>(activity) {
    private lateinit var binding: DialogBilibiliLoginBinding

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var coordinator: ScanLoginCoordinator<BilibiliScanSession, Bitmap, Unit>? = null

    private val storageKey = BilibiliPlaybackPreferencesStore.storageKey(library)
    private val repository = BilibiliRepository(storageKey)

    override fun getChildLayoutId(): Int = R.layout.dialog_bilibili_login

    override fun initView(binding: DialogBilibiliLoginBinding) {
        this.binding = binding

        setTitle(if (apiType == BilibiliApiType.TV) "Bilibili TV 扫码登录" else "Bilibili 扫码登录")
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
                provider =
                    BilibiliScanProvider(
                        activity = activity,
                        repository = repository,
                        apiType = apiType,
                    ),
            ) { state ->
                renderState(state)
            }
        coordinator?.start()
    }

    private fun renderState(state: ScanUiState<Bitmap, Unit>) {
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
                    onLoginSuccess.invoke()
                }
            }

            is ScanUiState.Failure -> {
                binding.loadingPb.isVisible = false
                binding.statusTv.text = state.failure.userMessage
                LogFacade.e(
                    LogModule.STORAGE,
                    LOG_TAG,
                    "bilibili scan login failed",
                    mapOf(
                        "storageKey" to storageKey,
                        "apiType" to apiType.name,
                        "category" to state.failure.category.name,
                        "debugCode" to state.failure.debugCode,
                        "retryable" to state.failure.retryable.toString(),
                    ),
                )
            }
        }
    }

    private companion object {
        private const val LOG_TAG: String = "bilibili_scan_login"
    }
}
