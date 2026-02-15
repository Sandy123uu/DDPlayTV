package com.xyoye.common_component.log.http.model

data class ErrorResponse(
    val success: Boolean = false,
    val errorCode: Int,
    val errorMessage: String,
    val details: Map<String, Any?>? = null,
)
