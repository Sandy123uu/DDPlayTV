package com.xyoye.common_component.bilibili.repository

import com.xyoye.data_component.data.bilibili.BilibiliHistoryCursorData

class BilibiliHistoryRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    suspend fun historyCursor(
        max: Long? = null,
        viewAt: Long? = null,
        business: String? = null,
        ps: Int = 30,
        type: String = "archive",
        preferCache: Boolean = true
    ): Result<BilibiliHistoryCursorData> =
        core.historyCursor(
            max = max,
            viewAt = viewAt,
            business = business,
            ps = ps,
            type = type,
            preferCache = preferCache,
        )
}
