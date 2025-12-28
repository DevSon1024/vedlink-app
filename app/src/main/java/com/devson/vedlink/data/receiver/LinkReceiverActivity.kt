package com.devson.vedlink.data.receiver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.devson.vedlink.data.service.LinkSaveService

class LinkReceiverActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the shared link
        handleSharedLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedLink(intent)
    }

    private fun handleSharedLink(intent: Intent?) {
        // Prevent duplicate processing
        if (isProcessing) {
            return
        }

        isProcessing = true

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        // Start service to save links
                        val serviceIntent = Intent(this, LinkSaveService::class.java).apply {
                            putExtra(LinkSaveService.EXTRA_TEXT, sharedText)
                        }

                        try {
                            startService(serviceIntent)
                        } catch (e: Exception) {
                            // Handle service start failure
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // Delay finish to ensure service starts properly
        handler.postDelayed({
            finish()
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}