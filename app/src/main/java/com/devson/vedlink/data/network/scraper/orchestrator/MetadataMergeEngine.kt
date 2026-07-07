package com.devson.vedlink.data.network.scraper.orchestrator

import com.devson.vedlink.domain.model.ScrapedMetadata
import java.net.URI

class MetadataMergeEngine {

    fun merge(partials: List<PartialMetadata>, domainFallback: String, providerName: String?): ScrapedMetadata {
        val titleResult = selectField(
            partials = partials,
            priority = listOf(
                ExtractorType.OPEN_GRAPH to 1.0,
                ExtractorType.TWITTER_CARD to 0.9,
                ExtractorType.JSON_LD to 0.8,
                ExtractorType.HTML_META to 0.6 // Represents standard HTML title
            ),
            getter = { it.title },
            fallback = domainFallback,
            fallbackConfidence = 0.1
        )

        // Standard HTML Meta extractor might return firstParagraph or main heading as fallbacks
        val descriptionResult = selectField(
            partials = partials,
            priority = listOf(
                ExtractorType.OPEN_GRAPH to 1.0,
                ExtractorType.TWITTER_CARD to 0.9,
                ExtractorType.JSON_LD to 0.8,
                ExtractorType.HTML_META to 0.6 // HTML description
            ),
            getter = { it.description ?: it.firstParagraph },
            fallback = "Saved from $domainFallback",
            fallbackConfidence = 0.1
        )

        val imageResult = selectField(
            partials = partials,
            priority = listOf(
                ExtractorType.OPEN_GRAPH to 1.0,
                ExtractorType.TWITTER_CARD to 0.9,
                ExtractorType.JSON_LD to 0.8,
                ExtractorType.HTML_META to 0.5 // Represents largestArticleImage or logo
            ),
            getter = { it.imageUrl ?: it.largestArticleImage ?: it.websiteLogo },
            fallback = null,
            fallbackConfidence = 0.0
        )

        val websiteNameResult = selectField(
            partials = partials,
            priority = listOf(
                ExtractorType.OPEN_GRAPH to 1.0,
                ExtractorType.JSON_LD to 0.9,
                ExtractorType.PROVIDER_SPECIFIC to 0.7
            ),
            getter = { it.websiteName },
            fallback = providerName ?: domainFallback,
            fallbackConfidence = 0.2
        )

        val faviconResult = selectField(
            partials = partials,
            priority = listOf(
                ExtractorType.OPEN_GRAPH to 1.0,
                ExtractorType.JSON_LD to 0.9,
                ExtractorType.HTML_META to 0.7,
                ExtractorType.OEMBED to 0.8
            ),
            getter = { it.faviconUrl },
            fallback = null,
            fallbackConfidence = 0.0
        )

        // Log merge decisions with confidence scores
        println("[MetadataMergeEngine] Decisions & Confidence:")
        println("  Title: '${titleResult.value}' (Confidence: ${titleResult.confidence})")
        println("  Description: '${descriptionResult.value}' (Confidence: ${descriptionResult.confidence})")
        println("  Preview Image: '${imageResult.value}' (Confidence: ${imageResult.confidence})")
        println("  Website Name: '${websiteNameResult.value}' (Confidence: ${websiteNameResult.confidence})")

        return ScrapedMetadata(
            title = titleResult.value,
            description = descriptionResult.value,
            imageUrl = imageResult.value,
            faviconUrl = faviconResult.value ?: partials.firstOrNull { !it.faviconUrl.isNullOrBlank() }?.faviconUrl,
            websiteName = websiteNameResult.value
        )
    }

    private fun <T> selectField(
        partials: List<PartialMetadata>,
        priority: List<Pair<ExtractorType, Double>>,
        getter: (PartialMetadata) -> T?,
        fallback: T?,
        fallbackConfidence: Double
    ): MergeResult<T> {
        for ((type, confidence) in priority) {
            val partial = partials.find { it.extractorType == type }
            if (partial != null) {
                val value = getter(partial)
                if (value != null && (value !is String || value.isNotBlank())) {
                    return MergeResult(value, confidence)
                }
            }
        }
        return MergeResult(fallback, fallbackConfidence)
    }

    private data class MergeResult<T>(
        val value: T?,
        val confidence: Double
    )
}
