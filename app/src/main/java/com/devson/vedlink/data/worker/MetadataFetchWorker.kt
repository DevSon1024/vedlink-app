package com.devson.vedlink.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.vedlink.data.local.dao.LinkDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

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

            // HYBRID STRATEGY:
            // 1. Reddit: Use Native JSON API (Bypasses NSFW blocks/Interstitials)
            // 2. Others: Use Microlink API (Works best for Insta/YouTube/General)
            val metadata = if (isRedditUrl(link.url)) {
                fetchRedditMetadata(link.url)
            } else {
                fetchMicrolinkMetadata(link.url)
            }

            // Calculate domain if missing
            val domain = if (link.domain.isNullOrBlank()) {
                extractDomain(link.url)
            } else {
                link.domain
            }

            // Update link with metadata
            // Only overwrite if we actually found new data
            val updatedLink = link.copy(
                title = if (!metadata.title.isNullOrBlank()) metadata.title else link.title,
                description = if (!metadata.description.isNullOrBlank()) metadata.description else link.description,
                imageUrl = if (!metadata.imageUrl.isNullOrBlank()) metadata.imageUrl else link.imageUrl,
                domain = domain,
                updatedAt = System.currentTimeMillis()
            )

            linkDao.updateLink(updatedLink)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun isRedditUrl(url: String): Boolean {
        return url.contains("reddit.com", ignoreCase = true) || url.contains("redd.it", ignoreCase = true)
    }

    // --- STRATEGY 1: NATIVE REDDIT JSON ---
    private suspend fun fetchRedditMetadata(originalUrl: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Resolve Redirects (Fixes "Share" links like .../s/...)
                // We use a lightweight HEAD request to let OkHttp follow redirects to the final URL
                var finalUrl = originalUrl
                try {
                    val headRequest = Request.Builder()
                        .url(originalUrl)
                        .head()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    val response = okHttpClient.newCall(headRequest).execute()
                    finalUrl = response.request.url.toString()
                    response.close()
                } catch (e: Exception) {
                    // If HEAD fails, proceed with original URL
                }

                // 2. Construct Clean .json URL
                val uri = URI(finalUrl)
                // Remove query params (?) and ensure no double slash, then append .json
                val path = uri.path.trimEnd('/')
                val jsonUrl = "https://${uri.host}$path.json"

                // 3. Fetch JSON
                val request = Request.Builder()
                    .url(jsonUrl)
                    .header("User-Agent", "android:com.devson.vedlink:v1.0.0") // Unique UA prevents blocking
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val jsonData = response.body?.string() ?: return@withContext LinkMetadata()

                // 4. Parse (Handle Array [Post] vs Object [Subreddit/Error])
                if (jsonData.trim().startsWith("[")) {
                    parseRedditPostJson(jsonData)
                } else {
                    // If it's not a post array, fallback to Microlink (e.g., subreddit main page)
                    fetchMicrolinkMetadata(originalUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Final fallback
                fetchMicrolinkMetadata(originalUrl)
            }
        }
    }

    private fun parseRedditPostJson(jsonData: String): LinkMetadata {
        return try {
            val jsonArray = JSONArray(jsonData)
            // Post data is always at index 0 -> data -> children -> 0 -> data
            val postData = jsonArray.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")

            val title = postData.optString("title")

            // Description from 'selftext' or subreddit name
            val description = postData.optString("selftext").takeIf { it.isNotBlank() }
                ?: postData.optString("subreddit_name_prefixed")

            // Smart Image Extraction
            var imageUrl = ""
            val urlOverridden = postData.optString("url_overridden_by_dest")

            // 1. Direct Image Link
            if (urlOverridden.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$"))) {
                imageUrl = urlOverridden
            } else {
                // 2. Preview Image (Best Quality)
                val preview = postData.optJSONObject("preview")
                val images = preview?.optJSONArray("images")
                if (images != null && images.length() > 0) {
                    val source = images.getJSONObject(0).optJSONObject("source")
                    imageUrl = source?.optString("url") ?: ""
                    imageUrl = imageUrl.replace("&amp;", "&") // Fix HTML encoding
                }
            }

            // 3. Thumbnail Fallback
            if (imageUrl.isBlank()) {
                val thumb = postData.optString("thumbnail")
                if (thumb.startsWith("http")) {
                    imageUrl = thumb
                }
            }

            LinkMetadata(title, description, imageUrl)
        } catch (e: Exception) {
            LinkMetadata()
        }
    }

    // --- STRATEGY 2: MICROLINK (For Instagram, YouTube, etc.) ---
    private suspend fun fetchMicrolinkMetadata(targetUrl: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.microlink.io?url=$targetUrl"

                val request = Request.Builder().url(apiUrl).build()
                val response = okHttpClient.newCall(request).execute()
                val jsonResponse = response.body?.string()

                if (response.isSuccessful && !jsonResponse.isNullOrBlank()) {
                    val jsonObject = JSONObject(jsonResponse)
                    if (jsonObject.optString("status") == "success") {
                        val data = jsonObject.getJSONObject("data")

                        LinkMetadata(
                            title = data.optString("title"),
                            description = data.optString("description"),
                            imageUrl = data.optJSONObject("image")?.optString("url")
                        )
                    } else {
                        LinkMetadata()
                    }
                } else {
                    LinkMetadata()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LinkMetadata()
            }
        }
    }

    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host
            if (host != null && host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: Exception) {
            try {
                val domain = url.substringAfter("://").substringBefore("/")
                if (domain.startsWith("www.")) domain.substring(4) else domain
            } catch (e2: Exception) {
                null
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