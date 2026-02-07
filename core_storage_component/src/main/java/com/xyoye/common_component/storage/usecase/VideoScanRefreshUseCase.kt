package com.xyoye.common_component.storage.usecase

import com.xyoye.common_component.storage.StorageFactory
import com.xyoye.data_component.entity.MediaLibraryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoScanRefreshUseCase {
    suspend fun refreshLocalStorageVideos() {
        withContext(Dispatchers.IO) {
            val storage = StorageFactory.createStorage(MediaLibraryEntity.LOCAL) ?: return@withContext
            val rootFile = storage.getRootFile() ?: return@withContext
            storage.openDirectory(rootFile, true)
        }
    }
}
