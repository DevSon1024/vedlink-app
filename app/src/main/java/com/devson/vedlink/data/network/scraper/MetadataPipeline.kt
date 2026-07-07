package com.devson.vedlink.data.network.scraper

import com.devson.vedlink.data.network.scraper.orchestrator.MetadataOrchestrator
import com.devson.vedlink.domain.model.ScrapedMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataPipeline @Inject constructor(
    private val orchestrator: MetadataOrchestrator
) {
    /**
     * Executes the new modular orchestrator pipeline to resolve metadata.
     */
    suspend fun resolveMetadata(url: String): ScrapedMetadata {
        return orchestrator.resolveMetadata(url)
    }
}
