package com.xyoye.player.kernel.impl.media3

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes

internal object Media3SubtitleFormatClassifier {
    fun isMedia3CueFormat(format: Format): Boolean =
        format.sampleMimeType == MimeTypes.APPLICATION_MEDIA3_CUES

    fun isSsaFamilyTrack(format: Format): Boolean =
        isSsaMime(format.sampleMimeType) ||
            (isMedia3CueFormat(format) && isSsaCodecs(format.codecs))

    fun isSsaCodecs(codecs: String?): Boolean {
        if (codecs.isNullOrBlank()) {
            return false
        }
        return codecs
            .split(',')
            .asSequence()
            .map { it.trim().lowercase() }
            .any { codec ->
                codec == "text/x-ssa" ||
                    codec == "text/ssa" ||
                    codec == "text/x-ass" ||
                    codec == "application/x-ass" ||
                    codec == "application/x-ssa" ||
                    codec == "application/ass" ||
                    codec == "ssa" ||
                    codec == "ass"
            }
    }

    fun isSsaMime(mimeType: String?): Boolean {
        if (mimeType == null) {
            return false
        }
        val normalized = mimeType.lowercase()
        return mimeType == MimeTypes.TEXT_SSA ||
            normalized == "text/ssa" ||
            normalized == "text/x-ass" ||
            normalized == "application/x-ass" ||
            normalized == "application/x-ssa" ||
            normalized == "application/ass"
    }
}
