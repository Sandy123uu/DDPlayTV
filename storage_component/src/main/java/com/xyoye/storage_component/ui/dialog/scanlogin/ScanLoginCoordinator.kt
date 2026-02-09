package com.xyoye.storage_component.ui.dialog.scanlogin

import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive

class ScanLoginCoordinator<Session, QrData, SuccessPayload>(
    private val scope: CoroutineScope,
    private val provider: ScanProvider<Session, QrData, SuccessPayload>,
    private val onState: (ScanUiState<QrData, SuccessPayload>) -> Unit,
) {
    private var runningJob: Job? = null

    fun start() {
        cancel()
        runningJob =
            scope.launch {
                runFlow()
            }
    }

    fun cancel() {
        runningJob?.cancel()
        runningJob = null
    }

    private suspend fun runFlow() {
        onState.invoke(
            ScanUiState.Loading(
                step = ScanStep.FETCH_QR,
                message = "正在获取二维码…",
            ),
        )

        LogFacade.d(
            LogModule.STORAGE,
            LOG_TAG,
            "scan flow start",
            mapOf(
                "provider" to provider.providerId,
            ),
        )

        val qrPayload =
            provider
                .fetchQrCode()
                .getOrElse { throwable ->
                    emitFailure(
                        step = ScanStep.FETCH_QR,
                        throwable = throwable,
                    )
                    return
                }

        onState.invoke(
            ScanUiState.QrReady(
                qrData = qrPayload.qrData,
                hintMessage = qrPayload.hintMessage,
            ),
        )
        onState.invoke(ScanUiState.Progress(qrPayload.hintMessage))

        while (coroutineContext.isActive) {
            val pollResult =
                provider
                    .poll(qrPayload.session)
                    .getOrElse { throwable ->
                        emitFailure(
                            step = ScanStep.POLL_STATUS,
                            throwable = throwable,
                        )
                        return
                    }

            when (pollResult) {
                is ScanPollResult.Waiting -> {
                    if (pollResult.message.isNotBlank()) {
                        onState.invoke(ScanUiState.Progress(pollResult.message))
                    }
                    val interval = (pollResult.nextIntervalMs ?: qrPayload.pollIntervalMs).coerceAtLeast(500L)
                    delay(interval)
                }

                is ScanPollResult.Success -> {
                    onState.invoke(
                        ScanUiState.Success(
                            payload = pollResult.payload,
                            message = pollResult.successMessage,
                        ),
                    )
                    LogFacade.i(
                        LogModule.STORAGE,
                        LOG_TAG,
                        "scan flow success",
                        mapOf(
                            "provider" to provider.providerId,
                        ),
                    )
                    return
                }

                is ScanPollResult.Failure -> {
                    onState.invoke(ScanUiState.Failure(pollResult.failure))
                    LogFacade.e(
                        LogModule.STORAGE,
                        LOG_TAG,
                        "scan flow failed",
                        mapOf(
                            "provider" to provider.providerId,
                            "category" to pollResult.failure.category.name,
                            "debugCode" to pollResult.failure.debugCode,
                            "retryable" to pollResult.failure.retryable.toString(),
                        ),
                    )
                    return
                }
            }
        }
    }

    private fun emitFailure(
        step: ScanStep,
        throwable: Throwable,
    ) {
        val mapped =
            ScanErrorMapper.map(
                providerId = provider.providerId,
                step = step,
                throwable = throwable,
            )
        onState.invoke(
            ScanUiState.Failure(mapped),
        )
        LogFacade.e(
            LogModule.STORAGE,
            LOG_TAG,
            "scan flow exception",
            mapOf(
                "provider" to provider.providerId,
                "step" to step.name,
                "category" to mapped.category.name,
                "debugCode" to mapped.debugCode,
                "retryable" to mapped.retryable.toString(),
                "exception" to throwable::class.java.simpleName,
            ),
            throwable,
        )
    }
}

private const val LOG_TAG: String = "scan_login"
