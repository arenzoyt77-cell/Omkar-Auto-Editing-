package com.example.model

import java.util.UUID

/**
 * Represents a non-destructive text overlay or sticker positioned on the video.
 */
data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val positionX: Float = 50f, // percentage 0..100 across width
    val positionY: Float = 85f, // percentage 0..100 across height
    val fontSizeSp: Float = 22f,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0xB3000000,
    val isCaption: Boolean = false
) {
    fun contains(timestampMs: Long): Boolean = timestampMs in startMs..endMs

    val formattedTimeRange: String
        get() = String.format("%.1fs - %.1fs", startMs / 1000f, endMs / 1000f)
}
