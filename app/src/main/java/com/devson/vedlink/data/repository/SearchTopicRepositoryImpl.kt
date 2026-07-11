package com.devson.vedlink.data.repository

import com.devson.vedlink.data.local.dao.SearchTopicDao
import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.model.toDomain
import com.devson.vedlink.domain.model.toEntity
import com.devson.vedlink.domain.repository.SearchTopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchTopicRepositoryImpl @Inject constructor(
    private val searchTopicDao: SearchTopicDao
) : SearchTopicRepository {

    override fun getSearchTopics(): Flow<List<SearchTopic>> {
        return searchTopicDao.getAllSearchTopics().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertSearchTopic(topic: SearchTopic): Long {
        return searchTopicDao.insertSearchTopic(topic.toEntity())
    }

    override suspend fun updateSearchTopic(topic: SearchTopic) {
        searchTopicDao.updateSearchTopic(topic.toEntity())
    }

    override suspend fun deleteSearchTopic(topic: SearchTopic) {
        searchTopicDao.deleteSearchTopic(topic.toEntity())
    }
}
