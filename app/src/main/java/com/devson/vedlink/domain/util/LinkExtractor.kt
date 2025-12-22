package com.devson.vedlink.domain.util

import android.util.Patterns
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkExtractor @Inject constructor() {

    // Comprehensive URL regex pattern that matches various URL formats
    private val urlPattern = Pattern.compile(
        """(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»""'']))""",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )

    /**
     * Extracts all valid URLs from the given text.
     * Handles:
     * - URLs with http/https schemes
     * - URLs without schemes (adds https automatically)
     * - URLs embedded in large paragraphs, articles, or jargon text
     * - Multiple URLs in the same text
     * - Duplicate URLs (returns unique list)
     */
    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val urls = mutableSetOf<String>() // Use Set to avoid duplicates

        // Method 1: Use comprehensive regex pattern
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            if (url != null) {
                val cleanedUrl = cleanAndValidateUrl(url)
                if (cleanedUrl != null) {
                    urls.add(cleanedUrl)
                }
            }
        }

        // Method 2: Split by whitespace and check each word (backup method)
        text.split(Regex("\\s+")).forEach { word ->
            val cleanedUrl = cleanAndValidateUrl(word)
            if (cleanedUrl != null && !urls.contains(cleanedUrl)) {
                urls.add(cleanedUrl)
            }
        }

        // Return sorted list for consistent order
        return urls.toList().sorted()
    }

    /**
     * Cleans and validates a potential URL string.
     * Returns null if the URL is invalid.
     */
    private fun cleanAndValidateUrl(potentialUrl: String): String? {
        if (potentialUrl.isBlank()) return null

        var cleaned = potentialUrl.trim()

        // Remove common trailing punctuation that might be captured
        cleaned = cleaned.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}', '"', '\'')

        // Remove leading punctuation
        cleaned = cleaned.trimStart('(', '[', '{', '"', '\'')

        // Skip if it's too short to be a valid URL
        if (cleaned.length < 4) return null

        // Add https:// if no scheme is present
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            // Check if it looks like a domain
            if (cleaned.contains(".") && !cleaned.startsWith("www.")) {
                cleaned = "https://$cleaned"
            } else if (cleaned.startsWith("www.")) {
                cleaned = "https://$cleaned"
            } else {
                return null // Not a valid URL format
            }
        }

        // Validate using Android's Patterns utility
        return if (isValidUrl(cleaned)) {
            cleaned
        } else {
            null
        }
    }

    /**
     * Validates if the given string is a properly formatted URL.
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            Patterns.WEB_URL.matcher(url).matches() &&
                    (url.startsWith("http://") || url.startsWith("https://")) &&
                    url.contains(".")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts a single URL from text (for backward compatibility).
     * Returns the first valid URL found, or null if none found.
     */
    fun extractSingleUrl(text: String): String? {
        return extractUrls(text).firstOrNull()
    }
}