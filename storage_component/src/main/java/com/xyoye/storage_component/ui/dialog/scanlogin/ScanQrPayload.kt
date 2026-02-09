package com.xyoye.storage_component.ui.dialog.scanlogin

data class ScanQrPayload<Session, QrData>(
    val session: Session,
    val qrData: QrData,
    val hintMessage: String,
    val pollIntervalMs: Long,
)
