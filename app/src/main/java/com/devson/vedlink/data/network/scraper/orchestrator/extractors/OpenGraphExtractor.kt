package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import org.jsoup.nodes.Document
import java.net.URI

class OpenGraphExtractor : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.OPEN_GRAPH

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        if (document == null) return null

        val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
        val description = document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
        val rawImage = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
        val ogUrl = document.select("meta[property=og:url]").attr("content").takeIf { it.isNotBlank() }
        val siteName = document.select("meta[property=og:site_name]").attr("content").takeIf { it.isNotBlank() }

        val imageUrl = rawImage?.let { resolveAbsoluteUrl(url, it) }

        if (title == null && description == null && imageUrl == null && siteName == null) {
            return null
        }

        return PartialMetadata(
            extractorType = type,
            title = title,
            description = description,
            imageUrl = imageUrl,
            websiteName = siteName,
            canonicalUrl = ogUrl
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
