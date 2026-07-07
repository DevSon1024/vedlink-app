package com.devson.vedlink.data.network.scraper.orchestrator

import com.devson.vedlink.domain.model.WebsiteProvider
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderDetector @Inject constructor() {

    fun detectProvider(url: String): WebsiteProvider {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        return when {
            host.contains("youtube.com") || host.contains("youtu.be") -> WebsiteProvider.YOUTUBE
            host.contains("instagram.com") || host.contains("instagr.am") -> WebsiteProvider.INSTAGRAM
            host.contains("threads.net") -> WebsiteProvider.THREADS
            host.contains("facebook.com") || host.contains("fb.com") || host.contains("fb.watch") -> WebsiteProvider.FACEBOOK
            host.contains("reddit.com") || host.contains("redditmedia.com") -> WebsiteProvider.REDDIT
            host.contains("github.com") -> WebsiteProvider.GITHUB
            host.contains("spotify.com") -> WebsiteProvider.SPOTIFY
            host.contains("imdb.com") -> WebsiteProvider.IMDB
            host.contains("steampowered.com") || host.contains("steamcommunity.com") -> WebsiteProvider.STEAM
            host.contains("pinterest.com") || host.contains("pin.it") -> WebsiteProvider.PINTEREST
            host.contains("linkedin.com") || host.contains("lnkd.in") -> WebsiteProvider.LINKEDIN
            host.contains("tiktok.com") || host.contains("vt.tiktok.com") -> WebsiteProvider.TIKTOK
            host.contains("wikipedia.org") -> WebsiteProvider.WIKIPEDIA
            host.contains("amazon.") || host.contains("amzn.to") -> WebsiteProvider.AMAZON
            host.contains("medium.com") -> WebsiteProvider.MEDIUM
            host.contains("stackoverflow.com") -> WebsiteProvider.STACK_OVERFLOW
            host.contains("dev.to") -> WebsiteProvider.DEV_TO
            host.contains("play.google.com") -> WebsiteProvider.PLAY_STORE
            host.contains("apps.apple.com") || host.contains("itunes.apple.com") -> WebsiteProvider.APP_STORE
            host.contains("letterboxd.com") -> WebsiteProvider.LETTERBOXD
            host.contains("netflix.com") -> WebsiteProvider.NETFLIX
            host.contains("primevideo.com") || host.contains("amazon.com/primevideo") -> WebsiteProvider.PRIME_VIDEO
            else -> WebsiteProvider.GENERIC
        }
    }
}
