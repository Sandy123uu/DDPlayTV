package com.okamihoro.ddplaytv.app.cast

import com.xyoye.data_component.entity.media3.CastTarget
import com.xyoye.data_component.entity.media3.PlaybackSession
import com.xyoye.data_component.entity.media3.PlayerCapabilityContract
import com.xyoye.player_component.media3.fallback.CodecFallbackDecision
import com.xyoye.player_component.media3.fallback.CodecFallbackHandler
import com.xyoye.player_component.media3.mapper.LegacyCapabilityResult

data class CastSessionPayload(
    val target: CastTarget,
    val sessionId: String,
    val audioOnly: Boolean,
    val fallbackMessage: String?
)

sealed class CastSessionPrepareResult {
    data class Ready(
        val payload: CastSessionPayload
    ) : CastSessionPrepareResult()

    data class UnsupportedTarget(
        val targetId: String
    ) : CastSessionPrepareResult()

    data class Disabled(
        val message: String
    ) : CastSessionPrepareResult()
}

/**
 * Prepares metadata for Cast handoffs so PiP/background/capability state stays in sync with Media3.
 *
 * TV端说明：
 * - TV设备通常作为投屏接收端，极少需要“从TV投向其他设备”的逻辑。
 * - 若目标平台仅面向TV，可整体裁剪该管理器及其入口，避免无效依赖。
 */
class Media3CastManager(
    private val codecFallbackHandler: CodecFallbackHandler,
    private val castSenderEnabled: Boolean = false,
) {
    fun prepareCastSession(
        targetId: String,
        session: PlaybackSession,
        capability: PlayerCapabilityContract,
        capabilityResult: LegacyCapabilityResult?
    ): CastSessionPrepareResult {
        if (!castSenderEnabled) {
            return CastSessionPrepareResult.Disabled(DISABLED_REASON)
        }

        val target = capability.castTargets.firstOrNull { it.id == targetId }
            ?: return CastSessionPrepareResult.UnsupportedTarget(targetId)
        val decision = capabilityResult?.let { codecFallbackHandler.evaluate(it) }
            ?: CodecFallbackDecision.None
        val (audioOnly, message) = when (decision) {
            is CodecFallbackDecision.AudioOnly -> true to decision.reason
            is CodecFallbackDecision.BlockPlayback -> false to decision.reason
            CodecFallbackDecision.None -> false to null
        }
        return CastSessionPrepareResult.Ready(
            CastSessionPayload(
                target = target,
                sessionId = session.sessionId,
                audioOnly = audioOnly,
                fallbackMessage = message,
            )
        )
    }

    private companion object {
        const val DISABLED_REASON = "TV 端不支持发起投屏，请在接收端开启投屏接收后再连接"
    }
}
