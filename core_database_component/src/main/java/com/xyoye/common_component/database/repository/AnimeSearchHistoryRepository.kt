package com.xyoye.common_component.database.repository

import androidx.lifecycle.LiveData
import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.entity.AnimeSearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnimeSearchHistoryRepository {
    fun getAll(): LiveData<MutableList<String>> =
        DatabaseProvider.instance
            .getAnimeSearchHistoryDao()
            .getAll()

    suspend fun insert(searchText: String) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getAnimeSearchHistoryDao()
                .insert(AnimeSearchHistoryEntity(searchText))
        }
    }

    suspend fun deleteByText(searchText: String) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getAnimeSearchHistoryDao()
                .deleteByText(searchText)
        }
    }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            DatabaseProvider.instance
                .getAnimeSearchHistoryDao()
                .deleteAll()
        }
    }
}
