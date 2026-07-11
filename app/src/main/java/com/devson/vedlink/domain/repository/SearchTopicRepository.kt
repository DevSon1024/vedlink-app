package com.devson.vedlink.domain.repository

import com.devson.vedlink.domain.model.SearchTopic
import kotlinx.coroutines.flow.Flow

interface SearchTopicRepository {
    fun getSearchTopics(): Flow<List<SearchTopic>>
    suspend fun insertSearchTopic(topic: SearchTopic): Long
    suspend fun updateSearchTopic(topic: SearchTopic)
    suspend fun deleteSearchTopic(topic: SearchTopic)
}
