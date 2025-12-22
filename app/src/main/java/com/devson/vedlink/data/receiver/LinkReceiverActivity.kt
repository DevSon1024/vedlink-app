package com.devson.vedlink.data.receiver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.devson.vedlink.data.service.LinkSaveService

class LinkReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the shared link
        handleSharedLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedLink(intent)
    }

    private fun handleSharedLink(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        // Start service to save links (can be single or multiple)
                        val serviceIntent = Intent(this, LinkSaveService::class.java).apply {
                            putExtra(LinkSaveService.EXTRA_TEXT, sharedText)
                        }
                        startService(serviceIntent)
                    }
                }
            }
        }

        // Always finish this activity
        finish()
    }
}