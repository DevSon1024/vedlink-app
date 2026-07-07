package com.devson.vedlink.data.network.favicon

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaviconEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val faviconDir = File(context.filesDir, "favicons").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Finds, downloads, and stores a website's favicon.
     * Returns the absolute path of the locally stored file, or null if it fails.
     */
    suspend fun getOrFetchFavicon(urlStr: String): String? = withContext(Dispatchers.IO) {
        val host = try {
            URI(urlStr).host?.lowercase() ?: return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }

        // Check L2 local cache first
        val cachedFile = File(faviconDir, "$host.png")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext cachedFile.absolutePath
        }

        // Discovery pipeline
        val faviconUrls = discoverFaviconUrls(urlStr, host)
        for (faviconUrl in faviconUrls) {
            val file = downloadAndSave(faviconUrl, cachedFile)
            if (file != null) {
                return@withContext file.absolutePath
            }
        }

        // Google Favicon API fallback
        val fallbackUrl = "https://www.google.com/s2/favicons?domain=$host&sz=128"
        val fallbackFile = downloadAndSave(fallbackUrl, cachedFile)
        if (fallbackFile != null) {
            return@withContext fallbackFile.absolutePath
        }

        return@withContext null
    }

    private fun discoverFaviconUrls(pageUrl: String, host: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val doc = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(5000)
                .get()

            // 1. Apple touch icon
            val appleTouch = doc.select("link[rel=apple-touch-icon]").attr("href")
            if (appleTouch.isNotBlank()) urls.add(resolveAbsoluteUrl(pageUrl, appleTouch))

            // 2. SVG or high-resolution icon
            val svgIcon = doc.select("link[rel=icon][type*=svg], link[rel*=icon][sizes*=any]").attr("href")
            if (svgIcon.isNotBlank()) urls.add(resolveAbsoluteUrl(pageUrl, svgIcon))

            // 3. PNG icon
            val pngIcon = doc.select("link[rel*=icon][type*=png]").attr("href")
            if (pngIcon.isNotBlank()) urls.add(resolveAbsoluteUrl(pageUrl, pngIcon))

            // 4. Default links
            val genericIcon = doc.select("link[rel*=icon], link[rel='shortcut icon']").attr("href")
            if (genericIcon.isNotBlank()) urls.add(resolveAbsoluteUrl(pageUrl, genericIcon))

        } catch (e: Exception) {
            // Document loading failed
        }

        // 5. Hardcoded default root path fallback
        urls.add("https://$host/favicon.ico")
        return urls.distinct()
    }

    private fun downloadAndSave(imageUrl: String, targetFile: File): File? {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return null
                    // Save bytes to file
                    FileOutputStream(targetFile).use { fos ->
                        body.byteStream().use { input ->
                            input.copyTo(fos)
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0) {
                        return targetFile
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore download failures and let pipeline try next option
        }
        return null
    }

    private fun resolveAbsoluteUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else {
                val base = URL(baseUrl)
                URL(base, relativeUrl).toString()
            }
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
