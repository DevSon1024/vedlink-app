package com.devson.vedlink.domain.usecase

import android.util.Patterns
import com.devson.vedlink.data.network.normalizer.UrlNormalizer
import com.devson.vedlink.data.worker.WorkManagerHelper
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.MetadataState
import com.devson.vedlink.domain.model.SaveResult
import com.devson.vedlink.domain.model.SaveStatus
import com.devson.vedlink.domain.model.WebsiteProvider
import com.devson.vedlink.domain.repository.LinkRepository
import com.devson.vedlink.domain.util.LinkExtractor
import java.net.URI
import javax.inject.Inject

class SaveLinkUseCase @Inject constructor(
    private val repository: LinkRepository,
    private val urlNormalizer: UrlNormalizer,
    private val workManagerHelper: WorkManagerHelper,
    private val linkExtractor: LinkExtractor
) {
    suspend operator fun invoke(
        url: String,
        checkDuplicate: Boolean = false,
        title: String? = null,
        description: String? = null,
        imageUrl: String? = null,
        folderId: Int? = null,
        tags: List<String> = emptyList(),
        noteTitle: String? = null,
        noteBodyMarkdown: String? = null,
        isPinned: Boolean = false,
        accentColorHex: Int? = null,
        readingTimeMinutes: Int? = null
    ): Result<SaveResult> {
        return try {
            val rawInput = url.trim()

            // Smart URL extraction using LinkExtractor to support rich notes containing both thoughts and links
            val extractedUrls = linkExtractor.extractUrls(rawInput)
            val extractedUrl = extractedUrls.firstOrNull() ?: rawInput

            // Preserve remaining text (or full text) in noteBodyMarkdown if not explicitly provided
            var finalNoteBody = noteBodyMarkdown
            if (finalNoteBody.isNullOrBlank() && extractedUrls.isNotEmpty()) {
                val urlWithoutScheme = extractedUrl.removePrefix("https://").removePrefix("http://")
                val isJustUrl = rawInput.equals(extractedUrl, ignoreCase = true) ||
                        rawInput.equals(urlWithoutScheme, ignoreCase = true)
                if (!isJustUrl) {
                    val remaining = rawInput
                        .replace(extractedUrl, "", ignoreCase = true)
                        .replace(urlWithoutScheme, "", ignoreCase = true)
                        .trim()
                    finalNoteBody = remaining.ifBlank { rawInput }
                }
            }

            val trimmedUrl = extractedUrl.trim()
            if (!isValidUrl(trimmedUrl)) {
                return Result.failure(IllegalArgumentException("Invalid URL format"))
            }

            // Normalize URL to get canonical representation
            val canonicalUrl = urlNormalizer.normalize(trimmedUrl)

            // Duplicate detection check at both original and canonical levels
            val existingLink = repository.getLinkByCanonicalUrl(canonicalUrl)
                ?: repository.getLinkByUrl(trimmedUrl)

            if (existingLink != null) {
                return Result.success(
                    SaveResult(
                        linkId = existingLink.id.toLong(),
                        status = SaveStatus.ALREADY_EXISTS
                    )
                )
            }

            val domain = extractDomain(canonicalUrl)
            
            // Build link with rich note fields and QUEUED metadata state
            val link = Link(
                url = trimmedUrl,
                canonicalUrl = canonicalUrl,
                title = title ?: domain,
                description = description,
                imageUrl = imageUrl,
                faviconUrl = null,
                domain = domain,
                provider = WebsiteProvider.GENERIC, // Background worker will classify
                folderId = folderId,
                isFavorite = false,
                isPinned = isPinned,
                isArchived = false,
                isUnread = true,
                notes = finalNoteBody,
                noteTitle = noteTitle,
                noteBodyMarkdown = finalNoteBody,
                accentColorHex = accentColorHex,
                readingTimeMinutes = readingTimeMinutes,
                metadataState = MetadataState.QUEUED,
                tags = tags
            )

            val id = repository.insertLink(link)

            if (id > 0) {
                // Enqueue WorkManager background task for metadata, favicon, and image extraction
                workManagerHelper.enqueueMetadataFetch(trimmedUrl, id.toInt())
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

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val patternMatches = try {
                Patterns.WEB_URL.matcher(url).matches()
            } catch (e: Exception) {
                url.matches(Regex("""https?://[a-zA-Z0-9.\-]+(?::\d+)?(?:/[^\s]*)?"""))
            }
            patternMatches && (url.startsWith("http://") || url.startsWith("https://"))
        } catch (e: Exception) {
            false
        }
    }
}