package com.example.model

import android.net.Uri

/**
 * Encapsulates metadata and cached thumbnail for imported original or reference video.
 */
data class VideoMetadata(
    val uri: Uri,
    val fileName: String = "Video",
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val sizeBytes: Long = 0L,
    val rotation: Int = 0,
    val frameRate: Float = 30.0f,
    val thumbnailPath: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedResolution: String
        get() = "${width}x${height} (${if (height > width) "9:16 Portrait" else "16:9 Landscape"})"

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "Unknown size"
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                val kb = sizeBytes.toDouble() / 1024.0
                String.format("%.0f KB", kb)
            }
        }

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f
}
