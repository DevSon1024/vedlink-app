package com.devson.vedlink.data.network.scraper.orchestrator

import org.jsoup.nodes.Document

interface MetadataExtractor {
    val type: ExtractorType
    suspend fun extract(url: String, document: Document?): PartialMetadata?
}
