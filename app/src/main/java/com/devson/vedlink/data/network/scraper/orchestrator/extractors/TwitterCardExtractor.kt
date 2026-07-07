package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import org.jsoup.nodes.Document
import java.net.URI

class TwitterCardExtractor : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.TWITTER_CARD

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        if (document == null) return null

        val title = document.select("meta[name=twitter:title], meta[property=twitter:title]").attr("content").takeIf { it.isNotBlank() }
        val description = document.select("meta[name=twitter:description], meta[property=twitter:description]").attr("content").takeIf { it.isNotBlank() }
        val rawImage = document.select("meta[name=twitter:image], meta[property=twitter:image]").attr("content").takeIf { it.isNotBlank() }
        val creator = document.select("meta[name=twitter:creator], meta[property=twitter:creator]").attr("content").takeIf { it.isNotBlank() }
        val site = document.select("meta[name=twitter:site], meta[property=twitter:site]").attr("content").takeIf { it.isNotBlank() }

        val imageUrl = rawImage?.let { resolveAbsoluteUrl(url, it) }

        if (title == null && description == null && imageUrl == null) {
            return null
        }

        return PartialMetadata(
            extractorType = type,
            title = title,
            description = description,
            imageUrl = imageUrl,
            author = creator ?: site
        )
    }

    private fun resolveAbsoluteUrl(baseUr: String, relativeUrl: String): String {
        return try {
            val base = URI(baseUr)
            base.resolve(relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
