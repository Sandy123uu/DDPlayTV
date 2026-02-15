package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogTag

/**
 * 对业务代码暴露的统一日志门面。
 */
object LogFacade {
    private const val EMPTY_MESSAGE_PLACEHOLDER = "<empty>"

    fun d(
        module: LogModule,
        tag: String? = null,
        message: String,
        context: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogLevel.DEBUG, module, tag, message, context, throwable)

    fun i(
        module: LogModule,
        tag: String? = null,
        message: String,
        context: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogLevel.INFO, module, tag, message, context, throwable)

    fun w(
        module: LogModule,
        tag: String? = null,
        message: String,
        context: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogLevel.WARN, module, tag, message, context, throwable)

    fun e(
        module: LogModule,
        tag: String? = null,
        message: String,
        context: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogLevel.ERROR, module, tag, message, context, throwable)

    fun log(
        level: LogLevel,
        module: LogModule,
        tag: String? = null,
        message: String,
        context: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) {
        val logTag = normalizeTag(module, tag)
        val safeMessage = normalizeMessage(message)
        val safeContext = normalizeContext(context)
        val event =
            LogEvent(
                level = level,
                module = module,
                tag = logTag,
                message = safeMessage,
                context = safeContext,
                throwable = throwable,
            )
        LogSystem.log(event)
    }

    private fun normalizeTag(
        module: LogModule,
        tag: String?
    ): LogTag? {
        val cleaned = tag?.trim().orEmpty()
        if (cleaned.isEmpty()) return null
        val safe = cleaned.take(LogTag.MAX_TAG_LENGTH)
        return LogTag(module, safe)
    }

    private fun normalizeMessage(message: String): String {
        val cleaned = message.replace('\n', ' ').replace('\r', ' ').trim()
        if (cleaned.isEmpty()) return EMPTY_MESSAGE_PLACEHOLDER
        val limit = LogEvent.MAX_MESSAGE_LENGTH
        if (cleaned.length <= limit) return cleaned
        if (limit <= 3) return cleaned.take(limit)
        return cleaned.take(limit - 3) + "..."
    }

    private fun normalizeContext(context: Map<String, String>): Map<String, String> {
        if (context.isEmpty()) return emptyMap()
        val normalized = LinkedHashMap<String, String>(context.size)
        context.forEach { (rawKey, rawValue) ->
            val cleanedKey = rawKey.trim()
            if (cleanedKey.isEmpty()) return@forEach
            val safeKey = cleanedKey.take(LogEvent.MAX_CONTEXT_KEY_LENGTH)
            val safeValue = rawValue.take(LogEvent.MAX_CONTEXT_VALUE_LENGTH)
            normalized[safeKey] = safeValue
        }
        return normalized
    }
}
