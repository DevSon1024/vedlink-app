package com.devson.vedlink.data.network.normalizer

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UrlNormalizer @Inject constructor(
    private val httpClient: OkHttpClient
) {
    // List of tracking query parameters to remove
    private val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "igshid", "ref", "aff", "si", "yclid", "mc_cid", "mc_eid",
        "__twitter_impression"
    )

    /**
     * Resolves redirects, strips tracking parameters, and returns the canonical URL.
     */
    suspend fun normalize(url: String): String = withContext(Dispatchers.IO) {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext ""

        // Ensure scheme exists
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        // 1. Resolve redirect if it's a known shortened URL or redirect service
        if (shouldResolveRedirect(cleanUrl)) {
            cleanUrl = resolveRedirect(cleanUrl)
        }

        // 2. Clean query parameters (strip trackers)
        cleanUrl = stripTrackingParameters(cleanUrl)

        // 3. Apply domain-specific canonicalizations (mobile domains, etc.)
        cleanUrl = canonicalizeDomainSpecific(cleanUrl)

        // 4. Standard formatting (trailing slashes, protocol)
        cleanUrl = finalizeUrlFormatting(cleanUrl)

        return@withContext cleanUrl
    }

    private fun shouldResolveRedirect(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
        val shortenedHosts = setOf(
            "youtu.be", "bit.ly", "tinyurl.com", "rebrand.ly", "t.co", "lnkd.in",
            "t.me", "wa.me", "amzn.to", "fb.me", "pin.it", "goo.gl", "ow.ly", "is.gd"
        )
        return host in shortenedHosts || shortenedHosts.any { host.endsWith(".$it") }
    }

    private suspend fun resolveRedirect(url: String): String {
        return try {
            // Build okhttp client specifically allowing redirects
            val client = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(url)
                .head() // Use HEAD request to save bandwidth
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.request.url.toString()
                } else {
                    // Try with GET if HEAD fails
                    val getRequest = request.newBuilder().get().build()
                    client.newCall(getRequest).execute().use { getResponse ->
                        getResponse.request.url.toString()
                    }
                }
            }
        } catch (e: Exception) {
            url // Fallback to original URL on network/redirect failures
        }
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
            var path = uri.path ?: ""
            var query = uri.query ?: ""

            // YouTube: m.youtube.com -> youtube.com, youtu.be -> youtube.com
            if (host.contains("youtube.com") || host.contains("youtu.be")) {
                host = "youtube.com"
                if (host.startsWith("m.")) {
                    host = host.removePrefix("m.")
                }
            }
            // Amazon: amazon.in, amazon.co.uk -> amazon.com (or normalize to base domain format)
            if (host.startsWith("www.")) {
                // Keep www for consistency or strip it; let's strip www prefix for canonical comparison
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
