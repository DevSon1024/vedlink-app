package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.repository.SearchTopicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchTopicsUseCase @Inject constructor(
    private val repository: SearchTopicRepository
) {
    operator fun invoke(): Flow<List<SearchTopic>> {
        return repository.getSearchTopics()
    }
}
