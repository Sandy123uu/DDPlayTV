package com.xyoye.user_component.ui.fragment.scan_extend

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.database.repository.ScanSettingsRepository
import com.xyoye.common_component.storage.usecase.VideoScanRefreshUseCase
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.meida.VideoScan
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.entity.ExtendFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanExtendFragmentViewModel : BaseViewModel() {
    val extendFolderLiveData = MutableLiveData<MutableList<Any>>()
    val extendAppendedLiveData = MutableLiveData<Any>()

    fun getExtendFolder() {
        viewModelScope.launch {
            try {
                val entities = ScanSettingsRepository.getAllExtendFolders()
                val extendFolderList = arrayListOf<Any>()
                // 扩展目录
                extendFolderList.addAll(entities)
                // 添加按钮
                extendFolderList.add(0)
                extendFolderLiveData.postValue(extendFolderList)
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "ScanExtendFragmentViewModel",
                    "getExtendFolder",
                    "Failed to get extend folder list from database",
                )
            }
        }
    }

    fun removeExtendFolder(entity: ExtendFolderEntity) {
        viewModelScope.launch {
            try {
                ScanSettingsRepository.removeExtendFolderAndVideos(entity.folderPath)
                // 刷新扩展目录UI
                getExtendFolder()
            } catch (e: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "ScanExtendFragmentViewModel",
                    "removeExtendFolder",
                    "Failed to remove extend folder: ${entity.folderPath}",
                )
            }
        }
    }

    fun addExtendFolder(folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                showLoading()
                val extendVideos = VideoScan.traverse(folderPath)
                hideLoading()

                if (extendVideos.isEmpty()) {
                    ToastCenter.showError("失败，当前文件夹内未识别到任何视频")
                    return@launch
                }

                // 新增扩展目录到数据库
                ScanSettingsRepository.addExtendFolder(folderPath, extendVideos.size)

                // 刷新本地视频库
                VideoScanRefreshUseCase.refreshLocalStorageVideos()
                // 刷新扩展目录UI
                getExtendFolder()

                // 关闭弹窗
                extendAppendedLiveData.postValue(Any())
            } catch (e: Exception) {
                hideLoading()
                ErrorReportHelper.postCatchedExceptionWithContext(
                    e,
                    "ScanExtendFragmentViewModel",
                    "addExtendFolder",
                    "Failed to add extend folder: $folderPath",
                )
                ToastCenter.showError("添加扩展目录失败，请稍后再试")
            }
        }
    }
}
