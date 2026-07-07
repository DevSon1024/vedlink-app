package com.devson.vedlink.data.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewImageEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val previewDir = File(context.filesDir, "previews").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Downloads a remote image, compresses & resizes it, and caches it locally.
     * Returns the absolute local path to the cached file, or null if it fails.
     */
    suspend fun getOrFetchPreview(imageUrl: String): String? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null

        val filename = hashString(imageUrl) + ".jpg"
        val targetFile = File(previewDir, filename)

        // Check if already cached
        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext targetFile.absolutePath
        }

        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null

                // Decode and downsample/compress bitmap to preserve space & memory
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // Enforce max size of 800px on either dimension
                options.inSampleSize = calculateInSampleSize(options, 800, 800)
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null

                // Compress and write to local file
                FileOutputStream(targetFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
                
                bitmap.recycle() // Clean up native memory allocations
                return@withContext targetFile.absolutePath
            }
        } catch (e: Exception) {
            // Failed to download/decode image, delete incomplete file if any
            if (targetFile.exists()) {
                targetFile.delete()
            }
        }
        return@withContext null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
