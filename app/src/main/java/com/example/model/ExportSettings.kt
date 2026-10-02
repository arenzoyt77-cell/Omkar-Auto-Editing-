package com.example.model

enum class ExportResolution(val displayName: String, val width: Int, val height: Int) {
    ORIGINAL("Original Source Resolution", 0, 0),
    FHD_1080P("1080p Full HD (1920x1080)", 1920, 1080),
    HD_720P("720p HD (1280x720)", 1280, 720),
    PORTRAIT_1080P("1080x1920 (Reels/Shorts 9:16)", 1080, 1920)
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.ORIGINAL,
    val bitrateMbps: Int = 10,
    val fps: Int = 30,
    val includeSubtitles: Boolean = false,
    val format: String = "MP4 (H.264 / AAC)"
)
