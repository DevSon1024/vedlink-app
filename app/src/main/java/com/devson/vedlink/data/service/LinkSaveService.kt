package com.devson.vedlink.data.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.domain.util.LinkExtractor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LinkSaveService : Service() {

    @Inject
    lateinit var saveLinkUseCase: SaveLinkUseCase

    @Inject
    lateinit var linkExtractor: LinkExtractor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedText = intent?.getStringExtra(EXTRA_TEXT)

        if (!sharedText.isNullOrBlank()) {
            serviceScope.launch {
                try {
                    // Extract all URLs from the shared text
                    val urls = linkExtractor.extractUrls(sharedText)

                    if (urls.isEmpty()) {
                        showToastAndStop("No valid links found", startId)
                        return@launch
                    }

                    // Save all links and track results
                    var savedCount = 0
                    var duplicateCount = 0
                    var errorCount = 0

                    urls.forEach { url ->
                        val result = saveLinkUseCase(url)
                        result
                            .onSuccess { linkId ->
                                // Check if it's a new link or existing (duplicate)
                                if (linkId > 0) {
                                    savedCount++
                                }
                            }
                            .onFailure { error ->
                                if (error is LinkAlreadyExistsException) {
                                    duplicateCount++
                                } else {
                                    errorCount++
                                }
                            }
                    }

                    // Show appropriate message based on results
                    val message = when {
                        urls.size == 1 && duplicateCount == 1 -> "Link Already Available"
                        urls.size == 1 && savedCount == 1 -> "Link Saved"
                        urls.size > 1 && duplicateCount == urls.size -> "All Links Already Available"
                        urls.size > 1 && savedCount > 0 -> {
                            if (duplicateCount > 0) {
                                "$savedCount Links Saved ($duplicateCount already available)"
                            } else {
                                "$savedCount Links Saved"
                            }
                        }
                        errorCount > 0 -> "Failed to save some links"
                        else -> "Links processed"
                    }

                    showToastAndStop(message, startId)

                } catch (e: Exception) {
                    showToastAndStop(
                        "Failed to process links: ${e.message ?: "Unknown error"}",
                        startId,
                        isError = true
                    )
                }
            }
        } else {
            showToastAndStop("No content to save", startId)
        }

        return START_NOT_STICKY
    }

    private fun showToastAndStop(message: String, startId: Int, isError: Boolean = false) {
        mainHandler.post {
            Toast.makeText(
                this@LinkSaveService,
                message,
                if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()

            // Delay service stop to ensure toast is displayed
            mainHandler.postDelayed({
                stopSelf(startId)
            }, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
        @Deprecated("Use EXTRA_TEXT instead", ReplaceWith("EXTRA_TEXT"))
        const val EXTRA_URL = "extra_url"
    }
}

// Custom exception for duplicate links
class LinkAlreadyExistsException(message: String) : Exception(message)