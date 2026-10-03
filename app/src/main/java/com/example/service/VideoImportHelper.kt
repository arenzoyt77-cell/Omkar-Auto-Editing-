package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * High-performance, asynchronous helper to inspect video files,
 * read standard container metadata, and generate bounded thumbnails
 * without blocking the Android UI thread.
 */
object VideoImportHelper {
    private const val TAG = "VideoImportHelper"

    suspend fun extractMetadata(
        context: Context,
        uri: Uri,
        fallbackName: String = "Selected Video"
    ): VideoMetadata = withContext(Dispatchers.IO) {
        var fileName = fallbackName
        var sizeBytes = 0L

        // 1. Query ContentResolver for real file name and file size
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        fileName = cursor.getString(nameIndex) ?: fallbackName
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ContentResolver query failed: ${e.message}")
        }

        // 2. Query MediaMetadataRetriever for duration, dimensions, rotation, framerate
        var durationMs = 10000L
        var width = 1920
        var height = 1080
        var rotation = 0
        var frameRate = 30.0f
        var thumbnailPath: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

            durStr?.toLongOrNull()?.let { durationMs = it.coerceAtLeast(100L) }
            wStr?.toIntOrNull()?.let { width = it.coerceAtLeast(128) }
            hStr?.toIntOrNull()?.let { height = it.coerceAtLeast(128) }
            rotStr?.toIntOrNull()?.let { rotation = it }
            rateStr?.toFloatOrNull()?.let { frameRate = it.coerceIn(15.0f, 120.0f) }

            // Adjust width/height if rotated 90 or 270 degrees
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            // 3. Generate a small, bounded thumbnail asynchronously
            try {
                val rawBitmap = retriever.getFrameAtTime(
                    (durationMs * 500L).coerceAtLeast(0L), // Sample at ~500ms or 50%
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: retriever.getFrameAtTime(0L)

                if (rawBitmap != null) {
                    val thumbDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
                    val thumbFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")

                    // Scale down to bounded dimensions (max 480x480)
                    val maxDim = 480f
                    val scale = (maxDim / kotlin.math.max(rawBitmap.width, rawBitmap.height)).coerceAtMost(1.0f)
                    val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(64)
                    val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(64)

                    val scaledBitmap = if (scale < 1.0f) {
                        Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                    } else {
                        rawBitmap
                    }

                    FileOutputStream(thumbFile).use { fos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                    }

                    if (scaledBitmap != rawBitmap) {
                        scaledBitmap.recycle()
                    }
                    rawBitmap.recycle()

                    thumbnailPath = thumbFile.absolutePath
                }
            } catch (e: Exception) {
                Log.w(TAG, "Thumbnail generation failed: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever failed on $uri: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignored
            }
        }

        VideoMetadata(
            uri = uri,
            fileName = fileName,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            rotation = rotation,
            frameRate = frameRate,
            thumbnailPath = thumbnailPath
        )
    }
}
