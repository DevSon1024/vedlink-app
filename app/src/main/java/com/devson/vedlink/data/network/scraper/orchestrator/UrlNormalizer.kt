package com.devson.vedlink.data.network.scraper.orchestrator

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlNormalizer @Inject constructor() {
    private val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "igshid", "ref", "aff", "si", "yclid", "mc_cid", "mc_eid",
        "__twitter_impression", "action_share"
    )

    fun normalize(url: String): String {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return ""

        // Ensure scheme exists
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        // Clean query parameters (strip trackers)
        cleanUrl = stripTrackingParameters(cleanUrl)

        // Apply domain-specific canonicalizations (mobile domains, etc.)
        cleanUrl = canonicalizeDomainSpecific(cleanUrl)

        // Standard formatting (trailing slashes, protocol)
        cleanUrl = finalizeUrlFormatting(cleanUrl)

        return cleanUrl
    }

    private fun stripTrackingParameters(urlStr: String): String {
        return try {
            val uri = URI(urlStr)
            val query = uri.query ?: return urlStr
            if (query.isBlank()) return urlStr

            val newQuery = query.split("&")
                .map { it.split("=") }
                .filter { it.isNotEmpty() && !trackingParams.contains(it[0].lowercase()) }
                .joinToString("&") { parts ->
                    if (parts.size > 1) "${parts[0]}=${parts[1]}" else parts[0]
                }

            val newUri = URI(
                uri.scheme,
                uri.authority,
                uri.path,
                if (newQuery.isNotBlank()) newQuery else null,
                null // Strip unnecessary fragment/anchors
            )
            newUri.toString()
        } catch (e: Exception) {
            urlStr
        }
    }

    private fun canonicalizeDomainSpecific(urlStr: String): String {
        return try {
            val uri = URI(urlStr)
            var host = uri.host?.lowercase() ?: return urlStr
            val path = uri.path ?: ""
            val query = uri.query ?: ""

            // Wikipedia: m.wikipedia.org -> wikipedia.org
            if (host.contains("wikipedia.org") && host.startsWith("m.")) {
                host = host.removePrefix("m.")
            }
            // YouTube: m.youtube.com -> youtube.com
            if (host.contains("youtube.com") && host.startsWith("m.")) {
                host = host.removePrefix("m.")
            }
            // Twitter / X: mobile.twitter.com -> twitter.com, mobile.x.com -> x.com
            if (host.startsWith("mobile.")) {
                host = host.removePrefix("mobile.")
            }
            if (host.startsWith("m.")) {
                host = host.removePrefix("m.")
            }
            // General www stripping for canonical consistency
            if (host.startsWith("www.")) {
                host = host.removePrefix("www.")
            }

            val newUri = URI(
                uri.scheme ?: "https",
                host,
                path,
                if (query.isNotBlank()) query else null,
                null
            )
            newUri.toString()
        } catch (e: Exception) {
            urlStr
        }
    }

    private fun finalizeUrlFormatting(urlStr: String): String {
        var formatted = urlStr
        // Enforce HTTPS
        if (formatted.startsWith("http://")) {
            formatted = "https://" + formatted.substring(7)
        }
        // Remove trailing slash in paths without query parameters to prevent duplicates like domain.com/ vs domain.com
        if (formatted.endsWith("/") && formatted.count { it == '/' } == 3 && !formatted.contains("?")) {
            formatted = formatted.removeSuffix("/")
        }
        return formatted
    }
}
