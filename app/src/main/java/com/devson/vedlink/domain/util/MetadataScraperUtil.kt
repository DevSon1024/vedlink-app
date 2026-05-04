package com.devson.vedlink.domain.util

import com.jaincomapny.androidlinkpreview.LinkMetadataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScrapedMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

object MetadataScraperUtil {
    /**
     * Fetches link metadata using the Android-Link-Preview library.
     */
    suspend fun fetchMetadata(url: String): ScrapedMetadata = withContext(Dispatchers.IO) {
        try {
            // The library requires an instance of the parser
            val previewData = LinkMetadataParser().parse(url)
            ScrapedMetadata(
                title = previewData?.title?.takeIf { it.isNotBlank() },
                description = previewData?.description?.takeIf { it.isNotBlank() },
                imageUrl = previewData?.imageUrl?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            ScrapedMetadata()
        }
    }
}
