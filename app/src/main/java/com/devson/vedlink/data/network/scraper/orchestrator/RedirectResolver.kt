package com.devson.vedlink.data.network.scraper.orchestrator

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RedirectResolver @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val shortenedHosts = setOf(
        "youtu.be", "bit.ly", "tinyurl.com", "rebrand.ly", "t.co", "lnkd.in",
        "t.me", "wa.me", "amzn.to", "fb.me", "pin.it", "goo.gl", "ow.ly", "is.gd"
    )

    fun isShortenedUrl(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
        return host in shortenedHosts || shortenedHosts.any { host.endsWith(".$it") }
    }

    suspend fun resolve(url: String): String = withContext(Dispatchers.IO) {
        var currentUrl = url
        if (!isShortenedUrl(currentUrl)) return@withContext currentUrl

        try {
            val client = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(currentUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    currentUrl = response.request.url.toString()
                } else {
                    // Try with GET if HEAD fails
                    val getRequest = request.newBuilder().get().build()
                    client.newCall(getRequest).execute().use { getResponse ->
                        currentUrl = getResponse.request.url.toString()
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to original URL on network failure
        }
        return@withContext currentUrl
    }
}
