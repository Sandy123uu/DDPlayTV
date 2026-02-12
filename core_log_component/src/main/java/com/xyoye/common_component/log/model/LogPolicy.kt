package com.xyoye.common_component.log.model

/**
 * 日志策略模型：全局级别 + 是否写入调试文件 + 采样规则。
 */
data class LogPolicy(
    val name: String,
    val defaultLevel: LogLevel,
    val samplingRules: List<SamplingRule> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "LogPolicy name must not be blank" }
        samplingRules.forEach {
            require(it.sampleRate in 0.0..1.0) { "Sampling rate must be between 0 and 1" }
            it.maxEventsPerMinute?.let { limit ->
                require(limit > 0) { "maxEventsPerMinute must be positive" }
            }
        }
    }

    companion object {
        const val DEFAULT_RELEASE_POLICY_NAME = "default-release"
        const val DEBUG_SESSION_POLICY_NAME = "debug-session"
        const val HIGH_VOLUME_POLICY_NAME = "high-volume-debug"

        fun defaultReleasePolicy(): LogPolicy =
            LogPolicy(
                name = DEFAULT_RELEASE_POLICY_NAME,
                defaultLevel = LogLevel.INFO,
                samplingRules = emptyList(),
            )

        fun debugSessionPolicy(
            minLevel: LogLevel = LogLevel.DEBUG,
            samplingRules: List<SamplingRule> = emptyList(),
        ): LogPolicy =
            LogPolicy(
                name = DEBUG_SESSION_POLICY_NAME,
                defaultLevel = minLevel,
                samplingRules = samplingRules,
            )

        /**
         * 高日志量策略：用于压测/排查阶段，打开 DEBUG 级别并允许业务自定义采样规则。
         *
         * 注意：当前版本已移除本地文件日志落盘，高日志量策略会放大 logcat/TCP 输出量。
         */
        fun highVolumePolicy(
            minLevel: LogLevel = LogLevel.DEBUG,
            samplingRules: List<SamplingRule> = emptyList()
        ): LogPolicy =
            LogPolicy(
                name = HIGH_VOLUME_POLICY_NAME,
                defaultLevel = minLevel,
                samplingRules = samplingRules,
            )
    }
}

/**
 * 采样 / 限流规则，当前用于内部策略（不暴露模块独立开关）。
 */
data class SamplingRule(
    val targetModule: LogModule,
    val minLevel: LogLevel,
    val sampleRate: Double,
    val maxEventsPerMinute: Int? = null
) {
    init {
        require(sampleRate in 0.0..1.0) { "sampleRate must be between 0 and 1" }
        maxEventsPerMinute?.let { require(it > 0) { "maxEventsPerMinute must be positive" } }
    }
}
