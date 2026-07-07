package com.devson.vedlink.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.vedlink.data.network.favicon.FaviconEngine
import com.devson.vedlink.data.network.image.PreviewImageEngine
import com.devson.vedlink.data.network.normalizer.UrlNormalizer
import com.devson.vedlink.data.network.scraper.MetadataPipeline
import com.devson.vedlink.data.network.scraper.WebsiteDetector
import com.devson.vedlink.domain.model.MetadataState
import com.devson.vedlink.domain.repository.LinkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

@HiltWorker
class MetadataFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LinkRepository,
    private val urlNormalizer: UrlNormalizer,
    private val metadataPipeline: MetadataPipeline,
    private val faviconEngine: FaviconEngine,
    private val previewImageEngine: PreviewImageEngine,
    private val websiteDetector: WebsiteDetector
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val linkId = inputData.getInt(KEY_LINK_ID, -1)
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()

        if (linkId == -1) return@withContext Result.failure()

        // 1. Fetch current link from DB
        val link = repository.getLinkById(linkId) ?: return@withContext Result.failure()

        try {
            // 2. Set state to FETCHING
            repository.updateMetadataState(linkId, MetadataState.FETCHING)

            // 3. Normalize URL (resolve redirects, remove tracking, domain canonicalization)
            val canonicalUrl = urlNormalizer.normalize(url)
            val domain = extractDomain(canonicalUrl)
            val provider = websiteDetector.detectProvider(canonicalUrl)

            // Update database with intermediate normalizations
            val linkWithCanonical = link.copy(
                canonicalUrl = canonicalUrl,
                domain = domain,
                provider = provider,
                metadataState = MetadataState.PROCESSING,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateLink(linkWithCanonical)

            // 4. Resolve site metadata (title, description, raw remote image/favicon links)
            val scraped = metadataPipeline.resolveMetadata(canonicalUrl)

            // 5. Download and store favicon in L2 permanent storage
            val localFaviconPath = try {
                scraped.faviconUrl?.let { faviconEngine.getOrFetchFavicon(it) } 
                    ?: faviconEngine.getOrFetchFavicon(canonicalUrl)
            } catch (e: Exception) {
                null
            }

            // 6. Download and store preview image in local storage
            val localPreviewPath = try {
                scraped.imageUrl?.let { previewImageEngine.getOrFetchPreview(it) }
            } catch (e: Exception) {
                null
            }

            // 7. Resolve final metadata state
            // If text details are present but image download failed, state is PARTIAL, otherwise COMPLETED
            val finalState = if (scraped.title.isNullOrBlank()) {
                MetadataState.FAILED
            } else if (localPreviewPath == null && !scraped.imageUrl.isNullOrBlank()) {
                MetadataState.PARTIAL
            } else {
                MetadataState.COMPLETED
            }

            // 8. Commit final metadata to SQLite database
            val updatedLink = linkWithCanonical.copy(
                title = scraped.title ?: linkWithCanonical.title,
                description = scraped.description ?: linkWithCanonical.description,
                imageUrl = localPreviewPath ?: linkWithCanonical.imageUrl,
                faviconUrl = localFaviconPath ?: linkWithCanonical.faviconUrl,
                metadataState = finalState,
                updatedAt = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )

            repository.updateLink(updatedLink)
            Result.success()

        } catch (e: Exception) {
            val isTransient = e is IOException || 
                              e is UnknownHostException || 
                              e is SocketTimeoutException || 
                              e is ConnectException
            
            if (isTransient) {
                // Reschedule worker with exponential backoff on transient errors
                repository.updateMetadataState(linkId, MetadataState.RETRYING)
                Result.retry()
            } else {
                // Permanent failures (like SQL error or null pointers)
                repository.updateMetadataState(linkId, MetadataState.FAILED)
                Result.failure()
            }
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

    companion object {
        const val KEY_LINK_ID = "link_id"
        const val KEY_URL = "url"
        const val KEY_IS_FORCED_REFRESH = "is_forced_refresh"
        const val WORK_NAME = "metadata_fetch_worker"
    }
}