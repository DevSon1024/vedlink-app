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
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
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

            // --- AGGRESSIVE LOCAL CACHE CHECK ---
            // If we already have good metadata and this is not a forced refresh, skip all network calls.
            val isForcedRefresh = inputData.getBoolean(KEY_IS_FORCED_REFRESH, false)
            if (!isForcedRefresh &&
                !link.title.isNullOrBlank() &&
                !link.imageUrl.isNullOrBlank() &&
                link.title != link.url
            ) {
                return@withContext Result.success()
            }

            // --- HYBRID SCRAPER STRATEGY ---
            // 1. YouTube  → OpenGraph HTML scrape → Microlink fallback
            // 2. Reddit   → Native JSON API
            // 3. Others   → Generic Jsoup OpenGraph scrape → Microlink fallback (last resort)
            val metadata = when {
                isYouTubeUrl(link.url) -> fetchYouTubeMetadata(link.url)
                isRedditUrl(link.url)  -> fetchRedditMetadata(link.url)
                else -> {
                    // Phase 1: Try Jsoup generic scraper (free, no rate limits)
                    val genericMetadata = fetchGenericOpenGraphMetadata(link.url)
                    if (!genericMetadata.title.isNullOrBlank()) {
                        genericMetadata
                    } else {
                        // Phase 2 (last resort): Microlink API — only when Jsoup fails
                        fetchMicrolinkMetadata(link.url)
                    }
                }
            }

            // Calculate domain if missing
            val domain = if (link.domain.isNullOrBlank()) {
                extractDomain(link.url)
            } else {
                link.domain
            }

            // Update link with metadata — only overwrite if we actually found new data
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

    // --- GENERIC JSOUP OPEN GRAPH SCRAPER ---
    // Uses Jsoup directly (no external API calls, no rate limits).
    // Prioritizes og: tags → Twitter card tags → standard HTML tags.
    // Returns an empty LinkMetadata if the site blocks scraping (IOException).
    private suspend fun fetchGenericOpenGraphMetadata(url: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get()

                // Title: og:title → twitter:title → <title>
                val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.title().takeIf { it.isNotBlank() }

                // Description: og:description → twitter:description → meta[name=description]
                val description =
                    document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }

                // Image: og:image → twitter:image
                val imageUrl =
                    document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }

                LinkMetadata(title, description, imageUrl)
            } catch (e: IOException) {
                // Site blocked the scraper or network error — signal caller to use fallback
                LinkMetadata()
            } catch (e: Exception) {
                e.printStackTrace()
                LinkMetadata()
            }
        }
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

    // Fetch OpenGraph metadata from HTML via OkHttp + Jsoup (used for YouTube)
    private suspend fun fetchOpenGraphMetadata(url: String): LinkMetadata {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val html = response.body?.string() ?: return@withContext LinkMetadata()

                val document = Jsoup.parse(html)

                val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.title().takeIf { it.isNotBlank() }

                val description =
                    document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }

                val imageUrl =
                    document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                        ?: document.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }

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
            val gson = Gson()
            val listType = object : TypeToken<List<RedditResponse>>() {}.type
            val responses: List<RedditResponse> = gson.fromJson(jsonData, listType)

            val postData = responses.firstOrNull()?.data?.children?.firstOrNull()?.data
                ?: return LinkMetadata()

            val title = postData.title
            val description = postData.selftext?.takeIf { it.isNotBlank() }
                ?: postData.subredditNamePrefixed

            var imageUrl = ""
            val urlOverridden = postData.urlOverriddenByDest ?: ""

            if (urlOverridden.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)(?:\\?.*)?$"))) {
                imageUrl = urlOverridden
            } else {
                val sourceUrl = postData.preview?.images?.firstOrNull()?.source?.url
                if (!sourceUrl.isNullOrBlank()) {
                    imageUrl = sourceUrl.replace("&amp;", "&")
                }
            }

            if (imageUrl.isBlank()) {
                val thumb = postData.thumbnail ?: ""
                if (thumb.startsWith("http")) {
                    imageUrl = thumb
                }
            }

            LinkMetadata(title, description, imageUrl)
        } catch (e: Exception) {
            LinkMetadata()
        }
    }

    // --- MICROLINK (Ultimate Fallback — rate-limited: 50 req/day) ---
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
                            title = data.optString("title").takeIf { it.isNotBlank() },
                            description = data.optString("description").takeIf { it.isNotBlank() },
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
        /** Pass `true` to bypass the local cache and force a fresh network fetch. */
        const val KEY_IS_FORCED_REFRESH = "is_forced_refresh"
        const val WORK_NAME = "metadata_fetch_worker"
    }
}

data class LinkMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

// Reddit JSON Data Classes for Gson
data class RedditResponse(val data: RedditData?)
data class RedditData(val children: List<RedditChild>?)
data class RedditChild(val data: RedditPost?)
data class RedditPost(
    val title: String?,
    val selftext: String?,
    @SerializedName("subreddit_name_prefixed") val subredditNamePrefixed: String?,
    @SerializedName("url_overridden_by_dest") val urlOverriddenByDest: String?,
    val thumbnail: String?,
    val preview: RedditPreview?
)
data class RedditPreview(val images: List<RedditImage>?)
data class RedditImage(val source: RedditSource?)
data class RedditSource(val url: String?)