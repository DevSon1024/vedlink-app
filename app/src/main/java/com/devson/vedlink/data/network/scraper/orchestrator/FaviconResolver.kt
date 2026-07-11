package com.devson.vedlink.data.network.scraper.orchestrator

import org.jsoup.nodes.Document
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaviconResolver @Inject constructor() {

    fun resolveFavicon(url: String, document: Document?): String {
        if (url.contains("instagram.com", ignoreCase = true) || url.contains("instagr.am", ignoreCase = true)) {
            return "https://www.google.com/s2/favicons?sz=128&domain=instagram.com"
        }
        if (document != null) {
            // 1. Apple Touch Icon
            val appleTouch = document.select("link[rel=apple-touch-icon], link[rel=apple-touch-icon-precomposed]").firstOrNull()?.attr("href")
            if (!appleTouch.isNullOrBlank()) return resolveAbsoluteUrl(url, appleTouch)

            // 2. SVG Favicon
            val svgFavicon = document.select("link[rel~=(?i)^(shortcut|icon)$][type=image/svg+xml]").firstOrNull()?.attr("href")
            if (!svgFavicon.isNullOrBlank()) return resolveAbsoluteUrl(url, svgFavicon)

            // 3. PNG Favicon (with size checks)
            val pngFavicon = document.select("link[rel~=(?i)^(shortcut|icon)$][href~=\\.(png|PNG)]")
                .maxByOrNull { link ->
                    val sizes = link.attr("sizes")
                    if (sizes.contains("x")) {
                        sizes.split("x").firstOrNull()?.toIntOrNull() ?: 0
                    } else 0
                }?.attr("href")
            if (!pngFavicon.isNullOrBlank()) return resolveAbsoluteUrl(url, pngFavicon)

            // 4. Any icon link tag
            val iconLink = document.select("link[rel~=(?i)^(shortcut|icon)$]").firstOrNull()?.attr("href")
            if (!iconLink.isNullOrBlank()) return resolveAbsoluteUrl(url, iconLink)
        }

        // 5. Default root favicon.ico
        val rootFavicon = try {
            val uri = URI(url)
            URI(uri.scheme, uri.authority, "/favicon.ico", null, null).toString()
        } catch (e: Exception) {
            null
        }
        if (!rootFavicon.isNullOrBlank()) return rootFavicon

        // 6. Google Favicon Service fallback
        val domain = extractDomain(url)
        return "https://www.google.com/s2/favicons?sz=128&domain=$domain"
    }

    private fun resolveAbsoluteUrl(baseUr: String, relativeUrl: String): String {
        return try {
            val base = URI(baseUr)
            base.resolve(relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            URI(url).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }
}
