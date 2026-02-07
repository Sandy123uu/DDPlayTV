package com.xyoye.common_component.log

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.xyoye.common_component.log.model.DebugToggleState
import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.LogRuntimeState
import com.xyoye.common_component.log.model.PolicySource
import com.xyoye.common_component.log.tcp.TcpLogServerManager
import com.xyoye.common_component.log.tcp.TcpLogServerState
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 日志系统单例，负责初始化、策略状态维护与写入调度。
 */
object LogSystem {
    private val defaultPolicyRepositoryFactory: (LogPolicy) -> LogPolicyRepository = { defaultPolicy ->
        LogPolicyRepository(defaultPolicy)
    }

    private val defaultWriterFactory:
        (
            Context,
            () -> Boolean,
            (String) -> Unit,
            (Throwable) -> Unit,
        ) -> LogWriter = { context, tcpEnabledProvider, tcpSink, onFileError ->
            LogWriter(
                context = context,
                onFileError = onFileError,
                tcpLogEnabledProvider = tcpEnabledProvider,
                tcpLogSink = tcpSink,
            )
        }

    private val defaultTcpRunningProvider: () -> Boolean = { TcpLogServerManager.isRunning() }

    private val defaultTcpEmitSink: (String) -> Unit = { line -> TcpLogServerManager.tryEmit(line) }

    private val defaultTcpApplyFromStorage: () -> TcpLogServerState = { TcpLogServerManager.applyFromStorage() }

    private val defaultTcpSnapshotProvider: () -> TcpLogServerState = { TcpLogServerManager.snapshot() }

    private val defaultTcpSetEnabled: (Boolean, Int) -> TcpLogServerState = { enabled, port ->
        TcpLogServerManager.setEnabled(enabled, port)
    }

    private val stateRef =
        AtomicReference(
            LogRuntimeState(
                activePolicy = LogPolicy.defaultReleasePolicy(),
            ),
        )
    private val sequenceGenerator = AtomicLong(0)
    private val initLock = Any()

    internal var policyRepositoryFactory: (LogPolicy) -> LogPolicyRepository = defaultPolicyRepositoryFactory

    internal var writerFactory:
        (
            Context,
            () -> Boolean,
            (String) -> Unit,
            (Throwable) -> Unit,
        ) -> LogWriter = defaultWriterFactory

    internal var tcpRunningProvider: () -> Boolean = defaultTcpRunningProvider

    internal var tcpEmitSink: (String) -> Unit = defaultTcpEmitSink

    internal var tcpApplyFromStorage: () -> TcpLogServerState = defaultTcpApplyFromStorage

    internal var tcpSnapshotProvider: () -> TcpLogServerState = defaultTcpSnapshotProvider

    internal var tcpSetEnabled: (Boolean, Int) -> TcpLogServerState = defaultTcpSetEnabled

    private var policyRepository: LogPolicyRepository = policyRepositoryFactory(LogPolicy.defaultReleasePolicy())

    @Volatile
    private var initialized = false

    @Volatile
    private var writer: LogWriter? = null

    fun init(
        context: Context,
        defaultPolicy: LogPolicy = LogPolicy.defaultReleasePolicy()
    ) {
        if (initialized) return
        synchronized(initLock) {
            if (initialized) return
            policyRepository = policyRepositoryFactory(defaultPolicy)
            val initialState = policyRepository.loadFromStorage()
            stateRef.set(initialState)
            writer =
                writerFactory(
                    context.applicationContext,
                    { tcpRunningProvider() },
                    { line -> tcpEmitSink(line) },
                    { error -> handleWriterFileError(error) },
                ).also { it.updateRuntimeState(initialState) }
            tcpApplyFromStorage()
            initialized = true
        }
    }

    fun loadPolicyFromStorage(): LogRuntimeState {
        if (!initialized) {
            Log.w(LOG_TAG, "loadPolicyFromStorage called before init, ignore")
            return stateRef.get()
        }
        val refreshed = policyRepository.loadFromStorage()
        return applyRuntimeState(refreshed)
    }

    fun getLoggingPolicy(): LogRuntimeState = getRuntimeState()

    fun updateLoggingPolicy(
        policy: LogPolicy,
        source: PolicySource = PolicySource.USER_OVERRIDE
    ): LogRuntimeState {
        if (!initialized) {
            Log.w(LOG_TAG, "updatePolicy called before init, ignore")
            return stateRef.get()
        }
        val updated = policyRepository.updatePolicy(policy, source)
        return applyRuntimeState(updated)
    }

