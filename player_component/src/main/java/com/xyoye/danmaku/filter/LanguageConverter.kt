package com.xyoye.danmaku.filter

import com.xyoye.data_component.enums.DanmakuLanguage
import com.xyoye.open_cc.OpenCC
import master.flame.danmaku.controller.DanmakuFilters
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.android.DanmakuContext

/**
 * Created by xyoye on 2023/5/27.
 */

class LanguageConverter : DanmakuFilters.BaseDanmakuFilter<DanmakuLanguage>() {
    @Volatile
    private var language: DanmakuLanguage = DanmakuLanguage.ORIGINAL

    override fun filter(
        danmaku: BaseDanmaku,
        index: Int,
        totalsizeInScreen: Int,
        timer: DanmakuTimer?,
        fromCachingTask: Boolean,
        config: DanmakuContext?
    ): Boolean {
        val currentLanguage = language
        if (currentLanguage == DanmakuLanguage.ORIGINAL) {
            return false
        }

        val origin = danmaku.text?.toString().orEmpty()
        if (origin.isEmpty()) {
            return false
        }

        if (currentLanguage == DanmakuLanguage.SC) {
            danmaku.text = OpenCC.convertSC(origin)
        } else if (currentLanguage == DanmakuLanguage.TC) {
            danmaku.text = OpenCC.convertTC(origin)
        }
        return false
    }

    override fun setData(language: DanmakuLanguage) {
        this.language = language
    }

    override fun reset() {
        language = DanmakuLanguage.ORIGINAL
    }
}
