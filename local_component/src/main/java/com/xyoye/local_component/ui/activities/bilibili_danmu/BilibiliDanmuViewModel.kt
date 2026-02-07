package com.xyoye.local_component.ui.activities.bilibili_danmu

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.storage.usecase.BilibiliDanmuUseCase
import com.xyoye.common_component.utils.PathHelper
import com.xyoye.data_component.bean.LocalDanmuBean
import com.xyoye.data_component.data.EpisodeCidData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BilibiliDanmuViewModel : BaseViewModel() {
    val downloadMessageLiveData = MutableLiveData<String>()
    val clearMessageLiveData = MutableLiveData<Boolean>()

    fun downloadByCode(
        code: String,
        isAvCode: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            clearDownloadMessage()

            val mode = if (isAvCode) "AV号" else "BV号"
            sendDownloadMessage("以${mode}模式下载：$code")

            actionDo("开始获取CID")
            val episodeCid = BilibiliDanmuUseCase.findEpisodeCidByCode(code, isAvCode)
            if (episodeCid == null) {
                actionFailed()
                return@launch
            }
            actionSuccess(episodeCid.cid)

            saveEpisodeDanmu(episodeCid)
        }
    }

    fun downloadByUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            clearDownloadMessage()
            val isBangumiUrl = Uri.parse(url).pathSegments.firstOrNull() == "bangumi"
            if (isBangumiUrl) {
                downloadByBangumiUrl(url)
            } else {
                downloadByVideoUrl(url)
            }
        }
    }

    private suspend fun downloadByBangumiUrl(url: String) {
        sendDownloadMessage("以视频链接模式下载：$url")

        actionDo("获取番剧剧集列表")
        val result = BilibiliDanmuUseCase.findBangumiEpisodes(url)
        val animeCid = result.data
        if (animeCid == null || animeCid.episodes.isEmpty()) {
            result.errorMessage?.let {
                sendDownloadMessage("错误：$it")
            }
            actionFailed()
            return
        }
        actionSuccess("共${animeCid.episodes.size}集")

        actionDo("开始下载弹幕")
        sendDownloadMessage("========================")
        val resultList =
            animeCid.episodes.map {
                saveEpisodeDanmu(it, animeCid.animeTitle)
            }
        sendDownloadMessage("------------------------")
        sendDownloadMessage("========================")

        val successCount = resultList.count { it != null }
        val failedCount = resultList.size - successCount
        sendDownloadMessage("")
        sendDownloadMessage("番剧剧集弹幕下载完成，成功：$successCount, 失败 :$failedCount")

        if (successCount > 0) {
            val dirPath = File(PathHelper.getDanmuDirectory(), animeCid.animeTitle).absolutePath
            sendDownloadMessage("弹幕已保存目录：$dirPath")
        }
    }

    private suspend fun downloadByVideoUrl(url: String) {
        sendDownloadMessage("以视频链接模式下载：$url")

        actionDo("获取视频CID")
        val result = BilibiliDanmuUseCase.findVideoEpisodeCid(url)
        val episodeCid = result.data
        if (episodeCid == null) {
            result.errorMessage?.let {
                sendDownloadMessage("错误：$it")
            }
            actionFailed()
            return
        }
        actionSuccess(episodeCid.cid)

        saveEpisodeDanmu(episodeCid)
    }

    private suspend fun saveEpisodeDanmu(episodeCid: EpisodeCidData) {
        actionDo("开始下载弹幕")
        sendDownloadMessage("========================")
        val localDanmu = saveEpisodeDanmu(episodeCid, "")
        sendDownloadMessage("------------------------")
        sendDownloadMessage("========================")

        if (localDanmu != null) {
            sendDownloadMessage("")
            sendDownloadMessage("弹幕文件已保存至：")
            sendDownloadMessage(localDanmu.danmuPath)
        }
    }

    private suspend fun saveEpisodeDanmu(
        episodeCid: EpisodeCidData,
        animeTitle: String
    ): LocalDanmuBean? {
        sendDownloadMessage("------------------------")
        sendDownloadMessage("剧集：${episodeCid.title}")

        actionDo("下载并保存弹幕内容")
        val localDanmu = BilibiliDanmuUseCase.downloadEpisodeDanmu(episodeCid, animeTitle)
        if (localDanmu == null) {
            actionFailed()
            return null
        }
        actionSuccess()
        return localDanmu
    }

    private suspend fun actionDo(name: String) {
        sendDownloadMessage("")
        sendDownloadMessage("操作：$name")
    }

    private suspend fun actionFailed() {
        sendDownloadMessage("结果：失败")
    }

    private suspend fun actionSuccess(extra: String? = null) {
        val display = if (extra.isNullOrEmpty()) "" else "，$extra"
        sendDownloadMessage("结果：成功$display")
    }

    private suspend fun sendDownloadMessage(message: String) {
        withContext(Dispatchers.Main) {
            downloadMessageLiveData.value = message
        }
    }

    private fun clearDownloadMessage() {
        clearMessageLiveData.postValue(true)
    }
}
