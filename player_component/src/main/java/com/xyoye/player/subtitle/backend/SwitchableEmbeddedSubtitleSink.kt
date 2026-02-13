package com.xyoye.player.subtitle.backend

/**
 * Optional control interface for embedded subtitle sinks.
 *
 * This is used to temporarily mute embedded ASS/SSA events while an external
 * track is loaded directly into the GPU libass pipeline.
 */
interface EmbeddedSubtitleSinkController {
    fun setEnabled(enabled: Boolean)

    fun isEnabled(): Boolean
}

/**
 * Wraps an [EmbeddedSubtitleSink] and allows muting it without detaching the sink
 * from the kernel bridge.
 */
class SwitchableEmbeddedSubtitleSink(
    private val delegate: EmbeddedSubtitleSink
) : EmbeddedSubtitleSink,
    EmbeddedSubtitleSinkController {
    @Volatile
    private var enabled: Boolean = true

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun isEnabled(): Boolean = enabled

    override fun onFormat(codecPrivate: ByteArray?) {
        if (!enabled) return
        delegate.onFormat(codecPrivate)
    }

    override fun onSample(
        data: ByteArray,
        timeUs: Long,
        durationUs: Long?
    ) {
        if (!enabled) return
        delegate.onSample(data, timeUs, durationUs)
    }

    override fun onFlush() {
        if (!enabled) return
        delegate.onFlush()
    }

    override fun onRelease() {
        // Always release underlying resources, even when muted.
        delegate.onRelease()
    }
}

