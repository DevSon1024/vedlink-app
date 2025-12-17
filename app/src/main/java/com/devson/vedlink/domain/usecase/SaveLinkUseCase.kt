package com.devson.vedlink.domain.usecase

import android.net.Uri
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class SaveLinkUseCase @Inject constructor(
    private val repository: LinkRepository
) {
    suspend operator fun invoke(url: String): Result<Long> {
        return try {
            val domain = extractDomain(url)
            val link = Link(
                url = url,
                title = null, // Will be fetched by worker
                description = null,
                imageUrl = null,
                domain = domain
            )
            val id = repository.insertLink(link)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
