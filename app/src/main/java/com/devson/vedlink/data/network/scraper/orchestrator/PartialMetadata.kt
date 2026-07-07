package com.devson.vedlink.data.network.scraper.orchestrator

data class PartialMetadata(
    val extractorType: ExtractorType,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val faviconUrl: String? = null,
    val websiteName: String? = null,
    val author: String? = null,
    val language: String? = null,
    val themeColor: String? = null,
    val publishedDate: String? = null,
    val modifiedDate: String? = null,
    val canonicalUrl: String? = null,
    val firstParagraph: String? = null,
    val websiteLogo: String? = null,
    val largestArticleImage: String? = null
)
