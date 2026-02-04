package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.LogRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class LogWriterTcpSinkTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun tcpSinkReceivesFormattedLineWhenEnabled() {
        val context = TestLogContext(tempFolder.newFolder("tcp_sink_enabled"))
        val received = CopyOnWriteArrayList<String>()
        val writer =
            LogWriter(
                context,
                tcpLogEnabledProvider = { true },
                tcpLogSink = { line -> received.add(line) },
            )
        writer.updateRuntimeState(LogRuntimeState(activePolicy = LogPolicy.defaultReleasePolicy()))
        writer.submit(
            LogEvent(
                level = LogLevel.INFO,
                module = LogModule.CORE,
                message = "hello tcp",
            ),
        )

        Thread.sleep(200)

        assertEquals(1, received.size)
        assertTrue(received.first().contains("msg=\"hello tcp\""))
        assertTrue(received.first().contains("level=INFO"))
        assertTrue(received.first().contains("module=core"))
    }

    @Test
    fun tcpSinkNotCalledWhenDisabled() {
        val context = TestLogContext(tempFolder.newFolder("tcp_sink_disabled"))
        val received = CopyOnWriteArrayList<String>()
        val writer =
            LogWriter(
                context,
                tcpLogEnabledProvider = { false },
                tcpLogSink = { line -> received.add(line) },
            )
        writer.updateRuntimeState(LogRuntimeState(activePolicy = LogPolicy.defaultReleasePolicy()))
        writer.submit(
            LogEvent(
                level = LogLevel.INFO,
                module = LogModule.CORE,
                message = "should_not_send",
            ),
        )

        Thread.sleep(200)

        assertEquals(0, received.size)
    }
}

