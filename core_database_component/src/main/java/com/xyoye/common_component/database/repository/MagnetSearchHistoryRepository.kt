package com.xyoye.common_component.database.repository

import androidx.lifecycle.LiveData
import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.entity.MagnetSearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MagnetSearchHistoryRepository {
    fun getAll(): LiveData<MutableList<String>> =
        DatabaseProvider.instance
            .getMagnetSearchHistoryDao()
            .getAll()

    suspend fun insert(searchText: String) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMagnetSearchHistoryDao()
                .insert(MagnetSearchHistoryEntity(searchText))
        }
    }

    suspend fun deleteByText(searchText: String) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMagnetSearchHistoryDao()
                .deleteByText(searchText)
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getMagnetSearchHistoryDao()
                .deleteAll()
        }
    }
}
