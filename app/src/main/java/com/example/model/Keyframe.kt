package com.example.model

import java.util.UUID

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val scale: Float = 1.0f,
    val positionX: Float = 0.0f,
    val positionY: Float = 0.0f,
    val rotation: Float = 0.0f,
    val easing: MotionCurve = MotionCurve.EASE_IN_OUT
)
