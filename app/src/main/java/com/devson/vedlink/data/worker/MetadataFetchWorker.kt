package com.devson.vedlink.data.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.entity.LinkEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

@HiltWorker
class MetadataFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val linkDao: LinkDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val linkId = inputData.getInt(KEY_LINK_ID, -1)
            if (linkId == -1) return@withContext Result.failure()

            val link = linkDao.getLinkById(linkId) ?: return@withContext Result.failure()

            // Fetch metadata
            val metadata = fetchMetadata(link.url)

            // Update link with metadata
            val updatedLink = link.copy(
                title = metadata.title ?: link.title,
                description = metadata.description ?: link.description,
                imageUrl = metadata.imageUrl ?: link.imageUrl,
                updatedAt = System.currentTimeMillis()
            )

            linkDao.updateLink(updatedLink)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchMetadata(url: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val html = response.body?.string() ?: return@withContext LinkMetadata()

                val document = Jsoup.parse(html)

                // Extract metadata
                val title = document.select("meta[property=og:title]").attr("content")
                    .takeIf { it.isNotBlank() }
                    ?: document.select("title").text()
                    ?: ""

                val description = document.select("meta[property=og:description]").attr("content")
                    .takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=description]").attr("content")
                    ?: ""

                val imageUrl = document.select("meta[property=og:image]").attr("content")
                    .takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:image]").attr("content")
                    ?: ""

                LinkMetadata(
                    title = title.takeIf { it.isNotBlank() },
                    description = description.takeIf { it.isNotBlank() },
                    imageUrl = imageUrl.takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                LinkMetadata()
            }
        }
    }

    companion object {
        const val KEY_LINK_ID = "link_id"
        const val WORK_NAME = "metadata_fetch_worker"
    }
}

data class LinkMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)