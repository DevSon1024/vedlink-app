package com.devson.vedlink.data.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.devson.vedlink.domain.model.SaveStatus
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
import com.devson.vedlink.domain.util.LinkExtractor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class LinkSaveService : Service() {

    @Inject
    lateinit var saveLinkUseCase: SaveLinkUseCase

    @Inject
    lateinit var linkExtractor: LinkExtractor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Track active jobs to prevent premature service shutdown
    private val activeJobCount = AtomicInteger(0)
    private val processingMutex = Mutex()

    // Queue for pending requests
    private val pendingRequests = mutableListOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedText = intent?.getStringExtra(EXTRA_TEXT)

        if (!sharedText.isNullOrBlank()) {
            // Increment active job count immediately
            activeJobCount.incrementAndGet()

            serviceScope.launch {
                processingMutex.withLock {
                    processSharedText(sharedText, startId)
                }
            }
        } else {
            showToastAndStop("No content to save", startId)
        }

        // Use START_REDELIVER_INTENT to ensure reliability
        return START_REDELIVER_INTENT
    }

    private suspend fun processSharedText(sharedText: String, startId: Int) {
        try {
            val urls = linkExtractor.extractUrls(sharedText)

            if (urls.isEmpty()) {
                showToastAndStop("No valid links found", startId)
                return
            }

            var newlySavedCount = 0
            var alreadyExistingCount = 0
            var errorCount = 0

            // Process each URL independently
            urls.forEach { url ->
                try {
                    val result = saveLinkUseCase(url, checkDuplicate = true)
                    result.onSuccess { saveResult ->
                        when (saveResult.status) {
                            SaveStatus.NEWLY_SAVED -> newlySavedCount++
                            SaveStatus.ALREADY_EXISTS -> alreadyExistingCount++
                        }
                    }.onFailure {
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }

            val message = buildResultMessage(
                totalUrls = urls.size,
                newlySaved = newlySavedCount,
                alreadyExisting = alreadyExistingCount,
                errors = errorCount
            )

            showToastAndStop(message, startId, isError = errorCount > 0 && newlySavedCount == 0)

        } catch (e: Exception) {
            showToastAndStop(
                "Failed to process links: ${e.message ?: "Unknown error"}",
                startId,
                isError = true
            )
        }
    }

    private fun buildResultMessage(
        totalUrls: Int,
        newlySaved: Int,
        alreadyExisting: Int,
        errors: Int
    ): String {
        return when {
            totalUrls == 1 -> {
                when {
                    newlySaved == 1 -> "Link Saved"
                    alreadyExisting == 1 -> "Link Already Available"
                    else -> "Failed to save link"
                }
            }
            else -> {
                when {
                    alreadyExisting == totalUrls -> "All Links Already Available"
                    newlySaved == totalUrls -> "$newlySaved Links Saved"
                    newlySaved > 0 && alreadyExisting > 0 && errors == 0 -> {
                        "$newlySaved Links Saved ($alreadyExisting already available)"
                    }
                    newlySaved > 0 -> "$newlySaved Links Saved"
                    else -> "Failed to save links"
                }
            }
        }
    }

    private fun showToastAndStop(message: String, startId: Int, isError: Boolean = false) {
        mainHandler.post {
            try {
                Toast.makeText(
                    this@LinkSaveService,
                    message,
                    if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                // Ignore if context is not available
            }

            // Decrement active job count
            val remainingJobs = activeJobCount.decrementAndGet()

            // Only stop service when all jobs are complete
            if (remainingJobs <= 0) {
                mainHandler.postDelayed({
                    stopSelf()
                }, 300) // Reduced delay since we're tracking jobs
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        activeJobCount.set(0)
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
    }
}