package com.xyoye.common_component.log

import android.content.Context
import android.util.Log
import com.tencent.bugly.crashreport.CrashReport
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer

/**
 * Bugly 访问门面：避免业务/平台代码直接依赖 CrashReport，便于后续替换或收敛实现。
 */
object BuglyReporter {
    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var appContext: Context? = null

    fun init(
        context: Context,
        appId: String,
        debug: Boolean
    ) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        runCatching { CrashReport.initCrashReport(applicationContext, appId, debug) }
            .onSuccess { initialized = true }
            .onFailure { throwable ->
                initialized = false
                Log.w(LOG_TAG, "initCrashReport failed", throwable)
            }
    }

    fun isInitialized(): Boolean = initialized

    fun postCatchedException(throwable: Throwable) {
        if (!initialized) return
        runCatching { CrashReport.postCatchedException(throwable) }
            .onFailure { Log.w(LOG_TAG, "postCatchedException failed", it) }
    }

    fun getAppId(): String? = runCatching { CrashReport.getAppID() }.getOrNull()

    fun getVersion(context: Context): String? = runCatching { CrashReport.getBuglyVersion(context.applicationContext) }.getOrNull()

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
        CrashReport.putUserData(context.applicationContext, safeKey, safeValue)
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
        CrashReport.setUserSceneTag(context.applicationContext, tagId)
    }

    private const val LOG_TAG = "BuglyReporter"
}
