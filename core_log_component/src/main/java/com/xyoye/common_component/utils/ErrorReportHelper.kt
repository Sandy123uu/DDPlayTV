package com.xyoye.common_component.utils

import android.util.Log
import com.xyoye.common_component.log.BuglyReporter
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.core_log_component.BuildConfig
import kotlin.coroutines.cancellation.CancellationException

/**
 * 错误上报工具类
 * 统一处理异常上报：脱敏 + 过滤 CancellationException + 委托给 [BuglyReporter]。
 */
object ErrorReportHelper {
    private const val LOG_TAG = "ErrorReportHelper"
    private const val BUGLY_VALUE_LIMIT = 200
    private const val BUGLY_KEY_TAG = "err.tag"
    private const val BUGLY_KEY_EXTRA = "err.extra"
    private const val BUGLY_KEY_MESSAGE = "err.message"

    /**
     * 上报捕获的异常
     *
     * @param throwable 捕获的异常
     * @param tag 错误标签，用于分类
     * @param extraInfo 额外信息
     */
    fun postCatchedException(
        throwable: Throwable,
        tag: String = "",
        extraInfo: String = ""
    ) {
        try {
            val safeTag = SensitiveDataSanitizer.sanitizeFreeText(tag).trim()
            val safeExtraInfo = SensitiveDataSanitizer.sanitizeFreeText(extraInfo).trim()
            // 过滤掉 CancellationException，这是协程正常取消的标志，不应当作错误上报
            if (throwable is CancellationException) {
                if (BuildConfig.DEBUG) {
                    LogFacade.w(
                        module = LogModule.CORE,
                        tag = LOG_TAG,
                        message = "CancellationException ignored",
                        context =
                            buildMap {
                                if (safeTag.isNotBlank()) put("tag", safeTag.take(BUGLY_VALUE_LIMIT))
                                if (safeExtraInfo.isNotBlank()) put("extra", safeExtraInfo.take(BUGLY_VALUE_LIMIT))
                            },
                        throwable = throwable
                    )
                }
                return
            }

            if (safeTag.isNotBlank()) {
                BuglyReporter.putUserData(BUGLY_KEY_TAG, safeTag.take(BUGLY_VALUE_LIMIT))
            }
            if (safeExtraInfo.isNotBlank()) {
                BuglyReporter.putUserData(BUGLY_KEY_EXTRA, safeExtraInfo.take(BUGLY_VALUE_LIMIT))
            }
            BuglyReporter.postCatchedException(throwable)

            if (BuildConfig.DEBUG) {
                LogFacade.e(
                    module = LogModule.CORE,
                    tag = LOG_TAG,
                    message = "Exception reported",
                    context =
                        buildMap {
                            if (safeTag.isNotBlank()) put("tag", safeTag.take(BUGLY_VALUE_LIMIT))
                            if (safeExtraInfo.isNotBlank()) put("extra", safeExtraInfo.take(BUGLY_VALUE_LIMIT))
                        },
                    throwable = throwable
                )
            }
        } catch (e: Exception) {
            // 防止错误上报本身出现异常
            Log.w(LOG_TAG, "postCatchedException failed", e)
        }
    }

    /**
     * 上报自定义异常
     *
     * @param message 错误信息
     * @param tag 错误标签
     * @param cause 原因异常（可选）
     */
    fun postException(
        message: String,
        tag: String = "",
        cause: Throwable? = null
    ) {
        try {
            val safeTag = SensitiveDataSanitizer.sanitizeFreeText(tag).trim()
            val safeMessage = SensitiveDataSanitizer.sanitizeFreeText(message).trim()
            val exception =
                if (cause != null) {
                    RuntimeException("[$safeTag] $safeMessage", cause)
                } else {
                    RuntimeException("[$safeTag] $safeMessage")
                }

            if (safeTag.isNotBlank()) {
                BuglyReporter.putUserData(BUGLY_KEY_TAG, safeTag.take(BUGLY_VALUE_LIMIT))
            }
            if (safeMessage.isNotBlank()) {
                BuglyReporter.putUserData(BUGLY_KEY_MESSAGE, safeMessage.take(BUGLY_VALUE_LIMIT))
            }
            BuglyReporter.postCatchedException(exception)

            if (BuildConfig.DEBUG) {
                LogFacade.e(
                    module = LogModule.CORE,
                    tag = LOG_TAG,
                    message = "Custom exception reported",
                    context =
                        buildMap {
                            if (safeTag.isNotBlank()) put("tag", safeTag.take(BUGLY_VALUE_LIMIT))
                            if (safeMessage.isNotBlank()) put("message", safeMessage.take(BUGLY_VALUE_LIMIT))
                        },
                    throwable = exception
                )
            }
        } catch (e: Exception) {
            // 防止错误上报本身出现异常
            Log.w(LOG_TAG, "postException failed", e)
        }
    }

    /**
     * 带有完整上下文信息的异常上报
     *
     * @param throwable 异常对象
     * @param className 发生异常的类名
     * @param methodName 发生异常的方法名
     * @param extraInfo 额外的上下文信息
     */
    fun postCatchedExceptionWithContext(
        throwable: Throwable,
        className: String,
        methodName: String,
        extraInfo: String = ""
    ) {
        val safeExtraInfo = SensitiveDataSanitizer.sanitizeFreeText(extraInfo)
        // 过滤掉 CancellationException，这是协程正常取消的标志，不应当作错误上报
        if (throwable is CancellationException) {
            if (BuildConfig.DEBUG) {
                LogFacade.w(
                    module = LogModule.CORE,
                    tag = LOG_TAG,
                    message = "CancellationException ignored",
                    context =
                        buildMap {
                            put("class", className.take(BUGLY_VALUE_LIMIT))
                            put("method", methodName.take(BUGLY_VALUE_LIMIT))
                            if (safeExtraInfo.isNotBlank()) put("extra", safeExtraInfo.take(BUGLY_VALUE_LIMIT))
                        },
                    throwable = throwable
                )
            }
            return
        }

        val contextInfo = "Class: $className, Method: $methodName"
        val fullInfo =
            if (safeExtraInfo.isNotEmpty()) {
                "$contextInfo, Extra: $safeExtraInfo"
            } else {
                contextInfo
            }

        postCatchedException(throwable, "Context", fullInfo)
    }

    /**
     * 上报HTTP 403 Forbidden错误，包含详细的认证诊断信息
     *
     * @param throwable HTTP异常（或其它可追踪的异常对象）
     * @param className 发生异常的类名
     * @param methodName 发生异常的方法名
     * @param extraInfo 额外的上下文信息
     * @param authDiagnosis 认证/签名等诊断信息（由调用方提供，避免 log core 反向依赖网络/业务模块）
     */
    fun post403Exception(
        throwable: Throwable,
        className: String,
        methodName: String,
        extraInfo: String = "",
        authDiagnosis: String = ""
    ) {
        val fullExtraInfo =
            buildString {
                append(extraInfo)
                if (extraInfo.isNotEmpty() && authDiagnosis.isNotBlank()) {
                    append("\n\n")
                }
                if (authDiagnosis.isNotBlank()) {
                    append(authDiagnosis)
                }
            }

        postCatchedExceptionWithContext(
            throwable,
            className,
            methodName,
            fullExtraInfo
        )
    }
}
