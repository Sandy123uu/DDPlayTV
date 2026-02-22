package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.C
import androidx.media3.common.Format
import com.xyoye.common_component.media3.testing.Media3Dependent
import org.junit.Assert.assertEquals
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
        val decoder =
            LibassSsaStreamDecoder(
                format =
                    Format
                        .Builder()
                        .setSampleMimeType("text/x-ssa")
                        .build(),
                sinkProvider = { null },
            )

        assertEquals(500_000L, decoder.normalizeSampleTimeUs(1_000_000_500_000L, 1_000_000_000_000L))
        assertEquals(0L, decoder.normalizeSampleTimeUs(1_000_000_000_000L, Format.OFFSET_SAMPLE_RELATIVE))
        assertEquals(1_000_000_000_000L, decoder.normalizeSampleTimeUs(1_000_000_000_000L, C.TIME_UNSET))

        decoder.release()
    }
}
