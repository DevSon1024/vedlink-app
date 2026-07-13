package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getAllTags()
}
