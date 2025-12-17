package com.devson.vedlink.domain.usecase

import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class DeleteLinkUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(link: Link): Result<Unit> {
        return try {
            repository.deleteLink(link)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
