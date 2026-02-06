package com.xyoye.common_component.database.repository

import androidx.lifecycle.LiveData
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.data_component.bean.FolderBean
import com.xyoye.data_component.entity.ExtendFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ScanSettingsRepository {
    fun getAllFolderFilters(): LiveData<MutableList<FolderBean>> =
        DatabaseManager.instance
            .getVideoDao()
            .getAllFolder()

    suspend fun getAllExtendFolders(): MutableList<ExtendFolderEntity> =
        withContext(Dispatchers.IO) {
            DatabaseManager.instance
                .getExtendFolderDao()
                .getAll()
        }

    suspend fun addExtendFolder(
        folderPath: String,
        childCount: Int,
    ) {
        withContext(Dispatchers.IO) {
            DatabaseManager.instance
                .getExtendFolderDao()
                .insert(ExtendFolderEntity(folderPath, childCount))
        }
    }

    suspend fun removeExtendFolderAndVideos(folderPath: String) {
        withContext(Dispatchers.IO) {
            DatabaseManager.instance
                .getExtendFolderDao()
                .delete(folderPath)

            DatabaseManager.instance
                .getVideoDao()
                .deleteExtend()
        }
    }

    suspend fun updateFolderFilter(
        folderPath: String,
        filter: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            DatabaseManager.instance
                .getVideoDao()
                .updateFolderFilter(filter, folderPath)
        }
    }
}
