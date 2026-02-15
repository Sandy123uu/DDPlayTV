package com.xyoye.common_component.log

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.tencent.bugly.crashreport.BuglyLog
import com.tencent.bugly.crashreport.CrashReport
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Bugly 访问门面：避免业务/平台代码直接依赖 CrashReport，便于后续替换或收敛实现。
 */
object BuglyReporter {
    private const val LOG_TAG = "BuglyReporter"
    private const val BUGLY_VALUE_LIMIT = 200
    private const val BUGLY_LOG_LIMIT = 1000
    private const val BREADCRUMB_MAX = 6
    private const val BREADCRUMB_TAG_LIMIT = 40
    private const val BREADCRUMB_CONTEXT_LIMIT = 8
    private const val BREADCRUMB_TIME_PATTERN = "HH:mm:ss.SSS"
    private const val BREADCRUMB_KEY_PREFIX = "bc."
    private const val BREADCRUMB_EMPTY_VALUE = "-"

    private val freeTextKeys =
        setOf(
            "mpv.err",
            "mpv.err_raw",
            "mpv.last_error",
            "mpv.event",
        )

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var appContext: Context? = null

    private val lock = Any()
    private val lastUserData = LinkedHashMap<String, String>(32)
    private val breadcrumbs = ArrayDeque<String>(BREADCRUMB_MAX)

    @Volatile
    private var bridge: BuglyBridge = RealBuglyBridge

