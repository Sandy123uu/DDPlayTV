package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.C
import androidx.media3.common.Format
import com.xyoye.common_component.media3.testing.Media3Dependent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Media3Dependent("Prevent regressions that break libass SSA decoder initialization")
class LibassSsaStreamDecoderTest {
    @Test
    fun constructor_initializesWithoutArrayStoreException() {
        val decoder =
            LibassSsaStreamDecoder(
                format =
                    Format
                        .Builder()
                        .setSampleMimeType("text/x-ssa")
                        .build(),
                sinkProvider = { null },
            )

        decoder.release()
    }

    @Test
    fun normalizeSampleTimeUs_alignsSampleRelativeTimeline() {
        val decoder = createDecoder()

        assertEquals(500_000L, decoder.normalizeSampleTimeUs(1_000_000_500_000L, 1_000_000_000_000L))
        assertEquals(500_000L, decoder.normalizeSampleTimeUs(1_000_000_500_000L, Format.OFFSET_SAMPLE_RELATIVE))
        assertEquals(500_000L, decoder.normalizeSampleTimeUs(500_000L, Format.OFFSET_SAMPLE_RELATIVE))
        assertEquals(1_000_000_000_000L, decoder.normalizeSampleTimeUs(1_000_000_000_000L, C.TIME_UNSET))

        decoder.release()
    }

    @Test
    fun normalizeSampleForTest_unwrapsMedia3MatroskaDialoguePrefix() {
        val decoder = createDecoder()
        val payload =
            "Dialogue: 0:00:00:00,0:00:01:69,0,0,Default,,0,0,0,,Test line"
                .toByteArray(Charsets.UTF_8)

        val (normalized, durationUs) = decoder.normalizeSampleForTest(payload)

        assertEquals("0,0,Default,,0,0,0,,Test line", normalized.toString(Charsets.UTF_8))
        assertEquals(1_690_000L, durationUs)
        decoder.release()
    }

    @Test
    fun normalizeSampleForTest_keepsRawPayloadWhenNotWrapped() {
        val decoder = createDecoder()
        val payload = "0,0,Default,,0,0,0,,Raw line".toByteArray(Charsets.UTF_8)

        val (normalized, durationUs) = decoder.normalizeSampleForTest(payload)

        assertTrue(normalized.contentEquals(payload))
        assertNull(durationUs)
        decoder.release()
    }

    private fun createDecoder(): LibassSsaStreamDecoder =
        LibassSsaStreamDecoder(
            format =
                Format
                    .Builder()
                    .setSampleMimeType("text/x-ssa")
                    .build(),
            sinkProvider = { null },
        )
}
