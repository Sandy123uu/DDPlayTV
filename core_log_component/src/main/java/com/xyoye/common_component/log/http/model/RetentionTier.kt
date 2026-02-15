package com.xyoye.common_component.log.http.model

data class RetentionTier(
    val days: Int,
    val maxBytes: Long,
) {
    companion object {
        private const val ONE_GB_BYTES = 1L * 1024 * 1024 * 1024

        val Tier7Days: RetentionTier = RetentionTier(days = 7, maxBytes = ONE_GB_BYTES)
        val Tier14Days: RetentionTier = RetentionTier(days = 14, maxBytes = 2L * ONE_GB_BYTES)
        val Tier30Days: RetentionTier = RetentionTier(days = 30, maxBytes = 4L * ONE_GB_BYTES)

        fun forDays(days: Int): RetentionTier =
            when (days) {
                14 -> Tier14Days
                30 -> Tier30Days
                else -> Tier7Days
            }
    }
}
