package com.devson.vedlink.data.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.devson.vedlink.domain.usecase.SaveLinkUseCase
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)

        if (!url.isNullOrBlank()) {
            serviceScope.launch {
                try {
                    // Save link in background
                    val result = saveLinkUseCase(url)

                    // Show toast and ensure it displays before stopping service
                    result
                        .onSuccess {
                            showToastAndStop("Link saved", startId)
                        }
                        .onFailure { error ->
                            showToastAndStop(
                                "Failed to save: ${error.message ?: "Unknown error"}",
                                startId,
                                isError = true
                            )
                        }
                } catch (e: Exception) {
                    // Handle any unexpected exceptions
                    showToastAndStop(
                        "Failed to save: ${e.message ?: "Unknown error"}",
                        startId,
                        isError = true
                    )
                }
            }
        } else {
            // Show error if URL is empty
            showToastAndStop("No link to save", startId)
        }

        return START_NOT_STICKY
    }

    private fun showToastAndStop(message: String, startId: Int, isError: Boolean = false) {
        mainHandler.post {
            // Create and show toast
            val toast = Toast.makeText(
                this@LinkSaveService,
                message,
                if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            )
            toast.show()

            // Delay service stop to ensure toast is displayed
            // Toast.LENGTH_SHORT is ~2000ms, we give it 500ms to ensure it shows
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
        const val EXTRA_URL = "extra_url"
    }
}