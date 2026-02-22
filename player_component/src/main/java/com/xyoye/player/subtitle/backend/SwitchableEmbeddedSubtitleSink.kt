package com.xyoye.player.subtitle.backend

import androidx.media3.common.util.UnstableApi

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
@UnstableApi
class SwitchableEmbeddedSubtitleSink(
    private val delegate: EmbeddedSubtitleSink
) : EmbeddedSubtitleSink,
    EmbeddedSubtitleSinkController {
    private val lock = Any()

    @Volatile
    private var enabled: Boolean = true
    private var hasCachedFormat: Boolean = false
    private var cachedCodecPrivate: ByteArray? = null

    override fun setEnabled(enabled: Boolean) {
        val replayCodecPrivate: ByteArray?
        val shouldReplayFormat: Boolean
        synchronized(lock) {
            val wasEnabled = this.enabled
            this.enabled = enabled
            shouldReplayFormat = !wasEnabled && enabled && hasCachedFormat
            replayCodecPrivate = cachedCodecPrivate?.copyOf()
        }
        if (shouldReplayFormat) {
            delegate.onFormat(replayCodecPrivate)
        }
    }

    override fun isEnabled(): Boolean = enabled

    override fun onFormat(codecPrivate: ByteArray?) {
        val shouldDispatch: Boolean
        val codecPrivateCopy = codecPrivate?.copyOf()
        synchronized(lock) {
            cachedCodecPrivate = codecPrivateCopy
            hasCachedFormat = true
            shouldDispatch = enabled
        }
        if (shouldDispatch) {
            delegate.onFormat(codecPrivateCopy)
        }
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
