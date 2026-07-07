package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import com.devson.vedlink.data.network.scraper.orchestrator.ProviderDetector
import com.devson.vedlink.domain.model.WebsiteProvider
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document

@Singleton
class ProviderSpecificExtractor @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerDetector: ProviderDetector
) : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.PROVIDER_SPECIFIC

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        val provider = providerDetector.detectProvider(url)
        return when (provider) {
            WebsiteProvider.REDDIT -> scrapeRedditJson(url)
            WebsiteProvider.INSTAGRAM -> scrapeInstagramProxy(url)
            else -> null
        }
    }

    private suspend fun scrapeRedditJson(url: String): PartialMetadata? = withContext(Dispatchers.IO) {
        val cleanUrl = try {
            val uri = URI(url)
            val path = uri.path.removeSuffix("/")
            val query = uri.query
            // Convert to JSON endpoint
            val jsonPath = if (path.endsWith(".json")) path else "$path.json"
            URI(uri.scheme, uri.authority, jsonPath, null, null).toString()
        } catch (e: Exception) {
            "$url.json"
        }

        val request = Request.Builder()
            .url(cleanUrl)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyText = response.body?.string() ?: return@withContext null

                // Reddit JSON is an array containing listing structures
                val parsedElement = JsonParser.parseString(bodyText)
                val postArray = if (parsedElement.isJsonArray) parsedElement.asJsonArray else return@withContext null
                if (postArray.size() == 0) return@withContext null

                val postObject = postArray[0].asJsonObject
                val data = postObject.getAsJsonObject("data")
                    ?.getAsJsonArray("children")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("data") ?: return@withContext null

                val title = getStringOrNull(data, "title")
                val selfText = getStringOrNull(data, "selftext")?.take(150)
                val sub = getStringOrNull(data, "subreddit_name_prefixed") ?: "Reddit"

                // Extract preview images
                var imgUrl: String? = null
                val preview = data.getAsJsonObject("preview")
                if (preview != null) {
                    val images = preview.getAsJsonArray("images")
                    if (images != null && images.size() > 0) {
                        val source = images[0].asJsonObject.getAsJsonObject("source")
                        val rawImgUrl = getStringOrNull(source, "url")
                        if (!rawImgUrl.isNullOrBlank()) {
                            imgUrl = rawImgUrl.replace("&amp;", "&")
                        }
                    }
                }

                if (imgUrl == null) {
                    val thumbnail = getStringOrNull(data, "thumbnail")
                    if (!thumbnail.isNullOrBlank() && thumbnail.startsWith("http")) {
                        imgUrl = thumbnail
                    }
                }

                return@withContext PartialMetadata(
                    extractorType = type,
                    title = title,
                    description = selfText,
                    imageUrl = imgUrl,
                    websiteName = sub
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun scrapeInstagramProxy(url: String): PartialMetadata? = withContext(Dispatchers.IO) {
        // Rewrite instagram.com to ddinstagram.com to fetch standard OG meta elements
        val proxyUrl = try {
            val uri = URI(url)
            val host = uri.host.lowercase()
            val newHost = if (host.contains("instagram.com")) {
                host.replace("instagram.com", "ddinstagram.com")
            } else if (host.contains("instagr.am")) {
                host.replace("instagr.am", "ddinstagram.com")
            } else {
                host
            }
            URI(uri.scheme, uri.authority, uri.path, uri.query, null).toString()
                .replace(host, newHost)
        } catch (e: Exception) {
            url
        }

        val request = Request.Builder()
            .url(proxyUrl)
            .get()
            .header("User-Agent", "Discordbot/2.0")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyText = response.body?.string() ?: return@withContext null
                val doc = org.jsoup.Jsoup.parse(bodyText, proxyUrl)

                val title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                val desc = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                val image = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }

                return@withContext PartialMetadata(
                    extractorType = type,
                    title = title ?: "Instagram Post",
                    description = desc,
                    imageUrl = image,
                    websiteName = "Instagram"
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getStringOrNull(obj: JsonObject, memberName: String): String? {
        val element = obj.get(memberName)
        return if (element != null && element.isJsonPrimitive) element.asString else null
    }
}
