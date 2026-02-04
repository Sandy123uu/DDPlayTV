package com.xyoye.common_component.base.app

import android.content.Context
import androidx.startup.Initializer
import com.xyoye.common_component.log.BuglyReporter
import com.xyoye.common_component.utils.SecurityHelperConfig
import com.xyoye.core_system_component.BuildConfig

/**
 * Bugly 初始化入口（AndroidX Startup）。
 *
 * 说明：
 * - 之前在 [BaseApplication.attachBaseContext] 中初始化，可能在 MultiDex.install 之前触发，
 *   从而导致 Bugly 内部依赖类未加载、初始化失败但应用仍可运行（表现为“配置正确但无上报”）。
 * - Startup Initializer 会在 Application.onCreate 之前执行，且在 MultiDex.install 之后，
 *   既保证初始化足够早，也避免初始化时序问题。
 */
class BuglyInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (!SecurityHelperConfig.isConfigured()) return
        BuglyReporter.init(
            context = context.applicationContext,
            appId = SecurityHelperConfig.BUGLY_APP_ID,
            debug = BuildConfig.DEBUG,
        )
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf(BaseInitializer::class.java)
}
