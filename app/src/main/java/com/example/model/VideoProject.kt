package com.example.model

import android.net.Uri
import java.util.UUID

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
)
