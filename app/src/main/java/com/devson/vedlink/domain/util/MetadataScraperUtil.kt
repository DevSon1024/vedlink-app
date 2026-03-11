package com.devson.vedlink.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

data class MinimalMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

object MetadataScraperUtil {
    
    suspend fun fetchFallbackMetadata(url: String): MinimalMetadata {
        return withContext(Dispatchers.IO) {
            try {
                // Using the spoofed WhatsApp User-Agent
                val document = Jsoup.connect(url)
                    .userAgent("WhatsApp/2.21.19.21 A")
                    .timeout(10_000)
                    .get()

                // Title: og:title -> twitter:title -> <title>
                val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.title().takeIf { it.isNotBlank() }

                // Description: og:description -> twitter:description -> meta[name=description]
                val description = document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }

                // Image: og:image -> twitter:image
                val imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                    ?: document.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }

                MinimalMetadata(title, description, imageUrl)
            } catch (e: IOException) {
                MinimalMetadata()
            } catch (e: Exception) {
                e.printStackTrace()
                MinimalMetadata()
            }
        }
    }
}
