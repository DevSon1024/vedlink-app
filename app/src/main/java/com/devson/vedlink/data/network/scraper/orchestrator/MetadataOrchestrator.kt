package com.devson.vedlink.data.network.scraper.orchestrator

import com.devson.vedlink.data.network.scraper.orchestrator.extractors.*
import com.devson.vedlink.domain.model.MetadataState
import com.devson.vedlink.domain.model.ScrapedMetadata
import com.devson.vedlink.domain.model.WebsiteProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataOrchestrator @Inject constructor(
    private val httpClient: OkHttpClient,
    private val urlNormalizer: UrlNormalizer,
    private val redirectResolver: RedirectResolver,
    private val providerDetector: ProviderDetector,
    private val cacheManager: MetadataCacheManager,
    private val oEmbedExtractor: OEmbedExtractor,
    private val providerSpecificExtractor: ProviderSpecificExtractor,
    private val faviconResolver: FaviconResolver,
    private val imageResolver: ImageResolver,
    private val retryEngine: RetryEngine,
    private val browserRenderer: BrowserRenderingFallback
) {
    private val mergeEngine = MetadataMergeEngine()

    private val extractors: List<MetadataExtractor> = listOf(
        OpenGraphExtractor(),
        TwitterCardExtractor(),
        JSONLDExtractor(),
        HTMLMetaExtractor(),
        CanonicalUrlExtractor(),
        oEmbedExtractor,
        providerSpecificExtractor
    )

    /**
     * Entrypoint of the orchestrator pipeline.
     */
    suspend fun resolveMetadata(url: String): ScrapedMetadata = coroutineScope {
        // 1. Normalize URL
        val normalized = urlNormalizer.normalize(url)

        // 2. Resolve Redirects
        val resolvedUrl = redirectResolver.resolve(normalized)

        // 3. Detect Provider
        val provider = providerDetector.detectProvider(resolvedUrl)

        // 4. Check Cache L1
        val cached = cacheManager.getMetadata(resolvedUrl)
        if (cached != null && !cached.title.isNullOrBlank()) {
            println("[MetadataOrchestrator] Cache Hit for $resolvedUrl")
            return@coroutineScope cached
        }

        // 5. Fetch HTML Document (supporting conditional GET tags: ETag, Last-Modified)
        val document = try {
            fetchHtml(resolvedUrl)
        } catch (e: Exception) {
            null
        }

        // 6. Execute Metadata Pipeline in parallel
        val partialJobs = extractors.map { extractor ->
            async(Dispatchers.Default) {
                try {
                    val start = System.currentTimeMillis()
                    val res = extractor.extract(resolvedUrl, document)
                    val duration = System.currentTimeMillis() - start
                    println("[MetadataOrchestrator] ${extractor.type} finished in ${duration}ms")
                    res
                } catch (e: Exception) {
                    println("[MetadataOrchestrator] ${extractor.type} failed: ${e.message}")
                    null // Error isolation: individual failures are ignored
                }
            }
        }

        val partials = partialJobs.awaitAll().filterNotNull()

        // 7. Merge results
        val domain = extractDomain(resolvedUrl)
        var merged = mergeEngine.merge(partials, domain, provider.name)

        // 8. If metadata is blank, attempt headless browser fallback
        if (merged.title.isNullOrBlank() || merged.title == domain) {
            try {
                println("[MetadataOrchestrator] Metadata is sparse. Attempting headless browser rendering...")
                val renderedHtml = browserRenderer.render(resolvedUrl)
                if (!renderedHtml.isNullOrBlank()) {
                    val renderedDoc = Jsoup.parse(renderedHtml, resolvedUrl)
                    val fallbackJobs = extractors.map { extractor ->
                        async(Dispatchers.Default) {
                            try {
                                extractor.extract(resolvedUrl, renderedDoc)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    val fallbackPartials = fallbackJobs.awaitAll().filterNotNull()
                    merged = mergeEngine.merge(fallbackPartials, domain, provider.name)
                }
            } catch (e: Exception) {
                println("[MetadataOrchestrator] Headless browser fallback failed: ${e.message}")
            }
        }

        // 9. Resolve assets (favicon, preview image)
        val finalFavicon = faviconResolver.resolveFavicon(resolvedUrl, document)
        val finalPreviewImage = imageResolver.resolvePreviewImage(resolvedUrl, document, merged.imageUrl)

        val finalMetadata = merged.copy(
            faviconUrl = finalFavicon,
            imageUrl = finalPreviewImage
        )

        // 10. Cache the resolved result
        cacheManager.putMetadata(resolvedUrl, finalMetadata)

        return@coroutineScope finalMetadata
    }

    private suspend fun fetchHtml(url: String): org.jsoup.nodes.Document? = retryEngine.executeWithRetry {
        val cachedHtml = cacheManager.getHtml(url)
        val eTag = cacheManager.getETag(url)
        val lastModified = cacheManager.getLastModified(url)

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")

        if (eTag != null) {
            requestBuilder.header("If-None-Match", eTag)
        }
        if (lastModified != null) {
            requestBuilder.header("If-Modified-Since", lastModified)
        }

        val request = requestBuilder.build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == 304 && cachedHtml != null) {
                println("[MetadataOrchestrator] 304 Not Modified. Reusing cached HTML.")
                return@executeWithRetry Jsoup.parse(cachedHtml, url)
            }

            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code}")
            }

            val bodyText = response.body?.string() ?: ""
            val nextETag = response.header("ETag")
            val nextModified = response.header("Last-Modified")

            cacheManager.putHtml(url, bodyText, nextETag, nextModified)
            return@executeWithRetry Jsoup.parse(bodyText, url)
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }
}
