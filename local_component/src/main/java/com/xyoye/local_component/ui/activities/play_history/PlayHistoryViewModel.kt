package com.xyoye.local_component.ui.activities.play_history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.common_component.source.VideoSourceManager
import com.xyoye.common_component.source.factory.StorageVideoSourceFactory
import com.xyoye.common_component.storage.StorageFactory
import com.xyoye.common_component.storage.impl.LinkStorage
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.thunder.ThunderManager
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.entity.PlayHistoryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.local_component.utils.HistorySortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayHistoryViewModel : BaseViewModel() {
    private val _historyLiveData = MutableLiveData<List<PlayHistoryEntity>>()
    val historyLiveData: LiveData<List<PlayHistoryEntity>> = _historyLiveData
    val playLiveData = MutableLiveData<Any>()

    // 文件排序选项
    private var sortOption = HistorySortOption()

    var mediaType = MediaType.OTHER_STORAGE

    fun updatePlayHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val historyData =
                    if (mediaType == MediaType.OTHER_STORAGE) {
                        DatabaseProvider.instance.getPlayHistoryDao().getAll()
                    } else {
                        DatabaseProvider.instance.getPlayHistoryDao().getSingleMediaType(mediaType)
                    }
                _historyLiveData.postValue(historyData)
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "updatePlayHistory",
                    "mediaType" to mediaType.name,
                )
            }
        }
    }

    fun removeHistory(history: PlayHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatabaseProvider.instance.getPlayHistoryDao().delete(history.id)
                updatePlayHistory()
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "removeHistory",
                    "historyId" to history.id,
                    "url" to history.url,
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val historyDao = DatabaseProvider.instance.getPlayHistoryDao()
                if (mediaType == MediaType.STREAM_LINK || mediaType == MediaType.MAGNET_LINK) {
                    historyDao.deleteTypeAll(listOf(mediaType))
                } else {
                    historyDao.deleteAll()
                }
                updatePlayHistory()
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "clearHistory",
                    "mediaType" to mediaType.name,
                )
            }
        }
    }

    /**
     * 修改文件排序
     */
    fun changeSortOption(option: HistorySortOption) {
        sortOption = option
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentFiles = _historyLiveData.value ?: return@launch
                mutableListOf<PlayHistoryEntity>()
                    .plus(currentFiles)
                    .sortedWith(sortOption.createComparator())
                    .apply { _historyLiveData.postValue(this) }
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "changeSortOption",
                    "sortOption" to option.javaClass.simpleName,
                )
            }
        }
    }

    fun unbindDanmu(history: PlayHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newHistory = history.copy(danmuPath = null, episodeId = null)
                DatabaseProvider.instance.getPlayHistoryDao().insert(newHistory)
                updatePlayHistory()
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "unbindDanmu",
                    "historyId" to history.id,
                    "url" to history.url,
                )
            }
        }
    }

    fun unbindSubtitle(history: PlayHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newHistory = history.copy(subtitlePath = null)
                DatabaseProvider.instance.getPlayHistoryDao().insert(newHistory)
                updatePlayHistory()
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "unbindSubtitle",
                    "historyId" to history.id,
                    "url" to history.url,
                )
            }
        }
    }

    fun openHistory(history: PlayHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (setupHistorySource(history)) {
                    playLiveData.postValue(Any())
                }
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "openHistory",
                    "historyId" to history.id,
                    "url" to history.url,
                )
            }
        }
    }

    fun openStreamLink(
        link: String,
        headers: Map<String, String>?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (setupLinkSource(link, headers)) {
                    playLiveData.postValue(Any())
                }
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "PlayHistoryViewModel",
                    "openStreamLink",
                    "url" to link,
                )
            }
        }
    }

    private suspend fun setupHistorySource(history: PlayHistoryEntity): Boolean {
        return try {
            showLoading()
            val library =
                history.storageId
                    ?.run { DatabaseProvider.instance.getMediaLibraryDao().getById(this) }
            if (library == null) {
                hideLoading()
                ToastCenter.showError("播放失败，找不到播放资源")
                return false
            }

            if (library.mediaType == MediaType.MAGNET_LINK) {
                val errorMessage = ThunderManager.ensureInitializedOrErrorMessage()
                if (errorMessage != null) {
                    hideLoading()
                    ToastCenter.showError(errorMessage)
                    return false
                }
            }

            val mediaSource =
                StorageFactory
                    .createStorage(library)
                    ?.run { historyFile(history) }
                    ?.run { StorageVideoSourceFactory.create(this) }
            hideLoading()

            if (mediaSource == null) {
                ToastCenter.showError("播放失败，找不到播放资源")
                return false
            }
            VideoSourceManager.getInstance().setSource(mediaSource)
            true
        } catch (e: Exception) {
            hideLoading()
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "PlayHistoryViewModel",
                "setupHistorySource",
                "historyId" to history.id,
                "storageId" to history.storageId,
            )
            ToastCenter.showError("播放失败，找不到播放资源")
            false
        }
    }

    private suspend fun setupLinkSource(
        link: String,
        headers: Map<String, String>?
    ): Boolean {
        return try {
            showLoading()
            val mediaSource =
                MediaLibraryEntity.STREAM
                    .copy(url = link)
                    .run { StorageFactory.createStorage(this) }
                    ?.run { this as? LinkStorage }
                    ?.apply { this.setupHttpHeader(headers) }
                    ?.run { getRootFile() }
                    ?.run { StorageVideoSourceFactory.create(this) }
            hideLoading()

            if (mediaSource == null) {
                ToastCenter.showError("播放失败，找不到播放资源")
                return false
            }
            VideoSourceManager.getInstance().setSource(mediaSource)
            true
        } catch (e: Exception) {
            hideLoading()
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "PlayHistoryViewModel",
                "setupLinkSource",
                "url" to link,
            )
            ToastCenter.showError("播放失败，找不到播放资源")
            false
        }
    }
}
