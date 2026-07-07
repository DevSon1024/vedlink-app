package com.devson.vedlink.data.network.scraper.orchestrator

import javax.inject.Inject
import javax.inject.Singleton

interface BrowserRenderer {
    suspend fun render(url: String): String?
}

@Singleton
class BrowserRenderingFallback @Inject constructor() : BrowserRenderer {
    override suspend fun render(url: String): String? {
        // Modular stub for headless browser rendering.
        // Can be fully implemented later with WebView-based extraction.
        return null
    }
}
