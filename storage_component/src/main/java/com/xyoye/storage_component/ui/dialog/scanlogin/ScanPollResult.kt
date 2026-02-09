package com.xyoye.storage_component.ui.dialog.scanlogin

sealed class ScanPollResult<out SuccessPayload> {
    data class Waiting(
        val message: String,
        val nextIntervalMs: Long? = null,
    ) : ScanPollResult<Nothing>()

    data class Success<SuccessPayload>(
        val payload: SuccessPayload,
        val successMessage: String,
    ) : ScanPollResult<SuccessPayload>()

    data class Failure(
        val failure: ScanFailure,
    ) : ScanPollResult<Nothing>()
}
