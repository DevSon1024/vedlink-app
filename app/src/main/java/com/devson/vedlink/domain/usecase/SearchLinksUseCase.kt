package com.devson.vedlink.domain.usecase

import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchLinksUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    operator fun invoke(query: String): Flow<List<Link>> {
        return repository.searchLinks(query)
    }
}
