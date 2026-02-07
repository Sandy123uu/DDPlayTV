package com.xyoye.common_component.extension

import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.weight.ToastCenter

fun Result<*>.reportAndToastOnFailure(
    unknownErrorMessage: String,
    className: String,
    methodName: String,
    reportMessage: String = "",
    toastMessage: String? = null,
    reportParams: Map<String, Any?> = emptyMap()
): Boolean {
    if (!isFailure) return false

    val exception = exceptionOrNull()
    ErrorReportHelper.postCatchedExceptionWithContext(
        throwable = exception ?: RuntimeException(unknownErrorMessage),
        className = className,
        methodName = methodName,
        params = reportParams,
        message = reportMessage,
    )

    if (toastMessage != null) {
        ToastCenter.showError(toastMessage)
    } else {
        exception?.message?.toastError()
    }

    return true
}
