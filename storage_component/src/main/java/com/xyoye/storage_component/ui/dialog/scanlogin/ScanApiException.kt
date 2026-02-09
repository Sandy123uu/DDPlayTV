package com.xyoye.storage_component.ui.dialog.scanlogin

class ScanApiException(
    val userMessage: String,
    val debugCode: String,
    val retryable: Boolean = true,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause)
