package com.xyoye.common_component.bilibili.repository

import com.xyoye.common_component.bilibili.BilibiliKeys
import com.xyoye.common_component.bilibili.BilibiliPlaybackPreferences
import com.xyoye.data_component.data.bilibili.BilibiliPagelistItem
import com.xyoye.data_component.data.bilibili.BilibiliPlayurlData

class BilibiliPlaybackRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    suspend fun playbackHeartbeat(
        key: BilibiliKeys.Key,
        playedTimeSec: Long
    ): Result<Unit> = core.playbackHeartbeat(key, playedTimeSec)

    suspend fun pagelist(bvid: String): Result<List<BilibiliPagelistItem>> = core.pagelist(bvid)

    suspend fun playurl(
        bvid: String,
        cid: Long,
        preferences: BilibiliPlaybackPreferences
    ): Result<BilibiliPlayurlData> = core.playurl(bvid, cid, preferences)

    suspend fun playurlFallbackOrNull(
        bvid: String,
        cid: Long,
        preferences: BilibiliPlaybackPreferences
    ): Result<BilibiliPlayurlData>? = core.playurlFallbackOrNull(bvid, cid, preferences)

    suspend fun pgcPlayurl(
        epId: Long,
        cid: Long,
        avid: Long? = null,
        preferences: BilibiliPlaybackPreferences,
        session: String? = null
    ): Result<BilibiliPlayurlData> =
        core.pgcPlayurl(
            epId = epId,
            cid = cid,
            avid = avid,
            preferences = preferences,
            session = session,
        )

    suspend fun pgcPlayurlFallbackOrNull(
        epId: Long,
        cid: Long,
        avid: Long? = null,
        preferences: BilibiliPlaybackPreferences,
        session: String? = null
    ): Result<BilibiliPlayurlData>? =
        core.pgcPlayurlFallbackOrNull(
            epId = epId,
            cid = cid,
            avid = avid,
            preferences = preferences,
            session = session,
        )
}
