package com.devson.vedlink.domain.usecase

import android.net.Uri
import android.util.Patterns
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.data.worker.WorkManagerHelper
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.SaveResult
import com.devson.vedlink.domain.model.SaveStatus
import javax.inject.Inject

class SaveLinkUseCase @Inject constructor(
    private val repository: LinkRepository,
    private val workManagerHelper: WorkManagerHelper
) {
    suspend operator fun invoke(url: String, checkDuplicate: Boolean = false): Result<SaveResult> {
        return try {
            val cleanUrl = cleanUrl(url)

            if (!isValidUrl(cleanUrl)) {
                return Result.failure(IllegalArgumentException("Invalid URL format"))
            }

            val existingLink = repository.getLinkByUrl(cleanUrl)
            if (existingLink != null) {
                return Result.success(
                    SaveResult(
                        linkId = existingLink.id.toLong(),
                        status = SaveStatus.ALREADY_EXISTS
                    )
                )
            }

            val domain = extractDomain(cleanUrl)
            val link = Link(
                url = cleanUrl,
                title = domain,
                description = null,
                imageUrl = null,
                domain = domain
            )

            val id = repository.insertLink(link)

            if (id > 0) {
                workManagerHelper.enqueueLinkMetadataFetch(id.toInt())
            }

            Result.success(
                SaveResult(
                    linkId = id,
                    status = SaveStatus.NEWLY_SAVED
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanUrl(url: String): String {
        var cleaned = url.trim()
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