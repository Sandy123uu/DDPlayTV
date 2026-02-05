package com.xyoye.common_component.utils

import android.util.Log
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer

/**
 * 运行时错误上报门面（供无法直接依赖 :core_log_component 的模块使用）。
 *
 * - 统一输出结构化日志（LogFacade）
 * - 统一委托 ErrorReportHelper 上报（脱敏 + 过滤 CancellationException）
 */
object RuntimeErrorReporter {
    private const val FALLBACK_TAG = "RuntimeErrorReporter"

    fun report(
        throwable: Throwable,
        tag: String,
        message: String,
        context: Map<String, String> = emptyMap(),
        extraInfo: String = ""
    ) {
        val safeTag = tag.trim().ifEmpty { FALLBACK_TAG }
        val safeMessage = message.trim().ifEmpty { "runtime error" }
        val safeContext = safeContext(context)

        runCatching {
            LogFacade.e(
                module = LogModule.CORE,
                tag = safeTag,
                message = safeMessage,
                context = safeContext,
                throwable = throwable,
            )
        }.onFailure { logFailure ->
            Log.e(FALLBACK_TAG, "LogFacade failed", logFailure)
        }

        runCatching {
            val contextInfo = renderContextForBugly(safeContext)
            val renderedExtra =
                buildString {
                    if (extraInfo.isNotBlank()) append(extraInfo.trim())
                    if (contextInfo.isNotBlank()) {
                        if (isNotEmpty()) append(" ")
                        append(contextInfo)
                    }
                }
            ErrorReportHelper.postCatchedException(
                throwable = throwable,
                tag = safeTag,
                extraInfo = renderedExtra,
            )
        }.onFailure { reportFailure ->
            Log.w(FALLBACK_TAG, "ErrorReportHelper failed", reportFailure)
        }
    }

    private fun safeContext(context: Map<String, String>): Map<String, String> {
        if (context.isEmpty()) return context
        return context
            .mapNotNull { (rawKey, rawValue) ->
                val key = rawKey.trim().take(LogEventConstraints.MAX_CONTEXT_KEY_LENGTH)
                if (key.isEmpty()) return@mapNotNull null
                val value =
                    SensitiveDataSanitizer
                        .sanitizeValueForKey(key, rawValue)
                        .trim()
                        .take(LogEventConstraints.MAX_CONTEXT_VALUE_LENGTH)
                key to value
            }.toMap()
    }

    private fun renderContextForBugly(context: Map<String, String>): String {
        if (context.isEmpty()) return ""
        return context.entries
            .joinToString(separator = " ") { (k, v) -> "$k=$v" }
            .take(BUGLY_EXTRA_LIMIT)
    }

    private object LogEventConstraints {
        const val MAX_CONTEXT_KEY_LENGTH = 32
        const val MAX_CONTEXT_VALUE_LENGTH = 256
    }

    private const val BUGLY_EXTRA_LIMIT = 200
}
