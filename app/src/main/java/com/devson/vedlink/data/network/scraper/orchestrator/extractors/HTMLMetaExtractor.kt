package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import org.jsoup.nodes.Document

class HTMLMetaExtractor : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.HTML_META

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        if (document == null) return null

        val htmlTitle = document.title().takeIf { it.isNotBlank() }
        val metaDescription = document.select("meta[name=description], meta[property=description]").attr("content").takeIf { it.isNotBlank() }
        val mainHeading = document.select("h1").firstOrNull()?.text()?.takeIf { it.isNotBlank() }
        val canonical = document.select("link[rel=canonical]").attr("href").takeIf { it.isNotBlank() }
        val lang = document.select("html").attr("lang").takeIf { it.isNotBlank() }
        val themeColor = document.select("meta[name=theme-color]").attr("content").takeIf { it.isNotBlank() }
        val author = document.select("meta[name=author]").attr("content").takeIf { it.isNotBlank() }

        val published = document.select("meta[property=article:published_time], meta[name=pubdate], meta[name=publish_date]").attr("content").takeIf { it.isNotBlank() }
        val modified = document.select("meta[property=article:modified_time], meta[name=last-modified]").attr("content").takeIf { it.isNotBlank() }

        // Extract first article paragraph (skip small snippets, cookie banners, navigation links)
        val firstParagraph = document.select("article p, main p, body p")
            .map { it.text().trim() }
            .firstOrNull { it.length > 50 } // Must have some meaningful body text

        // Website logo search
        val websiteLogo = document.select("link[rel=icon], link[rel=shortcut icon]").attr("href").takeIf { it.isNotBlank() }

        if (htmlTitle == null && metaDescription == null && mainHeading == null && canonical == null) {
            return null
        }

        return PartialMetadata(
            extractorType = type,
            title = htmlTitle ?: mainHeading,
            description = metaDescription,
            canonicalUrl = canonical,
            language = lang,
            themeColor = themeColor,
            author = author,
            publishedDate = published,
            modifiedDate = modified,
            firstParagraph = firstParagraph,
            websiteLogo = websiteLogo
        )
    }
}
