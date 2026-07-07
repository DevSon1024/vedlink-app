package com.devson.vedlink.data.network.scraper.orchestrator

import com.devson.vedlink.domain.model.ScrapedMetadata
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataCacheManager @Inject constructor() {

    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val eTag: String? = null,
        val lastModified: String? = null
    )

    private val htmlCache = ConcurrentHashMap<String, CacheEntry<String>>()
    private val metadataCache = ConcurrentHashMap<String, CacheEntry<ScrapedMetadata>>()
    
    // Default expiration: 24 hours
    private val cacheDurationMs = 24 * 60 * 60 * 1000L

    fun getHtml(url: String): String? {
        val entry = htmlCache[url] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > cacheDurationMs) {
            htmlCache.remove(url)
            return null
        }
        return entry.data
    }

    fun putHtml(url: String, html: String, eTag: String? = null, lastModified: String? = null) {
        htmlCache[url] = CacheEntry(html, eTag = eTag, lastModified = lastModified)
    }

    fun getETag(url: String): String? {
        return htmlCache[url]?.eTag
    }

    fun getLastModified(url: String): String? {
        return htmlCache[url]?.lastModified
    }

    fun getMetadata(url: String): ScrapedMetadata? {
        val entry = metadataCache[url] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > cacheDurationMs) {
            metadataCache.remove(url)
            return null
        }
        return entry.data
    }

    fun putMetadata(url: String, metadata: ScrapedMetadata) {
        metadataCache[url] = CacheEntry(metadata)
    }

    fun clear() {
        htmlCache.clear()
        metadataCache.clear()
    }
}
