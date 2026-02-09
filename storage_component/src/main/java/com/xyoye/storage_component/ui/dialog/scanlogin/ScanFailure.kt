package com.xyoye.storage_component.ui.dialog.scanlogin

data class ScanFailure(
    val userMessage: String,
    val category: ScanFailureCategory,
    val debugCode: String,
    val retryable: Boolean,
)
