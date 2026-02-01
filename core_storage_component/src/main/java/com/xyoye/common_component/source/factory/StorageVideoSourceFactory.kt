package com.xyoye.common_component.source.factory

import android.net.Uri
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.config.DanmuConfig
import com.xyoye.common_component.config.PlayerConfig
import com.xyoye.common_component.config.SubtitleConfig
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.resolver.PlaybackProfileResolver
import com.xyoye.common_component.source.media.StorageVideoSource
import com.xyoye.common_component.storage.Storage
import com.xyoye.common_component.storage.StorageSortOption
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.utils.isFileExist
import com.xyoye.common_component.utils.getDirPath
import com.xyoye.common_component.storage.impl.LinkStorage
import com.xyoye.data_component.bean.LocalDanmuBean
import com.xyoye.data_component.bean.PlaybackProfile
import com.xyoye.data_component.enums.PlayerType

/**
 * Created by xyoye on 2023/1/2.
 */

object StorageVideoSourceFactory {
    private const val LOG_TAG = "StorageVideoSourceFactory"

    suspend fun create(file: StorageFile): StorageVideoSource? {
        val profile = resolvePlaybackProfile(file.storage)
        return create(file, profile)
    }

    suspend fun create(
        file: StorageFile,
        profile: PlaybackProfile
    ): StorageVideoSource? {
        val storage = file.storage
        val videoSources = getVideoSources(storage)
        val playUrl = storage.createPlayUrl(file, profile) ?: return null
        val danmu = findLocalDanmu(file, storage)
        val subtitlePath = getSubtitlePath(file, storage)
        val audioPath = file.playHistory?.audioPath
        return StorageVideoSource(
            playUrl,
            file,
            videoSources,
            danmu,
            subtitlePath,
            audioPath,
            profile,
        )
    }

    private fun resolvePlaybackProfile(storage: Storage): PlaybackProfile {
        val globalPlayerType = PlayerType.valueOf(PlayerConfig.getUsePlayerType())
        val resolvedLibrary = storage.library.takeIf { storage !is LinkStorage }
        return PlaybackProfileResolver.resolve(
            library = resolvedLibrary,
            globalPlayerType = globalPlayerType,
            mediaType = storage.library.mediaType,
            supportedPlayerTypes = storage.supportedPlayerTypes(),
            preferredPlayerType = storage.preferredPlayerType(),
        )
    }

    private suspend fun findLocalDanmu(
        file: StorageFile,
        storage: Storage
    ): LocalDanmuBean? {
        // 从播放记录读取弹幕
        val history = file.playHistory
        val historyDanmuPath = history?.danmuPath?.takeIf { it.isNotBlank() }
        if (historyDanmuPath != null) {
            if (isFileExist(historyDanmuPath)) {
                return LocalDanmuBean(historyDanmuPath, history.episodeId)
            }
            LogFacade.w(
                LogModule.STORAGE,
                LOG_TAG,
                "history danmu path missing, try recover from storage",
                mapOf(
                    "storageId" to storage.library.id.toString(),
                    "mediaType" to storage.library.mediaType.name,
                    "uniqueKey" to file.uniqueKey(),
                    "filePath" to runCatching { file.filePath() }.getOrNull().orEmpty(),
                    "danmuPath" to historyDanmuPath,
                ),
            )
        }

        // 是否匹配同文件夹内同名弹幕
        if (DanmuConfig.isAutoLoadSameNameDanmu()) {
            storage.cacheDanmu(file)?.let { return it }
            if (openParentDirectoryIfNeeded(file, storage)) {
                return storage.cacheDanmu(file)
            }
        }

        return null
    }

    private suspend fun getSubtitlePath(
        file: StorageFile,
        storage: Storage
    ): String? {
        val subtitleNotFound = null

        // 从播放记录读取字幕（若文件已不存在，则忽略并尝试重新匹配/缓存）
        val historySubtitlePath = file.playHistory?.subtitlePath?.takeIf { it.isNotBlank() }
        if (historySubtitlePath != null) {
            if (isFileExist(historySubtitlePath)) {
                return normalizeFileScheme(historySubtitlePath)
            }
            LogFacade.w(
                LogModule.STORAGE,
                LOG_TAG,
                "history subtitle path missing, try recover from storage",
                mapOf(
                    "storageId" to storage.library.id.toString(),
                    "mediaType" to storage.library.mediaType.name,
                    "uniqueKey" to file.uniqueKey(),
                    "filePath" to runCatching { file.filePath() }.getOrNull().orEmpty(),
                    "subtitlePath" to historySubtitlePath,
                ),
            )
        }

        // 是否匹配同文件夹内同名字幕
        if (SubtitleConfig.isAutoLoadSameNameSubtitle()) {
            storage.cacheSubtitle(file)?.let { return it }
            if (openParentDirectoryIfNeeded(file, storage)) {
                return storage.cacheSubtitle(file) ?: subtitleNotFound
            }
            return subtitleNotFound
        }

        return subtitleNotFound
    }

    private fun normalizeFileScheme(path: String): String {
        if (!path.startsWith("file://")) {
            return path
        }
        return Uri.parse(path).path ?: path
    }

    private suspend fun openParentDirectoryIfNeeded(
        file: StorageFile,
        storage: Storage
    ): Boolean {
        return runCatching {
            val filePath = file.filePath()
            val parentPath = getDirPath(filePath).ifBlank { "/" }
            val parentDir = storage.pathFile(parentPath, isDirectory = true) ?: return false
            storage.openDirectory(parentDir, refresh = false)
            true
        }.getOrDefault(false)
    }

    private fun getVideoSources(storage: Storage): List<StorageFile> =
        storage.directoryFiles
            .filter { it.isVideoFile() }
            .filter { AppConfig.isShowHiddenFile() || !it.fileName().startsWith(".") }
            .sortedWith(StorageSortOption.comparator())
}
