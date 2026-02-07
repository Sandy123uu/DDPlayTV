package com.xyoye.common_component.database.repository

import androidx.lifecycle.LiveData
import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.bean.FolderBean
import com.xyoye.data_component.entity.ExtendFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ScanSettingsRepository {
    fun getAllFolderFilters(): LiveData<MutableList<FolderBean>> =
        DatabaseProvider.instance
            .getVideoDao()
            .getAllFolder()

    suspend fun getAllExtendFolders(): MutableList<ExtendFolderEntity> =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getExtendFolderDao()
                .getAll()
        }

    suspend fun addExtendFolder(
        folderPath: String,
        childCount: Int,
    ) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getExtendFolderDao()
                .insert(ExtendFolderEntity(folderPath, childCount))
        }
    }

    suspend fun removeExtendFolderAndVideos(folderPath: String) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getExtendFolderDao()
                .delete(folderPath)

            DatabaseProvider.instance
                .getVideoDao()
                .deleteExtend()
        }
    }

    suspend fun updateFolderFilter(
        folderPath: String,
        filter: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getVideoDao()
                .updateFolderFilter(filter, folderPath)
        }
    }
}
