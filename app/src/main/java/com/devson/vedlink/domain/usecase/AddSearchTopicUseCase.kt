package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.repository.SearchTopicRepository
import javax.inject.Inject

class AddSearchTopicUseCase @Inject constructor(
    private val repository: SearchTopicRepository
) {
    suspend operator fun invoke(query: String): Long {
        if (query.isBlank()) {
            throw IllegalArgumentException("Search topic query cannot be empty")
        }
        val topic = SearchTopic(query = query.trim())
        return repository.insertSearchTopic(topic)
    }
}
