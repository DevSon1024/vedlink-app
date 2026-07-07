package com.devson.vedlink.data.network.scraper.orchestrator

import org.jsoup.nodes.Document
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageResolver @Inject constructor() {

    fun resolvePreviewImage(url: String, document: Document?, parsedImageUrl: String?): String? {
        if (parsedImageUrl != null && parsedImageUrl.isNotBlank()) {
            return resolveAbsoluteUrl(url, parsedImageUrl)
        }

        if (document == null) return null

        // 1. Check picture elements
        val pictureSource = document.select("picture source").firstOrNull()?.attr("srcset")
            ?.let { parseSrcset(it) }
        if (!pictureSource.isNullOrBlank()) {
            return resolveAbsoluteUrl(url, pictureSource)
        }

        // 2. Check article-specific images with lazy-loading attributes
        val articleImg = document.select("article img, main img, body img").firstOrNull { img ->
            img.hasAttr("data-src") || img.hasAttr("data-lazy-src") || img.hasAttr("srcset") || img.hasAttr("src")
        }

        if (articleImg != null) {
            val candidate = articleImg.attr("data-src").takeIf { it.isNotBlank() }
                ?: articleImg.attr("data-lazy-src").takeIf { it.isNotBlank() }
                ?: articleImg.attr("srcset").takeIf { it.isNotBlank() }?.let { parseSrcset(it) }
                ?: articleImg.attr("src").takeIf { it.isNotBlank() }

            if (!candidate.isNullOrBlank()) {
                return resolveAbsoluteUrl(url, candidate)
            }
        }

        // 3. Fallback: Largest size image (based on height/width attributes in HTML if present)
        val largestImg = document.select("img").maxByOrNull { img ->
            val w = img.attr("width").toIntOrNull() ?: 0
            val h = img.attr("height").toIntOrNull() ?: 0
            w * h
        }
        val fallbackSrc = largestImg?.attr("src")?.takeIf { it.isNotBlank() }
        if (!fallbackSrc.isNullOrBlank()) {
            return resolveAbsoluteUrl(url, fallbackSrc)
        }

        return null
    }

    private fun parseSrcset(srcset: String): String? {
        // e.g., "image-200.jpg 200w, image-400.jpg 400w" -> select the largest one
        return try {
            srcset.split(",")
                .map { it.trim().split(" ") }
                .filter { it.isNotEmpty() }
                .maxByOrNull { parts ->
                    if (parts.size > 1) {
                        parts[1].replace("w", "").replace("x", "").toFloatOrNull() ?: 0f
                    } else 0f
                }?.firstOrNull()
        } catch (e: Exception) {
            srcset.split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull()
        }
    }

    private fun resolveAbsoluteUrl(baseUr: String, relativeUrl: String): String {
        return try {
            val base = URI(baseUr)
            base.resolve(relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
