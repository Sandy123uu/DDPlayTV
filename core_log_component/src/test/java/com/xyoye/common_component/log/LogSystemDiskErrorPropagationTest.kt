package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.DebugToggleState
import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.LogRuntimeState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class LogSystemDiskErrorPropagationTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @After
    fun tearDown() {
        LogSystem.resetForTests()
    }

    @Test
    fun fileWriteFailureUpdatesRuntimeStateAndStorage() {
        LogSystem.resetForTests()

        val context = TestLogContext(tempFolder.newFolder("system_disk_error"))
        val storage = InMemoryDiskErrorLogConfigStorage()
        val errorHandled = CountDownLatch(1)

        LogSystem.policyRepositoryFactory = { defaultPolicy ->
            LogPolicyRepository(defaultPolicy = defaultPolicy, storage = storage)
        }
        LogSystem.writerFactory = { appContext, tcpEnabledProvider, tcpSink, onFileError ->
            val faultyManager =
                object : LogFileManager(appContext) {
                    override fun appendLine(line: String) {
                        throw IOException("simulate disk full")
                    }
                }
            LogWriter(
                context = appContext,
                fileManager = faultyManager,
                onFileError = { error ->
                    onFileError(error)
                    errorHandled.countDown()
                },
                tcpLogEnabledProvider = tcpEnabledProvider,
                tcpLogSink = tcpSink,
            )
        }
        LogSystem.tcpRunningProvider = { false }
        LogSystem.tcpEmitSink = { _ -> }
        LogSystem.tcpApplyFromStorage = {
            com.xyoye.common_component.log.tcp.TcpLogServerState(
                enabled = false,
                running = false,
                requestedPort = 17010,
                boundPort = -1,
                clientCount = 0,
                lastError = null,
            )
        }
        LogSystem.tcpSnapshotProvider = {
            com.xyoye.common_component.log.tcp.TcpLogServerState(
                enabled = false,
                running = false,
                requestedPort = 17010,
                boundPort = -1,
                clientCount = 0,
                lastError = null,
            )
        }
        LogSystem.tcpSetEnabled = { _, _ ->
            com.xyoye.common_component.log.tcp.TcpLogServerState(
                enabled = false,
                running = false,
                requestedPort = 17010,
                boundPort = -1,
                clientCount = 0,
                lastError = null,
            )
        }

        LogSystem.init(context)
        LogSystem.updatePolicy(LogPolicy.debugSessionPolicy())
        LogSystem.startDebugSession()

        LogSystem.log(
            LogEvent(
                level = LogLevel.ERROR,
                module = LogModule.CORE,
                message = "trigger disk error",
            ),
        )

        assertTrue(errorHandled.await(2, TimeUnit.SECONDS))

        val runtimeState = waitForDiskErrorState()
        assertEquals(DebugToggleState.DISABLED_DUE_TO_ERROR, runtimeState.debugToggleState)
        assertFalse(runtimeState.debugSessionEnabled)
        assertFalse(runtimeState.activePolicy.enableDebugFile)

        assertEquals(DebugToggleState.DISABLED_DUE_TO_ERROR.name, storage.debugToggleState)
        assertFalse(storage.debugFileEnabled)
        assertTrue(storage.lastUpdated > 0)
    }

    private fun waitForDiskErrorState(): LogRuntimeState {
        repeat(20) {
            val current = LogSystem.getRuntimeState()
            if (current.debugToggleState == DebugToggleState.DISABLED_DUE_TO_ERROR) {
                return current
            }
            Thread.sleep(50)
        }
        return LogSystem.getRuntimeState()
    }
}

private class InMemoryDiskErrorLogConfigStorage : LogConfigStorage {
    var policyName: String? = null
    var defaultLevel: String? = null
    var debugFileEnabled: Boolean = false
    var exportable: Boolean = false
    var policySource: String? = null
    var debugToggleState: String? = null
    var lastUpdated: Long = 0L

    override fun readPolicyName(): String? = policyName

    override fun readDefaultLevel(): String? = defaultLevel

    override fun readDebugFileEnabled(): Boolean = debugFileEnabled

    override fun readExportable(): Boolean = exportable

    override fun readPolicySource(): String? = policySource

    override fun readDebugToggleState(): String? = debugToggleState

    override fun readLastPolicyUpdateTime(): Long = lastUpdated

    override fun write(state: LogRuntimeState) {
        policyName = state.activePolicy.name
        defaultLevel = state.activePolicy.defaultLevel.name
        debugFileEnabled = state.activePolicy.enableDebugFile
        exportable = state.activePolicy.exportable
        policySource = state.policySource.name
        debugToggleState = state.debugToggleState.name
        lastUpdated = state.lastPolicyUpdateTime
    }
}
