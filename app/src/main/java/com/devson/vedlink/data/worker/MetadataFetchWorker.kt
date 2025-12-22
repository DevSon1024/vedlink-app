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
            // 1. YouTube: Try OpenGraph + Microlink
            // 2. Reddit: Use Native JSON API
            // 3. Others: Use Microlink API
            val metadata = when {
                isYouTubeUrl(link.url) -> fetchYouTubeMetadata(link.url)
                isRedditUrl(link.url) -> fetchRedditMetadata(link.url)
                else -> fetchMicrolinkMetadata(link.url)
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
                title = if (!metadata.title.isNullOrBlank() && metadata.title != "- YouTube") {
                    metadata.title
                } else {
                    link.title
                },
                description = if (!metadata.description.isNullOrBlank()) {
                    metadata.description
                } else {
                    link.description
                },
                imageUrl = if (!metadata.imageUrl.isNullOrBlank()) {
                    metadata.imageUrl
                } else {
                    link.imageUrl
                },
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

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com", ignoreCase = true) ||
                url.contains("youtu.be", ignoreCase = true)
    }

    private fun isRedditUrl(url: String): Boolean {
        return url.contains("reddit.com", ignoreCase = true) ||
                url.contains("redd.it", ignoreCase = true)
    }

    // --- YOUTUBE METADATA FETCHING ---
    private suspend fun fetchYouTubeMetadata(url: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                // Method 1: Try direct HTML scraping for OpenGraph tags
                val htmlMetadata = fetchOpenGraphMetadata(url)

                // If we got a good title (not just "- YouTube"), return it
                if (!htmlMetadata.title.isNullOrBlank() &&
                    !htmlMetadata.title.endsWith("- YouTube") &&
                    htmlMetadata.title != "YouTube") {
                    return@withContext htmlMetadata
                }

                // Method 2: Fallback to Microlink
                val microlinkMetadata = fetchMicrolinkMetadata(url)

                // Clean up the title if it ends with "- YouTube"
                val cleanTitle = microlinkMetadata.title?.let { title ->
                    when {
                        title.endsWith(" - YouTube") -> title.substringBeforeLast(" - YouTube").trim()
                        title == "YouTube" -> null
                        else -> title
                    }
                }

                microlinkMetadata.copy(title = cleanTitle)
            } catch (e: Exception) {
                e.printStackTrace()
                LinkMetadata()
            }
        }
    }

    // Fetch OpenGraph metadata from HTML
    private suspend fun fetchOpenGraphMetadata(url: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val html = response.body?.string() ?: return@withContext LinkMetadata()

                // Extract OpenGraph tags
                val title = extractMetaTag(html, "og:title")
                    ?: extractMetaTag(html, "twitter:title")
                    ?: extractTitleTag(html)

                val description = extractMetaTag(html, "og:description")
                    ?: extractMetaTag(html, "twitter:description")
                    ?: extractMetaTag(html, "description")

                val imageUrl = extractMetaTag(html, "og:image")
                    ?: extractMetaTag(html, "twitter:image")

                // Clean YouTube title
                val cleanTitle = title?.let {
                    when {
                        it.endsWith(" - YouTube") -> it.substringBeforeLast(" - YouTube").trim()
                        it == "YouTube" -> null
                        else -> it
                    }
                }

                LinkMetadata(cleanTitle, description, imageUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                LinkMetadata()
            }
        }
    }

    private fun extractMetaTag(html: String, property: String): String? {
        // Try og: property
        val ogRegex = """<meta[^>]*property=["']$property["'][^>]*content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        ogRegex.find(html)?.groupValues?.getOrNull(1)?.let { return it }

        // Try name attribute
        val nameRegex = """<meta[^>]*name=["']$property["'][^>]*content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        nameRegex.find(html)?.groupValues?.getOrNull(1)?.let { return it }

        // Try reversed order (content before property/name)
        val reversedRegex = """<meta[^>]*content=["']([^"']+)["'][^>]*property=["']$property["']""".toRegex(RegexOption.IGNORE_CASE)
        reversedRegex.find(html)?.groupValues?.getOrNull(1)?.let { return it }

        return null
    }

    private fun extractTitleTag(html: String): String? {
        val titleRegex = """<title>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE)
        return titleRegex.find(html)?.groupValues?.getOrNull(1)?.trim()
    }

    // --- REDDIT METADATA ---
    private suspend fun fetchRedditMetadata(originalUrl: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                // Resolve redirects
                var finalUrl = originalUrl
                try {
                    val headRequest = Request.Builder()
                        .url(originalUrl)
                        .head()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()

                    val response = okHttpClient.newCall(headRequest).execute()
                    finalUrl = response.request.url.toString()
                    response.close()
                } catch (e: Exception) {
                    // Continue with original URL
                }

                // Construct .json URL
                val uri = URI(finalUrl)
                val path = uri.path.trimEnd('/')
                val jsonUrl = "https://${uri.host}$path.json"

                val request = Request.Builder()
                    .url(jsonUrl)
                    .header("User-Agent", "android:com.devson.vedlink:v1.0.0")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val jsonData = response.body?.string() ?: return@withContext LinkMetadata()

                if (jsonData.trim().startsWith("[")) {
                    parseRedditPostJson(jsonData)
                } else {
                    fetchMicrolinkMetadata(originalUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fetchMicrolinkMetadata(originalUrl)
            }
        }
    }

    private fun parseRedditPostJson(jsonData: String): LinkMetadata {
        return try {
            val jsonArray = JSONArray(jsonData)
            val postData = jsonArray.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")

            val title = postData.optString("title")

            val description = postData.optString("selftext").takeIf { it.isNotBlank() }
                ?: postData.optString("subreddit_name_prefixed")

            // Image extraction
            var imageUrl = ""
            val urlOverridden = postData.optString("url_overridden_by_dest")

            if (urlOverridden.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$"))) {
                imageUrl = urlOverridden
            } else {
                val preview = postData.optJSONObject("preview")
                val images = preview?.optJSONArray("images")
                if (images != null && images.length() > 0) {
                    val source = images.getJSONObject(0).optJSONObject("source")
                    imageUrl = source?.optString("url") ?: ""
                    imageUrl = imageUrl.replace("&amp;", "&")
                }
            }

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

    // --- MICROLINK (General) ---
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