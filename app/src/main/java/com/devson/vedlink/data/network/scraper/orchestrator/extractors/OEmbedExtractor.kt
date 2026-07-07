package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document

@Singleton
class OEmbedExtractor @Inject constructor(
    private val httpClient: OkHttpClient
) : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.OEMBED

    data class OEmbedProvider(
        val hostSuffix: String,
        val endpointUrl: String
    )

    // Configurable provider registry
    private val registry = listOf(
        OEmbedProvider("youtube.com", "https://www.youtube.com/oembed"),
        OEmbedProvider("youtu.be", "https://www.youtube.com/oembed"),
        OEmbedProvider("spotify.com", "https://open.spotify.com/oembed"),
        OEmbedProvider("vimeo.com", "https://vimeo.com/api/oembed.json"),
        OEmbedProvider("flickr.com", "https://www.flickr.com/services/oembed/"),
        OEmbedProvider("soundcloud.com", "https://soundcloud.com/oembed")
    )

    fun findProvider(url: String): OEmbedProvider? {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            return null
        }
        return registry.find { host == it.hostSuffix || host.endsWith(".${it.hostSuffix}") }
    }

    override suspend fun extract(url: String, document: Document?): PartialMetadata? = withContext(Dispatchers.IO) {
        val provider = findProvider(url) ?: return@withContext null

        val httpUrl = provider.endpointUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("url", url)
            ?.addQueryParameter("format", "json")
            ?.build() ?: return@withContext null

        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyText = response.body?.string() ?: return@withContext null
                val parsed = JsonParser.parseString(bodyText).asJsonObject

                val title = getStringOrNull(parsed, "title")
                val author = getStringOrNull(parsed, "author_name")
                val siteName = getStringOrNull(parsed, "provider_name")
                val thumbnail = getStringOrNull(parsed, "thumbnail_url")

                return@withContext PartialMetadata(
                    extractorType = type,
                    title = title,
                    author = author,
                    websiteName = siteName,
                    imageUrl = thumbnail
                )
            }
        } catch (e: Exception) {
            null // Suppress and isolate failures
        }
    }

    private fun getStringOrNull(obj: JsonObject, memberName: String): String? {
        val element = obj.get(memberName)
        return if (element != null && element.isJsonPrimitive) element.asString else null
    }
}
