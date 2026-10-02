package com.example.model

import java.util.UUID

enum class MotionPreset(val displayName: String, val startScale: Float, val endScale: Float, val defaultCurve: MotionCurve) {
    CINEMATIC_PUSH_IN("Cinematic Push-In", 1.00f, 1.12f, MotionCurve.CINEMATIC_SLOW),
    GENTLE_PULL_OUT("Gentle Pull-Out", 1.12f, 1.00f, MotionCurve.EASE_IN_OUT),
    SUBTLE_ZOOM_IN("Subtle Zoom In", 1.00f, 1.06f, MotionCurve.EASE_IN_OUT),
    DYNAMIC_PUNCH("Dynamic Punch", 1.00f, 1.18f, MotionCurve.DYNAMIC_PUNCH),
    STATIC("Static Frame", 1.00f, 1.00f, MotionCurve.LINEAR),
    CUSTOM("Custom Keyframes", 1.00f, 1.10f, MotionCurve.EASE_IN_OUT)
}

data class ClipSegment(
    val id: String = UUID.randomUUID().toString(),
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val speechText: String = "",
    val startKeyframe: Keyframe,
    val endKeyframe: Keyframe,
    val intermediateKeyframes: List<Keyframe> = emptyList(),
    val motionPreset: MotionPreset = MotionPreset.CINEMATIC_PUSH_IN,
    val thumbnailBitmapPath: String? = null
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    fun allKeyframes(): List<Keyframe> {
        val list = mutableListOf<Keyframe>()
        list.add(startKeyframe)
        list.addAll(intermediateKeyframes)
        list.add(endKeyframe)
        return list.sortedBy { it.timestampMs }
    }
}
