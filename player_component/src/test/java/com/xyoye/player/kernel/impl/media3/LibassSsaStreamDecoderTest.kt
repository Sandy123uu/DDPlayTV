package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.Format
import com.xyoye.common_component.media3.testing.Media3Dependent
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
}
