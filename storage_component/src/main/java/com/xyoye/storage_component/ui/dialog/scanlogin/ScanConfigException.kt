package com.xyoye.storage_component.ui.dialog.scanlogin

class ScanConfigException(
    val userMessage: String,
    val debugCode: String,
) : RuntimeException(userMessage)
