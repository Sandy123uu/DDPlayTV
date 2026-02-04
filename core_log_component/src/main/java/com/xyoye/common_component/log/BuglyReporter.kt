package com.xyoye.common_component.log

import android.content.Context
import com.tencent.bugly.crashreport.CrashReport

/**
 * Bugly 访问门面：避免业务/平台代码直接依赖 CrashReport，便于后续替换或收敛实现。
 */
object BuglyReporter {
    fun init(
        context: Context,
        appId: String,
        debug: Boolean
    ) {
        CrashReport.initCrashReport(context.applicationContext, appId, debug)
    }

    fun getAppId(): String? = runCatching { CrashReport.getAppID() }.getOrNull()

    fun getVersion(context: Context): String? = runCatching { CrashReport.getBuglyVersion(context.applicationContext) }.getOrNull()

    fun putUserData(
        context: Context,
        key: String,
        value: String
    ) {
        CrashReport.putUserData(context.applicationContext, key, value)
    }

    fun setUserSceneTag(
        context: Context,
        tagId: Int
    ) {
        CrashReport.setUserSceneTag(context.applicationContext, tagId)
    }
}
