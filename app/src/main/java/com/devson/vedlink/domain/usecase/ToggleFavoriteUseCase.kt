package com.devson.vedlink.domain.usecase

import com.devson.vedlink.data.repository.LinkRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(id: Int, isFavorite: Boolean): Result<Unit> {
        return try {
            repository.toggleFavorite(id, isFavorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
