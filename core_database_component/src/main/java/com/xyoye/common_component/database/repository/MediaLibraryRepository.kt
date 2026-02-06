package com.xyoye.common_component.database.repository

import androidx.lifecycle.LiveData
import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaLibraryRepository {
    fun getAll(): LiveData<MutableList<MediaLibraryEntity>> =
        DatabaseProvider.instance
            .getMediaLibraryDao()
            .getAll()

    suspend fun getById(libraryId: Int): MediaLibraryEntity? =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .getById(libraryId)
        }

    suspend fun getByMediaType(mediaType: MediaType): MutableList<MediaLibraryEntity> =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .getByMediaTypeSuspend(mediaType)
        }

    suspend fun getByUrl(
        url: String,
        mediaType: MediaType,
    ): MediaLibraryEntity? =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .getByUrl(url, mediaType)
        }

    suspend fun insert(vararg mediaLibraries: MediaLibraryEntity) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .insert(*mediaLibraries)
        }
    }

    suspend fun delete(
        url: String,
        mediaType: MediaType,
    ) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .delete(url, mediaType)
        }
    }

    suspend fun clearPassword(libraryId: Int) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .clearPassword(libraryId)
        }
    }

    suspend fun clearRemoteSecret(libraryId: Int) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMediaLibraryDao()
                .clearRemoteSecret(libraryId)
        }
    }
}
