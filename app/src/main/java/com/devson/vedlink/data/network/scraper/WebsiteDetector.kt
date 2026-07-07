package com.devson.vedlink.data.network.scraper

import com.devson.vedlink.data.network.scraper.orchestrator.ProviderDetector
import com.devson.vedlink.domain.model.WebsiteProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebsiteDetector @Inject constructor(
    private val detector: ProviderDetector
) {
    fun detectProvider(url: String): WebsiteProvider {
        return detector.detectProvider(url)
    }
}
