package com.xyoye.common_component.database.repository

import com.xyoye.common_component.database.DatabaseProvider
import com.xyoye.data_component.entity.EpisodeHistoryEntity
import kotlinx.coroutines.flow.Flow

object EpisodeHistoryRepository {
    fun getEpisodeHistory(episodeIds: List<String>): Flow<List<EpisodeHistoryEntity>> =
        DatabaseProvider.instance
            .getPlayHistoryDao()
            .getEpisodeHistory(episodeIds)
}
