package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.repository.LinkRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(id: Int, isFavorite: Boolean) {
        repository.toggleFavorite(id, !isFavorite)
    }
}