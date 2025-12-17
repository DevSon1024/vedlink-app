package com.devson.vedlink.domain.usecase

import android.net.Uri
import android.util.Patterns
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.data.worker.WorkManagerHelper
import com.devson.vedlink.domain.model.Link
import javax.inject.Inject

class SaveLinkUseCase @Inject constructor(
    private val repository: LinkRepository,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(url: String): Result<Long> {
        return try {
            // Clean and validate URL
            val cleanUrl = cleanUrl(url)

            if (!isValidUrl(cleanUrl)) {
                return Result.failure(IllegalArgumentException("Invalid URL format"))
            }

            // Check if link already exists
            val existingLink = repository.getLinkByUrl(cleanUrl)
            if (existingLink != null) {
                return Result.success(existingLink.id.toLong())
            }

            val domain = extractDomain(cleanUrl)
            val link = Link(
                url = cleanUrl,
                title = domain, // Temporary title until metadata is fetched
                description = null,
                imageUrl = null,
                domain = domain
            )

            val id = repository.insertLink(link)

            // Only enqueue metadata fetch if link was actually inserted
            if (id > 0) {
                workManagerHelper.enqueueLinkMetadataFetch(id.toInt())
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanUrl(url: String): String {
        var cleaned = url.trim()

        // Remove common tracking parameters
        cleaned = cleaned.split("?").firstOrNull() ?: cleaned

        // Add https:// if no scheme is present
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "https://$cleaned"
        }
        return cleaned
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host?.replace("www.", "") ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            Patterns.WEB_URL.matcher(url).matches() &&
                    (url.startsWith("http://") || url.startsWith("https://"))
        } catch (e: Exception) {
            false
        }
    }
}
