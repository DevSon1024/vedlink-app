package com.devson.vedlink.data.network.scraper.orchestrator.extractors

import com.devson.vedlink.data.network.scraper.orchestrator.ExtractorType
import com.devson.vedlink.data.network.scraper.orchestrator.MetadataExtractor
import com.devson.vedlink.data.network.scraper.orchestrator.PartialMetadata
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.nodes.Document
import java.net.URI

class JSONLDExtractor : MetadataExtractor {
    override val type: ExtractorType = ExtractorType.JSON_LD

    override suspend fun extract(url: String, document: Document?): PartialMetadata? {
        if (document == null) return null

        val scriptElements = document.select("script[type=application/ld+json]")
        if (scriptElements.isEmpty()) return null

        var title: String? = null
        var description: String? = null
        var imageUrl: String? = null
        var websiteName: String? = null
        var authorName: String? = null
        var published: String? = null
        var modified: String? = null

        for (element in scriptElements) {
            val jsonText = element.html().trim()
            if (jsonText.isBlank()) continue

            try {
                val parsed = JsonParser.parseString(jsonText)
                val objects = flattenJsonLd(parsed)

                for (obj in objects) {
                    val typeVal = getStringOrNull(obj, "@type") ?: ""
                    
                    // Match any of the 15+ supported schema.org types
                    val isSupportedType = typeVal in setOf(
                        "Article", "NewsArticle", "BlogPosting", "VideoObject", "ImageObject",
                        "Movie", "TVSeries", "Book", "Recipe", "SoftwareApplication", "Product",
                        "Organization", "Person", "WebPage", "WebSite"
                    )

                    if (isSupportedType || title == null) {
                        // Extract Title
                        val objTitle = getStringOrNull(obj, "headline") 
                            ?: getStringOrNull(obj, "name")
                        if (!objTitle.isNullOrBlank()) title = objTitle

                        // Extract Description
                        val objDesc = getStringOrNull(obj, "description")
                        if (!objDesc.isNullOrBlank()) description = objDesc

                        // Extract Image
                        val imgElement = obj.get("image")
                        val objImg = when {
                            imgElement == null -> null
                            imgElement.isJsonPrimitive -> imgElement.asString
                            imgElement.isJsonObject -> getStringOrNull(imgElement.asJsonObject, "url")
                            imgElement.isJsonArray -> {
                                val first = imgElement.asJsonArray.firstOrNull()
                                if (first?.isJsonPrimitive == true) first.asString else null
                            }
                            else -> null
                        }
                        if (!objImg.isNullOrBlank()) {
                            imageUrl = resolveAbsoluteUrl(url, objImg)
                        }

                        // Extract Website Name / Publisher
                        val pubElement = obj.get("publisher")
                        if (pubElement != null && pubElement.isJsonObject) {
                            val pubName = getStringOrNull(pubElement.asJsonObject, "name")
                            if (!pubName.isNullOrBlank()) websiteName = pubName
                        }

                        // Extract Author
                        val authElement = obj.get("author")
                        if (authElement != null) {
                            authorName = when {
                                authElement.isJsonObject -> getStringOrNull(authElement.asJsonObject, "name")
                                authElement.isJsonArray -> {
                                    val first = authElement.asJsonArray.firstOrNull()
                                    if (first?.isJsonObject == true) getStringOrNull(first.asJsonObject, "name") else null
                                }
                                else -> null
                            }
                        }

                        // Extract Dates
                        val pubDate = getStringOrNull(obj, "datePublished")
                        if (!pubDate.isNullOrBlank()) published = pubDate

                        val modDate = getStringOrNull(obj, "dateModified")
                        if (!modDate.isNullOrBlank()) modified = modDate
                    }
                }
            } catch (e: Exception) {
                // Suppress parse failures of individual script blocks
            }
        }

        if (title == null && description == null && imageUrl == null && websiteName == null) {
            return null
        }

        return PartialMetadata(
            extractorType = type,
            title = title,
            description = description,
            imageUrl = imageUrl,
            websiteName = websiteName,
            author = authorName,
            publishedDate = published,
            modifiedDate = modified
        )
    }

    private fun flattenJsonLd(element: JsonElement): List<JsonObject> {
        val list = mutableListOf<JsonObject>()
        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                if (obj.has("@graph")) {
                    val graph = obj.get("@graph")
                    if (graph.isJsonArray) {
                        list.addAll(flattenJsonLd(graph))
                    }
                } else {
                    list.add(obj)
                }
            }
            element.isJsonArray -> {
                for (item in element.asJsonArray) {
                    list.addAll(flattenJsonLd(item))
                }
            }
        }
        return list
    }

    private fun getStringOrNull(obj: JsonObject, memberName: String): String? {
        val element = obj.get(memberName)
        return if (element != null && element.isJsonPrimitive) element.asString else null
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