    fun init(
        context: Context,
        appId: String,
        debug: Boolean
    ) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        runCatching { bridge.initCrashReport(applicationContext, appId, debug) }
            .onSuccess { initialized = true }
            .onFailure { throwable ->
                initialized = false
                Log.w(LOG_TAG, "initCrashReport failed", throwable)
            }
    }

    fun isInitialized(): Boolean = initialized

    fun postCatchedException(throwable: Throwable) {
        if (!initialized) return
        runCatching { bridge.postCatchedException(throwable) }
            .onFailure { Log.w(LOG_TAG, "postCatchedException failed", it) }
    }

    fun getAppId(): String? = runCatching { bridge.getAppId() }.getOrNull()

    fun getVersion(context: Context): String? =
        runCatching { bridge.getBuglyVersion(context.applicationContext) }.getOrNull()

    fun putUserData(
        key: String,
        value: String
    ) {
        val context = appContext ?: return
        putUserData(context, key, value)
    }

    fun putUserData(
        context: Context,
        key: String,
        value: String
    ) {
        if (!initialized) return
        val safeKey = key.trim()
        if (safeKey.isEmpty()) return
        val safeValue = SensitiveDataSanitizer.sanitizeValueForKey(safeKey, value)
        bridge.putUserData(context.applicationContext, safeKey, safeValue)
    }

    fun setUserSceneTag(tagId: Int) {
        val context = appContext ?: return
        setUserSceneTag(context, tagId)
    }

    fun setUserSceneTag(
        context: Context,
        tagId: Int
    ) {
        if (!initialized) return
        bridge.setUserSceneTag(context.applicationContext, tagId)
    }

    fun updateContext(
        key: String,
        value: String
    ) {
        val context = appContext ?: return
        if (!initialized) return

        val safeKey = key.trim()
        if (safeKey.isEmpty()) return

        val sanitized =
            when {
                safeKey.startsWith(BREADCRUMB_KEY_PREFIX) ||
                    safeKey in freeTextKeys -> SensitiveDataSanitizer.sanitizeFreeText(value)
                else -> SensitiveDataSanitizer.sanitizeValueForKey(safeKey, value)
            }
        val safeValue = sanitized.trim().take(BUGLY_VALUE_LIMIT)
        if (safeValue.isEmpty()) return

        val shouldWrite =
            synchronized(lock) {
                val previous = lastUserData[safeKey]
                if (previous == safeValue) {
                    false
                } else {
                    lastUserData[safeKey] = safeValue
                    true
                }
            }
        if (!shouldWrite) return

        runCatching { bridge.putUserData(context.applicationContext, safeKey, safeValue) }
            .onFailure { Log.w(LOG_TAG, "putUserData failed: key=$safeKey", it) }
    }

    fun updateContext(values: Map<String, String>) {
        if (values.isEmpty()) return
        values.forEach { (k, v) ->
            updateContext(k, v)
        }
    }

    fun recordBreadcrumb(
        tag: String,
        message: String,
        context: Map<String, String> = emptyMap()
    ): String {
        val safeTag =
            SensitiveDataSanitizer.sanitizeFreeText(tag)
                .trim()
                .ifBlank { "breadcrumb" }
                .take(BREADCRUMB_TAG_LIMIT)
        val safeMessage = SensitiveDataSanitizer.sanitizeFreeText(message).trim()
        val safeContext = SensitiveDataSanitizer.sanitizeContext(context)
        val contextSuffix =
            safeContext.entries
                .asSequence()
                .filter { it.key.isNotBlank() && it.value.isNotBlank() }
                .take(BREADCRUMB_CONTEXT_LIMIT)
                .joinToString(separator = ",") { (k, v) -> "$k=$v" }
                .take(BUGLY_VALUE_LIMIT)

        val timestamp =
            runCatching {
                SimpleDateFormat(BREADCRUMB_TIME_PATTERN, Locale.US).format(Date(System.currentTimeMillis()))
            }.getOrElse { System.currentTimeMillis().toString() }

        val line =
            buildString {
                append(timestamp).append('|').append(safeTag).append('|').append(safeMessage)
                if (contextSuffix.isNotBlank()) {
                    append('|').append(contextSuffix)
                }
            }.take(BUGLY_VALUE_LIMIT)

        val snapshot: List<String> =
            synchronized(lock) {
                breadcrumbs.addFirst(line)
                while (breadcrumbs.size > BREADCRUMB_MAX) {
                    breadcrumbs.removeLast()
                }
                breadcrumbs.toList()
            }

        for (i in 0 until BREADCRUMB_MAX) {
            updateContext("$BREADCRUMB_KEY_PREFIX$i", snapshot.getOrNull(i) ?: BREADCRUMB_EMPTY_VALUE)
        }
        return line
    }

    fun logI(
        tag: String,
        message: String
    ) {
        if (!initialized) return
        val safeTag = SensitiveDataSanitizer.sanitizeFreeText(tag).trim().ifBlank { "Bugly" }.take(BREADCRUMB_TAG_LIMIT)
        val safeMessage = SensitiveDataSanitizer.sanitizeFreeText(message).trim().take(BUGLY_LOG_LIMIT)
        runCatching { bridge.logInfo(safeTag, safeMessage) }
            .onFailure { Log.w(LOG_TAG, "BuglyLog.i failed: tag=$safeTag", it) }
    }

    fun logW(
        tag: String,
        message: String
    ) {
        if (!initialized) return
        val safeTag = SensitiveDataSanitizer.sanitizeFreeText(tag).trim().ifBlank { "Bugly" }.take(BREADCRUMB_TAG_LIMIT)
        val safeMessage = SensitiveDataSanitizer.sanitizeFreeText(message).trim().take(BUGLY_LOG_LIMIT)
        runCatching { bridge.logWarn(safeTag, safeMessage) }
            .onFailure { Log.w(LOG_TAG, "BuglyLog.w failed: tag=$safeTag", it) }
    }

    fun logE(
        tag: String,
        message: String
    ) {
        if (!initialized) return
        val safeTag = SensitiveDataSanitizer.sanitizeFreeText(tag).trim().ifBlank { "Bugly" }.take(BREADCRUMB_TAG_LIMIT)
        val safeMessage = SensitiveDataSanitizer.sanitizeFreeText(message).trim().take(BUGLY_LOG_LIMIT)
        runCatching { bridge.logError(safeTag, safeMessage) }
            .onFailure { Log.w(LOG_TAG, "BuglyLog.e failed: tag=$safeTag", it) }
    }

    @VisibleForTesting
    internal fun setBridgeForTest(fake: BuglyBridge) {
        bridge = fake
    }

    @VisibleForTesting
    internal fun resetForTest() {
        initialized = false
        appContext = null
        synchronized(lock) {
            lastUserData.clear()
            breadcrumbs.clear()
        }
        bridge = RealBuglyBridge
    }

    internal interface BuglyBridge {
        fun initCrashReport(
            context: Context,
            appId: String,
            debug: Boolean
        )

        fun postCatchedException(throwable: Throwable)

        fun getAppId(): String?

        fun getBuglyVersion(context: Context): String?

        fun putUserData(
            context: Context,
            key: String,
            value: String
        )

        fun setUserSceneTag(
            context: Context,
            tagId: Int
        )

        fun logInfo(
            tag: String,
            message: String
        )

        fun logWarn(
            tag: String,
            message: String
        )

        fun logError(
            tag: String,
            message: String
        )
    }

    private object RealBuglyBridge : BuglyBridge {
        override fun initCrashReport(
            context: Context,
            appId: String,
            debug: Boolean
        ) {
            CrashReport.initCrashReport(context, appId, debug)
        }

        override fun postCatchedException(throwable: Throwable) {
            CrashReport.postCatchedException(throwable)
        }

        override fun getAppId(): String? = CrashReport.getAppID()

        override fun getBuglyVersion(context: Context): String? = CrashReport.getBuglyVersion(context)

        override fun putUserData(
            context: Context,
            key: String,
            value: String
        ) {
            CrashReport.putUserData(context, key, value)
        }

        override fun setUserSceneTag(
            context: Context,
            tagId: Int
        ) {
            CrashReport.setUserSceneTag(context, tagId)
        }

        override fun logInfo(
            tag: String,
            message: String
        ) {
            BuglyLog.i(tag, message)
        }

        override fun logWarn(
            tag: String,
            message: String
        ) {
            BuglyLog.w(tag, message)
        }

        override fun logError(
            tag: String,
            message: String
        ) {
            BuglyLog.e(tag, message)
        }
    }
}
