package com.xyoye.common_component.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.LogRuntimeState
import com.xyoye.common_component.log.tcp.TcpLogServerState
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class LogFacadeTest {
    private val received = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        LogSystem.resetForTests()
        LogSystem.policyRepositoryFactory = { defaultPolicy ->
            LogPolicyRepository(defaultPolicy, storage = NoopLogConfigStorage())
        }
        LogSystem.writerFactory = { tcpEnabledProvider, tcpSink ->
            LogWriter(
                tcpLogEnabledProvider = tcpEnabledProvider,
                tcpLogSink = tcpSink,
            )
        }
        LogSystem.tcpRunningProvider = { true }
        LogSystem.tcpEmitSink = { line -> received.add(line) }
        LogSystem.tcpApplyFromStorage =
            {
                TcpLogServerState(
                    enabled = false,
                    running = false,
                    requestedPort = 0,
                    boundPort = -1,
                    clientCount = 0,
                    lastError = null,
                )
            }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val policy =
            LogPolicy(
                name = "test",
                defaultLevel = LogLevel.DEBUG,
                samplingRules = emptyList(),
            )
        LogSystem.init(context = context, defaultPolicy = policy)
    }

    @After
    fun tearDown() {
        LogSystem.resetForTests()
        received.clear()
    }

    @Test
    fun blankMessageIsReplacedWithPlaceholder() {
        LogFacade.d(
            module = LogModule.PLAYER,
            tag = "LogFacadeTest",
            message = "  \n\t",
        )

        Thread.sleep(200)

        assertTrue(received.isNotEmpty())
        val line = received.last()
        assertTrue(line.contains("level=DEBUG"))
        assertTrue(line.contains("module=player"))
        assertTrue(line.contains("msg=\"<empty>\""))
    }
}

private class NoopLogConfigStorage : LogConfigStorage {
    override fun readPolicyName(): String? = null

    override fun readDefaultLevel(): String? = null

    override fun readPolicySource(): String? = null

    override fun readDebugToggleState(): String? = null

    override fun readLastPolicyUpdateTime(): Long = 0L

    override fun write(state: LogRuntimeState) {
        // no-op
    }
}
