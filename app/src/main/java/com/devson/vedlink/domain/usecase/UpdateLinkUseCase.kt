package com.devson.vedlink.domain.usecase

import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class UpdateLinkUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(link: Link) {
        repository.updateLink(link)
    }
}