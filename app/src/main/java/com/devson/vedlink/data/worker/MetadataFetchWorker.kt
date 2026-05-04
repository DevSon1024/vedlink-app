package com.devson.vedlink.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.vedlink.data.repository.LinkRepository
import com.devson.vedlink.domain.util.MetadataScraperUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

@HiltWorker
class MetadataFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LinkRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val linkId = inputData.getInt(KEY_LINK_ID, -1)
            val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
            
            if (linkId == -1) return@withContext Result.failure()

            val link = repository.getLinkById(linkId) ?: return@withContext Result.failure()

            val isForcedRefresh = inputData.getBoolean(KEY_IS_FORCED_REFRESH, false)
            if (!isForcedRefresh && !link.title.isNullOrBlank() && !link.imageUrl.isNullOrBlank()) {
                return@withContext Result.success()
            }

            // Fetch metadata using the unified utility
            val metadata = MetadataScraperUtil.fetchMetadata(url)

            val domain = link.domain?.takeIf { it.isNotBlank() } ?: extractDomain(url) ?: ""

            val updatedLink = link.copy(
                title = metadata.title ?: link.title,
                description = metadata.description ?: link.description,
                imageUrl = metadata.imageUrl ?: link.imageUrl,
                domain = domain,
                updatedAt = System.currentTimeMillis()
            )

            repository.updateLink(updatedLink)
            Result.success()
        } catch (e: Exception) {
            // Log error if needed, and return failure or retry
            Result.retry()
        }
    }

    private fun extractDomain(url: String): String? {
        return try {
            URI(url).host?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val KEY_LINK_ID = "link_id"
        const val KEY_URL = "url"
        const val KEY_IS_FORCED_REFRESH = "is_forced_refresh"
        const val WORK_NAME = "metadata_fetch_worker"
    }
}