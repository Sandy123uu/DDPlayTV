package com.xyoye.storage_component.ui.dialog.scanlogin

sealed class ScanUiState<out QrData, out SuccessPayload> {
    data class Loading(
        val step: ScanStep,
        val message: String,
    ) : ScanUiState<Nothing, Nothing>()

    data class QrReady<QrData>(
        val qrData: QrData,
        val hintMessage: String,
    ) : ScanUiState<QrData, Nothing>()

    data class Progress(
        val message: String,
    ) : ScanUiState<Nothing, Nothing>()

    data class Success<SuccessPayload>(
        val payload: SuccessPayload,
        val message: String,
    ) : ScanUiState<Nothing, SuccessPayload>()

    data class Failure(
        val failure: ScanFailure,
    ) : ScanUiState<Nothing, Nothing>()
}
