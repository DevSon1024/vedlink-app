package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.SearchTopic
import com.devson.vedlink.domain.repository.SearchTopicRepository
import javax.inject.Inject

class DeleteSearchTopicUseCase @Inject constructor(
    private val repository: SearchTopicRepository
) {
    suspend operator fun invoke(topic: SearchTopic) {
        repository.deleteSearchTopic(topic)
    }
}
