package com.xyoye.common_component.database.repository

import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.entity.PlayHistoryEntity
import com.xyoye.data_component.enums.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

object PlayHistoryRepository {
    suspend fun getAll(): MutableList<PlayHistoryEntity> =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .getAll()
        }

    suspend fun getSingleMediaType(mediaType: MediaType): MutableList<PlayHistoryEntity> =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .getSingleMediaType(mediaType)
        }

    suspend fun getLastPlay(vararg mediaTypes: MediaType): PlayHistoryEntity? =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .gitLastPlay(*mediaTypes)
        }

    suspend fun getPlayHistory(
        uniqueKey: String,
        storageId: Int,
    ): PlayHistoryEntity? =
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .getPlayHistory(uniqueKey, storageId)
        }

    fun getPlayHistoryFlow(
        uniqueKey: String,
        storageId: Int,
    ): Flow<PlayHistoryEntity?> =
        DatabaseProvider.instance
            .getPlayHistoryDao()
            .getPlayHistoryFlow(uniqueKey, storageId)

    suspend fun insert(playHistoryEntity: PlayHistoryEntity) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .insert(playHistoryEntity)
        }
    }

    suspend fun delete(historyId: Int) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .delete(historyId)
        }
    }

    suspend fun deleteTypeAll(mediaTypes: List<MediaType>) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .deleteTypeAll(mediaTypes)
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getPlayHistoryDao()
                .deleteAll()
        }
    }
}
