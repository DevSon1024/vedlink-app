package com.devson.vedlink.domain.usecase

import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class GetLinkByIdUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(id: Int): Result<Link> {
        return try {
            val link = repository.getLinkById(id)
            if (link != null) {
                Result.success(link)
            } else {
                Result.failure(Exception("Link not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
