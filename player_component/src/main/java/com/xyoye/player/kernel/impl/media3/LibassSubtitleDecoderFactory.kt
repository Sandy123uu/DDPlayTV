package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.extractor.text.SubtitleDecoder
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.player.info.PlayerInitializer
import com.xyoye.player.subtitle.backend.EmbeddedSubtitleSink
import java.util.concurrent.atomic.AtomicInteger

@UnstableApi
class LibassSubtitleDecoderFactory(
    private val sinkProvider: () -> EmbeddedSubtitleSink?
) : SubtitleDecoderFactory {
    private val defaultFactory = SubtitleDecoderFactory.DEFAULT
    private val supportsLogCounter = AtomicInteger(0)
    private val createLogCounter = AtomicInteger(0)

    override fun supportsFormat(format: Format): Boolean {
        val sink = sinkProvider()
        val ssaMime = Media3SubtitleFormatClassifier.isSsaMime(format.sampleMimeType)
        val supportsByLibass = sink != null && ssaMime
        if (shouldLog(supportsLogCounter.incrementAndGet())) {
            logDecision(
                prefix = "supportsFormat",
                format = format,
                sinkPresent = sink != null,
                isSsaMime = ssaMime,
                useLibassPath = supportsByLibass,
            )
        }
        if (supportsByLibass) {
            return true
        }
        return defaultFactory.supportsFormat(format)
    }

    override fun createDecoder(format: Format): SubtitleDecoder =
        if (sinkProvider() != null && Media3SubtitleFormatClassifier.isSsaMime(format.sampleMimeType)) {
            if (shouldLog(createLogCounter.incrementAndGet())) {
                logDecision(
                    prefix = "createDecoder",
                    format = format,
                    sinkPresent = true,
                    isSsaMime = true,
                    useLibassPath = true,
                )
            }
            LibassSsaStreamDecoder(format, sinkProvider)
        } else {
            if (shouldLog(createLogCounter.incrementAndGet())) {
                val sinkPresent = sinkProvider() != null
                val ssaMime = Media3SubtitleFormatClassifier.isSsaMime(format.sampleMimeType)
                logDecision(
                    prefix = "createDecoder",
                    format = format,
                    sinkPresent = sinkPresent,
                    isSsaMime = ssaMime,
                    useLibassPath = false,
                )
            }
            defaultFactory.createDecoder(format)
        }

    private fun logDecision(
        prefix: String,
        format: Format,
        sinkPresent: Boolean,
        isSsaMime: Boolean,
        useLibassPath: Boolean
    ) {
        if (!PlayerInitializer.isPrintLog) {
            return
        }
        LogFacade.d(
            LogModule.PLAYER,
            "PlayerSubtitle",
            "libass decoderFactory $prefix sinkPresent=$sinkPresent mime=${format.sampleMimeType} isSsaMime=$isSsaMime useLibassPath=$useLibassPath codecs=${format.codecs}",
        )
    }

    private fun shouldLog(count: Int): Boolean = count <= LOG_SAMPLE_LIMIT || count % LOG_SAMPLE_INTERVAL == 0

    private companion object {
        private const val LOG_SAMPLE_LIMIT = 6
        private const val LOG_SAMPLE_INTERVAL = 50
    }
}
