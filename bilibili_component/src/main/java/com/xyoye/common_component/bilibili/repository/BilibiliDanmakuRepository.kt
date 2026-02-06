package com.xyoye.common_component.bilibili.repository

import okhttp3.ResponseBody

class BilibiliDanmakuRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    suspend fun danmakuXml(cid: Long): Result<ResponseBody> = core.danmakuXml(cid)

    suspend fun danmakuListSo(cid: Long): Result<ResponseBody> = core.danmakuListSo(cid)
}
