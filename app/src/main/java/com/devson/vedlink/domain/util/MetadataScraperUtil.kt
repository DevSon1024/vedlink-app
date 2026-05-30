package com.devson.vedlink.domain.util

import com.jaincomapny.androidlinkpreview.LinkMetadataParser
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL

data class ScrapedMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

object MetadataScraperUtil {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Fetches link metadata using a multi-layered, robust strategy.
     */
    suspend fun fetchMetadata(url: String): ScrapedMetadata = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(url)
        if (sanitizedUrl.isBlank()) return@withContext ScrapedMetadata()

        // Strategy 1: YouTube Special Case
        val youtubeId = extractYouTubeVideoId(sanitizedUrl)
        if (youtubeId != null) {
            val ytMetadata = fetchYouTubeMetadata(sanitizedUrl, youtubeId)
            if (ytMetadata != null) return@withContext ytMetadata
        }

        // Strategy 2: High-Fidelity Jsoup HTML Scraper with Spoofed User-Agent
        try {
            val doc = Jsoup.connect(sanitizedUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .referrer("https://www.google.com")
                .timeout(8000)
                .followRedirects(true)
                .get()

            // Title Extraction (OG -> Twitter -> HTML tag -> H1)
            var title = doc.select("meta[property=og:title]").attr("content").trim()
            if (title.isBlank()) {
                title = doc.select("meta[name=twitter:title]").attr("content").trim()
            }
            if (title.isBlank()) {
                title = doc.title().trim()
            }
            if (title.isBlank()) {
                title = doc.select("h1").first()?.text()?.trim() ?: ""
            }

            // Description Extraction (OG -> Twitter -> Standard -> Paragraph snippet)
            var description = doc.select("meta[property=og:description]").attr("content").trim()
            if (description.isBlank()) {
                description = doc.select("meta[name=twitter:description]").attr("content").trim()
            }
            if (description.isBlank()) {
                description = doc.select("meta[name=description]").attr("content").trim()
            }
            if (description.isBlank()) {
                description = doc.select("p").first()?.text()?.take(200)?.trim() ?: ""
            }

            // Image URL Extraction (OG -> Twitter -> Apple Touch Icon -> Favicon)
            var imageUrl = doc.select("meta[property=og:image]").attr("content").trim()
            if (imageUrl.isBlank()) {
                imageUrl = doc.select("meta[name=twitter:image]").attr("content").trim()
            }
            if (imageUrl.isBlank()) {
                imageUrl = doc.select("link[rel=apple-touch-icon]").attr("href").trim()
            }
            if (imageUrl.isBlank()) {
                imageUrl = doc.select("link[rel*=icon]").attr("href").trim()
            }

            val domain = extractDomain(sanitizedUrl)
            val absoluteImageUrl = if (imageUrl.isNotBlank()) {
                resolveAbsoluteUrl(sanitizedUrl, imageUrl)
            } else {
                "https://www.google.com/s2/favicons?domain=$domain&sz=128"
            }

            if (title.isNotBlank()) {
                return@withContext ScrapedMetadata(
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    imageUrl = absoluteImageUrl
                )
            }
        } catch (e: Exception) {
            // Fall through to next strategy on Jsoup exceptions
        }

        // Strategy 3: Library Fallback (Android-Link-Preview)
        try {
            val previewData = LinkMetadataParser().parse(sanitizedUrl)
            if (previewData != null && !previewData.title.isNullOrBlank()) {
                val domain = extractDomain(sanitizedUrl)
                val img = previewData.imageUrl?.takeIf { it.isNotBlank() } ?: "https://www.google.com/s2/favicons?domain=$domain&sz=128"
                return@withContext ScrapedMetadata(
                    title = previewData.title,
                    description = previewData.description?.takeIf { it.isNotBlank() },
                    imageUrl = img
                )
            }
        } catch (e: Exception) {
            // Fall through to next strategy on library exceptions
        }

        // Strategy 4: Intelligent URL Parsing Fallback (Guarantees elegant title formatting offline)
        return@withContext generateFallbackMetadata(sanitizedUrl)
    }

    private fun sanitizeUrl(url: String): String {
        var cleaned = url.trim()
        if (cleaned.isBlank()) return ""
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "https://$cleaned"
        }
        return cleaned
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val pattern = "^(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_\\-]{11})".toRegex()
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
    }

    private fun fetchYouTubeMetadata(url: String, videoId: String): ScrapedMetadata? {
        val domain = "youtube.com"
        val thumbnail = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
        
        return try {
            val oEmbedUrl = "https://www.youtube.com/oembed?url=$url&format=json"
            val request = Request.Builder().url(oEmbedUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JsonParser.parseString(body).asJsonObject
                        val title = json.get("title")?.asString
                        val author = json.get("author_name")?.asString
                        ScrapedMetadata(
                            title = title ?: "YouTube Video",
                            description = if (!author.isNullOrBlank()) "Video by $author" else "YouTube Video",
                            imageUrl = thumbnail
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            // Fallback immediately to constructed parameters
            ScrapedMetadata(
                title = "YouTube Video",
                description = "Watch this video on YouTube",
                imageUrl = thumbnail
            )
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: "link"
        } catch (e: Exception) {
            "link"
        }
    }

    private fun resolveAbsoluteUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else {
                val base = URL(baseUrl)
                URL(base, relativeUrl).toString()
            }
        } catch (e: Exception) {
            relativeUrl
        }
    }

    private fun generateFallbackMetadata(url: String): ScrapedMetadata {
        val domain = extractDomain(url)
        val cleanDomain = domain.substringBefore(".").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val pathTitle = try {
            val uri = URI(url)
            val path = uri.path ?: ""
            val lastSegment = path.split("/").lastOrNull { it.isNotBlank() }
            if (lastSegment != null && !lastSegment.contains(".")) {
                lastSegment
                    .replace("-", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { segment ->
                        segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
            } else null
        } catch (e: Exception) {
            null
        }

        val formattedTitle = if (!pathTitle.isNullOrBlank()) {
            "$cleanDomain - $pathTitle"
        } else {
            cleanDomain
        }

        return ScrapedMetadata(
            title = formattedTitle,
            description = "Saved from $domain",
            imageUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=128"
        )
    }
}
