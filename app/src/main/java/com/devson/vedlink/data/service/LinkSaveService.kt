package com.devson.vedlink.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)

        if (!url.isNullOrBlank()) {
            Toast.makeText(this, "Saving link...", Toast.LENGTH_SHORT).show()

            serviceScope.launch {
                saveLinkUseCase(url)
                    .onSuccess {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@LinkSaveService,
                                "Link saved!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .onFailure { error ->
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@LinkSaveService,
                                "Failed: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}