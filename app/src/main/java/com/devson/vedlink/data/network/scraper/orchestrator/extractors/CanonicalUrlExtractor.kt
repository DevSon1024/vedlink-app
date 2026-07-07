package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import org.jsoup.nodes.Document

class CanonicalUrlExtractor : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.CANONICAL_URL

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        if (document == null) return null

        val canonical = document.select("link[rel=canonical]").attr("href").takeIf { it.isNotBlank() }
        val ogUrl = document.select("meta[property=og:url]").attr("content").takeIf { it.isNotBlank() }
        val resolved = canonical ?: ogUrl ?: return null

        return PartialMetadata(
            extractorType = type,
            canonicalUrl = resolved
        )
    }
}
