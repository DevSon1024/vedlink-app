package com.devson.vedlink.data.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.ColorUtils
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

    /**
     * Samples a local bitmap on [Dispatchers.Default] to extract a vibrant dominant color.
     * Guards against OutOfMemoryError, null bitmaps, and HARDWARE bitmaps by operating
     * strictly on a 64x64 downsampled thumbnail.
     * Returns a packed ARGB integer, or null if no vibrant color could be extracted.
     */
    suspend fun extractDominantColor(filePath: String?): Int? = withContext(Dispatchers.Default) {
        if (filePath.isNullOrBlank()) return@withContext null
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return@withContext null

        try {
            // 1. Measure dimensions without full decode
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // 2. Decode scaled down to ~64px
            val targetSize = 64
            val sampleOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, targetSize, targetSize)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, sampleOptions)
                ?: return@withContext null

            // 3. Convert HARDWARE bitmaps if necessary, scale exactly to 64x64
            val mutableBitmap = try {
                if (decodedBitmap.config == Bitmap.Config.HARDWARE) {
                    val copy = decodedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                    decodedBitmap.recycle()
                    copy ?: return@withContext null
                } else {
                    decodedBitmap
                }
            } catch (t: Throwable) {
                decodedBitmap
            }

            val finalThumb = if (mutableBitmap.width > targetSize || mutableBitmap.height > targetSize) {
                val scaled = Bitmap.createScaledBitmap(mutableBitmap, targetSize, targetSize, true)
                if (scaled != mutableBitmap) {
                    mutableBitmap.recycle()
                }
                scaled
            } else {
                mutableBitmap
            }

            val resultColor = computeVibrantDominantColor(finalThumb)
            finalThumb.recycle()
            resultColor
        } catch (oom: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun computeVibrantDominantColor(bitmap: Bitmap): Int? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bucketCounts = mutableMapOf<Int, Int>()
        val bucketScores = mutableMapOf<Int, Float>()
        val hsl = FloatArray(3)

        for (pixel in pixels) {
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 128) continue // Skip transparent pixels

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            // Filter out near-white and near-black pixels
            if (r > 245 && g > 245 && b > 245) continue
            if (r < 15 && g < 15 && b < 15) continue

            ColorUtils.RGBToHSL(r, g, b, hsl)
            val saturation = hsl[1]
            val lightness = hsl[2]

            // We want pleasant Material 3 tonal colors (moderate to high saturation, balanced lightness)
            if (lightness < 0.12f || lightness > 0.90f) continue

            val saturationScore = saturation
            val lightnessScore = 1f - kotlin.math.abs(lightness - 0.5f) * 2f
            val vibrancyWeight = 1.0f + (saturationScore * 2.0f) + (lightnessScore * 1.5f)

            // Quantize to 4 bits per channel (0..15)
            val qr = r shr 4
            val qg = g shr 4
            val qb = b shr 4
            val bucket = (qr shl 8) or (qg shl 4) or qb

            bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
            bucketScores[bucket] = (bucketScores[bucket] ?: 0f) + vibrancyWeight
        }

        if (bucketScores.isEmpty()) return null

        val bestBucket = bucketScores.maxByOrNull { it.value }?.key ?: return null
        val bestQr = (bestBucket shr 8) and 0x0F
        val bestQg = (bestBucket shr 4) and 0x0F
        val bestQb = bestBucket and 0x0F

        val red = (bestQr shl 4) or bestQr
        val green = (bestQg shl 4) or bestQg
        val blue = (bestQb shl 4) or bestQb

        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
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

