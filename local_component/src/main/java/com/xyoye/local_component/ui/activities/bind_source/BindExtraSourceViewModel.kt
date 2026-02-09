package com.xyoye.local_component.ui.activities.bind_source

import androidx.collection.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.database.repository.PlayHistoryRepository
import com.xyoye.common_component.extension.collectable
import com.xyoye.common_component.extension.toastError
import com.xyoye.common_component.network.repository.OtherRepository
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.weight.ToastCenter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.LinkedList

/**
 * Created by xyoye on 2022/1/24
 */
class BindExtraSourceViewModel : BaseViewModel() {
    companion object {
        private const val MAX_SEGMENT_CACHE_SIZE = 50
        private const val MAX_SEARCH_TEXT_CACHE_SIZE = 25

        // 分词结果缓存
        private val segmentCache = LruCache<String, List<String>>(MAX_SEGMENT_CACHE_SIZE)

        // 搜索记录缓存，格式为 <file_directory, file_name, searched_text>。
        private val searchTextCache = LinkedList<Triple<String, String, String>>()
        private val EPISODE_SUFFIX_REGEX = Regex(".* \\d{1,2}$")
        private val EPISODE_SUFFIX_TRIM_REGEX = Regex(" \\d{1,2}$")
    }

    private lateinit var storageFile: StorageFile

    val storageFileFlow: StateFlow<StorageFile> by lazy {
        PlayHistoryRepository
            .getPlayHistoryFlow(
                storageFile.uniqueKey(),
                storageFile.storage.library.id,
            ).map {
                storageFile.clone().apply { playHistory = it }
            }.stateIn(viewModelScope, SharingStarted.Lazily, storageFile)
    }

    private val _searchTextFlow = MutableSharedFlow<String>(1)
    val searchTextFlow = _searchTextFlow.collectable

    private val _segmentTitleLiveData = MediatorLiveData<List<String>>()
    val segmentTitleLiveData: LiveData<List<String>> = _segmentTitleLiveData

    fun setStorageFile(storageFile: StorageFile) {
        this.storageFile = storageFile
        val cachedSearchText: String? = matchSearchTextCache(storageFile)
        if (cachedSearchText != null) {
            viewModelScope.launch {
                _searchTextFlow.emit(cachedSearchText)
            }
        }
    }

    fun setSearchText(text: String) {
        viewModelScope.launch {
            _searchTextFlow.emit(text)
        }
        addSearchTextCache(storageFile, text)
    }

    fun segmentTitle(storageFile: StorageFile) {
        viewModelScope.launch {
            try {
                // 从缓存中获取
                val cache = segmentCache.get(storageFile.uniqueKey()) ?: emptyList()
                if (cache.isNotEmpty()) {
                    _segmentTitleLiveData.postValue(cache)
                    return@launch
                }

                // 从网络获取
                showLoading()
                val result = OtherRepository.getSegmentWords(storageFile.fileName())
                hideLoading()

                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    ErrorReportHelper.postCatchedExceptionWithContext(
                        exception ?: RuntimeException("Unknown segment words error"),
                        "BindExtraSourceViewModel",
                        "segmentTitle",
                        "File: ${storageFile.fileName()}",
                    )
                    exception?.message?.toastError()
                    return@launch
                }

                val data = result.getOrNull() ?: return@launch
                if (data.code() == 409) {
                    ToastCenter.showError("请求过于频繁(每分钟限2次)，请稍后再试")
                    return@launch
                }
                val json = data.body()?.string() ?: ""
                val segments = parseSegmentResult(json) ?: emptyList()
                segmentCache.put(storageFile.uniqueKey(), segments)
                _segmentTitleLiveData.postValue(segments)
            } catch (e: Exception) {
                hideLoading()
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "BindExtraSourceViewModel",
                    "segmentTitle",
                    "Unexpected error for file: ${storageFile.fileName()}",
                )
                e.message?.toastError()
            }
        }
    }

    /**
     * 解析分词结果
     */
    private fun parseSegmentResult(json: String): List<String>? {
        return try {
            val responseJson = JSONObject(json)
            val resultKey =
                responseJson.names()?.get(0)?.toString()
                    ?: return null
            val jsonArray =
                responseJson.optJSONArray(resultKey)
                    ?: return null
            val wordArray =
                jsonArray.optJSONArray(0)
                    ?: return null

            val words = mutableListOf<String>()
            val wordLength = wordArray.length()
            for (i in 0 until wordLength) {
                val word = wordArray.optString(i)
                words.add(word)
            }
            words
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "BindExtraSourceViewModel",
                "parseSegmentResult",
                "JSON parsing error",
            )
            null
        }
    }

    private fun matchSearchTextCache(target: StorageFile): String? {
        return try {
            if (!target.isFile()) {
                return null
            }

            val targetDir = parseFileDir(target.filePath())
            val targetName = target.fileName()
            searchTextCache.firstNotNullOfOrNull { cachedTriple ->
                matchCachedSearchText(targetDir, targetName, cachedTriple)
            }
        } catch (e: Exception) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                e,
                "BindExtraSourceViewModel",
                "matchSearchTextCache",
                "File: ${target.fileName()}",
            )
            null
        }
    }

    private fun matchCachedSearchText(
        targetDir: String,
        targetName: String,
        cachedTriple: Triple<String, String, String>
    ): String? {
        val (cachedFileDir, cachedFileName, cachedText) = cachedTriple
        if (targetDir != cachedFileDir) {
            return null
        }

        val diff = findFileNameDiffIndexes(targetName, cachedFileName)
        if (diff.isEmpty()) {
            return cachedText
        }
        if (diff.size > 2 || !isDigitOnlyDiff(targetName, cachedFileName, diff)) {
            return null
        }
        if (diff.size == 2 && diff[1] - diff[0] != 1) {
            return null
        }
        return normalizeCachedSearchText(cachedText)
    }

    private fun findFileNameDiffIndexes(
        targetName: String,
        cachedFileName: String
    ): List<Int> {
        val diff = mutableListOf<Int>()
        val maxLength = maxOf(targetName.length, cachedFileName.length)
        for (index in 0 until maxLength) {
            val targetChar = targetName.getOrNull(index)
            val cachedChar = cachedFileName.getOrNull(index)
            if (targetChar != cachedChar) {
                diff.add(index)
            }
        }
        return diff
    }

    private fun isDigitOnlyDiff(
        targetName: String,
        cachedFileName: String,
        diffIndexes: List<Int>
    ): Boolean =
        diffIndexes.all { index ->
            targetName.getOrNull(index)?.isDigit() == true &&
                cachedFileName.getOrNull(index)?.isDigit() == true
        }

    private fun normalizeCachedSearchText(cachedText: String): String {
        if (cachedText.matches(EPISODE_SUFFIX_REGEX)) {
            return cachedText.replace(EPISODE_SUFFIX_TRIM_REGEX, "")
        }
        return cachedText
    }

    private fun addSearchTextCache(
        file: StorageFile,
        text: String
    ) {
        val dir = parseFileDir(file.filePath())
        val name = file.fileName()
        searchTextCache.addFirst(Triple(dir, name, text))
        if (searchTextCache.size > MAX_SEARCH_TEXT_CACHE_SIZE) {
            searchTextCache.removeLast()
        }
    }

    private fun parseFileDir(fullPath: String): String {
        if (fullPath.contains(Regex("/"))) {
            return fullPath.replace(Regex("/[^/]*$"), "")
        }
        return "/"
    }
}
