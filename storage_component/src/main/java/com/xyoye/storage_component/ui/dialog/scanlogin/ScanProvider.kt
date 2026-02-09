package com.xyoye.storage_component.ui.dialog.scanlogin

interface ScanProvider<Session, QrData, SuccessPayload> {
    val providerId: String

    suspend fun fetchQrCode(): Result<ScanQrPayload<Session, QrData>>

    suspend fun poll(session: Session): Result<ScanPollResult<SuccessPayload>>
}
