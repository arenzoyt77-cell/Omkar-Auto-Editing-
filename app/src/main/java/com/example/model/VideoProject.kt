package com.example.model

import android.net.Uri
import java.util.UUID

/**
 * Core foundation model representing a video project in OMKAR Automatic Video Maker.
 * Holds all metadata, audio-speech thought segmentation, split points, keyframed clips,
 * editor mode, and export configuration.
 */
data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val videoUri: Uri,
    val localFilePath: String? = null,
    val title: String = "Untitled Project",
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val rotation: Int = 0,
    val frameRate: Float = 30.0f,
    val speechSegments: List<SpeechSegment> = emptyList(),
    val splitPoints: List<SplitPoint> = emptyList(),
    val clips: List<ClipSegment> = emptyList(),
    val mode: EditorMode = EditorMode.SIMPLE,
    val exportSettings: ExportSettings = ExportSettings(),
    val audioAmplitudes: List<Float> = emptyList(),
    val isAnalyzed: Boolean = false
) {
    val totalClipsCount: Int get() = clips.size
    val totalSplitsCount: Int get() = splitPoints.size
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f

    val formattedDuration: String
        get() {
            val totalSec = durationMs / 1000
            val mins = totalSec / 60
            val secs = totalSec % 60
            val ms = (durationMs % 1000) / 100
            return String.format("%02d:%02d.%d", mins, secs, ms)
        }

    fun findClipAt(timestampMs: Long): ClipSegment? {
        return clips.firstOrNull { timestampMs in it.startMs until it.endMs }
            ?: clips.lastOrNull { timestampMs >= it.startMs }
    }

    fun findSpeechAt(timestampMs: Long): SpeechSegment? {
        return speechSegments.firstOrNull { timestampMs in it.startMs..it.endMs }
    }
}
