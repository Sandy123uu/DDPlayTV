package com.xyoye.player_component.utils

import com.xyoye.common_component.media3.Media3SessionStore
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.player.info.PlayerInitializer

object PlayerErrorReporter {
    @JvmStatic
    fun report(
        throwable: Throwable,
        className: String,
        methodName: String,
        extraInfo: String = ""
    ) {
        ErrorReportHelper.postCatchedExceptionWithContext(
            throwable,
            className,
            methodName,
            enrich(extraInfo)
        )
    }

    private fun enrich(extraInfo: String): String {
        val session = Media3SessionStore.currentSession()
        val engine = session?.playerEngine?.name ?: PlayerInitializer.playerType.name
        val sessionId = session?.sessionId ?: "UNKNOWN"
        val sourceType = session?.sourceType?.name ?: "UNKNOWN"
        val mediaId = session?.mediaId?.takeIf { it.isNotBlank() }?.let { SensitiveDataSanitizer.sanitizeValueForKey("mediaId", it) }

        return buildString {
            append("engine=").append(engine)
            append(", sessionId=").append(sessionId)
            append(", sourceType=").append(sourceType)
            if (!mediaId.isNullOrBlank()) {
                append(", mediaId=").append(mediaId)
            }
            if (extraInfo.isNotBlank()) {
                append(" | ").append(extraInfo)
            }
        }
    }
}
