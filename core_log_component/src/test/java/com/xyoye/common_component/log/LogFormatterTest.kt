package com.xyoye.common_component.log

import com.xyoye.common_component.log.model.LogEvent
import com.xyoye.common_component.log.model.LogLevel
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.log.model.LogTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogFormatterTest {
    private val formatter = LogFormatter()

    @Test
    fun formatProducesStructuredLine() {
        val event =
            LogEvent(
                timestamp = 1_700_000_000_000,
                level = LogLevel.ERROR,
                module = LogModule.PLAYER,
                tag = LogTag(LogModule.PLAYER, "Renderer"),
                message = "renderer failed\nretrying",
                context = mapOf("scene" to "playback", "errorCode" to "E001"),
                throwable = IllegalStateException("boom"),
                threadName = "LogThread",
                sequenceId = 7,
            )

        val line = formatter.format(event)

        assertTrue(line.startsWith("time="))
        assertTrue(line.contains("level=ERROR"))
        assertTrue(line.contains("module=player"))
        assertTrue(line.contains("tag=player:Renderer"))
        assertTrue(line.contains("thread=LogThread"))
        assertTrue(line.contains("seq=7"))
        assertTrue(line.contains("ctx_scene=playback"))
        assertTrue(line.contains("ctx_errorCode=E001"))
        assertTrue(line.contains("throwable=java.lang.IllegalStateException"))
        assertTrue(line.contains("msg=\"renderer failed retrying\""))
    }

    @Test
    fun formatRedactsSensitiveData() {
        val event =
            LogEvent(
                level = LogLevel.INFO,
                module = LogModule.NETWORK,
                tag = LogTag(LogModule.NETWORK, "Http"),
                message = "open https://example.com/a.m3u8?token=abc Authorization: Bearer secret",
                context =
                    mapOf(
                        "token" to "abc",
                        "cookie" to "SESSDATA=abc; bili_jct=def",
                        "url" to "https://example.com/a.m3u8?access_token=aaa#frag",
                    ),
            )

        val line = formatter.format(event)

        assertFalse(line.contains("Bearer secret"))
        assertFalse(line.contains("SESSDATA=abc"))
        assertFalse(line.contains("access_token=aaa"))
        assertFalse(line.contains("?token=abc"))
        assertTrue(line.contains("https://example.com/a.m3u8"))
        assertTrue(line.contains("token=<redacted>") || line.contains("token=<redacted>".lowercase()))
    }

    @Test
    fun formatForLogcatIsCompact() {
        val event =
            LogEvent(
                level = LogLevel.WARN,
                module = LogModule.CORE,
                tag = LogTag(LogModule.CORE, "Shell"),
                message = "line1\nline2",
                context = mapOf("scene" to "boot", "stage" to "init"),
                sequenceId = 5,
            )

        val logcat = formatter.formatForLogcat(event)

        assertTrue(logcat.startsWith("WARN/core [core:Shell] #5 scene=boot: line1 line2"))
        assertTrue(logcat.contains("stage=init"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun contextValueMustRespectLengthLimit() {
        val tooLong = "a".repeat(LogEvent.MAX_CONTEXT_VALUE_LENGTH + 1)
        LogEvent(
            level = LogLevel.DEBUG,
            module = LogModule.ANIME,
            message = "overflow",
            context = mapOf("scene" to tooLong),
        )
    }
}
