package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogPolicy
import com.xyoye.common_component.log.model.LogRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(JUnit4::class)
class LogWriterHttpSinkTest {
    @Test
    fun httpSinkReceivesEventWhenAllowed() {
        val received = CopyOnWriteArrayList<LogEvent>()
        val writer = LogWriter(httpLogSink = { event -> received.add(event) })
        writer.updateRuntimeState(LogRuntimeState(activePolicy = LogPolicy.defaultReleasePolicy()))
        writer.submit(
            LogEvent(
                level = LogLevel.INFO,
                module = LogModule.CORE,
                message = "hello http",
            ),
        )

        Thread.sleep(200)

        assertEquals(1, received.size)
        assertEquals("hello http", received.first().message)
        assertEquals(LogLevel.INFO, received.first().level)
        assertEquals(LogModule.CORE, received.first().module)
    }

    @Test
    fun httpSinkNotCalledWhenBelowThreshold() {
        val received = CopyOnWriteArrayList<LogEvent>()
        val writer = LogWriter(httpLogSink = { event -> received.add(event) })
        writer.updateRuntimeState(
            LogRuntimeState(
                activePolicy = LogPolicy.defaultReleasePolicy().copy(defaultLevel = LogLevel.ERROR),
            ),
        )
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