    fun updatePolicy(
        policy: LogPolicy,
        source: PolicySource = PolicySource.USER_OVERRIDE
    ): LogRuntimeState = updateLoggingPolicy(policy, source)

    fun startDebugSession(): LogRuntimeState = updateDebugState(DebugToggleState.ON_CURRENT_SESSION, forceEnableFile = true)

    fun stopDebugSession(): LogRuntimeState = updateDebugState(DebugToggleState.OFF, forceEnableFile = false)

    fun markDiskError(): LogRuntimeState = updateDebugState(DebugToggleState.DISABLED_DUE_TO_ERROR, forceEnableFile = false)

    fun getRuntimeState(): LogRuntimeState = stateRef.get()

    fun isInitialized(): Boolean = initialized

    fun getTcpLogServerState(): TcpLogServerState = tcpSnapshotProvider()

    fun setTcpLogServerEnabled(
        enabled: Boolean,
        port: Int = TcpLogServerManager.DEFAULT_PORT
    ): TcpLogServerState {
        if (!initialized) {
            Log.w(LOG_TAG, "tcp log server state change before init, ignore")
            return tcpSnapshotProvider()
        }
        return tcpSetEnabled(enabled, port)
    }

    fun log(event: LogEvent) {
        if (!initialized) {
            Log.w(LOG_TAG, "log called before init, fallback to logcat only")
            fallbackLogcat(event)
            return
        }
        val enriched =
            event.copy(
                sequenceId = if (event.sequenceId == 0L) sequenceGenerator.incrementAndGet() else event.sequenceId,
            )
        writer?.submit(enriched)
    }

    private fun handleWriterFileError(error: Throwable) {
        val runtime = stateRef.get()
        val alreadyDisabled = runtime.debugToggleState == DebugToggleState.DISABLED_DUE_TO_ERROR && !runtime.debugSessionEnabled
        if (!alreadyDisabled) {
            markDiskError()
        }
        Log.e(LOG_TAG, "log file write failed, debug file logging disabled", error)
    }

    private fun updateDebugState(
        state: DebugToggleState,
        forceEnableFile: Boolean? = null
    ): LogRuntimeState {
        if (!initialized) {
            Log.w(LOG_TAG, "debug state change before init, ignore")
            return stateRef.get()
        }
        val updated = policyRepository.updateDebugState(state, forceEnableFile)
        return applyRuntimeState(updated)
    }

    private fun fallbackLogcat(event: LogEvent) {
        val formatter = LogFormatter()
        val content = formatter.formatForLogcat(event)
        when (event.level) {
            LogLevel.DEBUG -> Log.d(LOG_TAG, content, event.throwable)
            LogLevel.INFO -> Log.i(LOG_TAG, content, event.throwable)
            LogLevel.WARN -> Log.w(LOG_TAG, content, event.throwable)
            LogLevel.ERROR -> Log.e(LOG_TAG, content, event.throwable)
        }
    }

    private fun applyRuntimeState(state: LogRuntimeState): LogRuntimeState {
        stateRef.set(state)
        writer?.updateRuntimeState(state)
        SubtitleTelemetryLogger.updateFromRuntime(state)
        // TCP 日志输出门禁：仅在“调试会话 + 显式授权”下允许运行
        tcpApplyFromStorage()
        return state
    }

    @VisibleForTesting
    internal fun resetForTests() {
        synchronized(initLock) {
            initialized = false
            writer = null
            sequenceGenerator.set(0)
            policyRepositoryFactory = defaultPolicyRepositoryFactory
            writerFactory = defaultWriterFactory
            tcpRunningProvider = defaultTcpRunningProvider
            tcpEmitSink = defaultTcpEmitSink
            tcpApplyFromStorage = defaultTcpApplyFromStorage
            tcpSnapshotProvider = defaultTcpSnapshotProvider
            tcpSetEnabled = defaultTcpSetEnabled
            policyRepository = policyRepositoryFactory(LogPolicy.defaultReleasePolicy())
            stateRef.set(
                LogRuntimeState(
                    activePolicy = LogPolicy.defaultReleasePolicy(),
                ),
            )
        }
    }

    private const val LOG_TAG = "LogSystem"
}
