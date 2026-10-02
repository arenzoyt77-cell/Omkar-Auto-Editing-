package com.example.service

import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve

data class InterpolatedTransform(
    val scale: Float = 1.0f,
    val positionX: Float = 0.0f,
    val positionY: Float = 0.0f,
    val rotation: Float = 0.0f
)

object MotionInterpolationEngine {

    fun interpolateAt(clip: ClipSegment, currentTimestampMs: Long): InterpolatedTransform {
        val keyframes = clip.allKeyframes()
        if (keyframes.isEmpty()) {
            return InterpolatedTransform()
        }
        if (keyframes.size == 1) {
            val k = keyframes.first()
            return InterpolatedTransform(k.scale, k.positionX, k.positionY, k.rotation)
        }

        val clampedTime = currentTimestampMs.coerceIn(clip.startMs, clip.endMs)

        // Find surrounding keyframes
        if (clampedTime <= keyframes.first().timestampMs) {
            val first = keyframes.first()
            return InterpolatedTransform(first.scale, first.positionX, first.positionY, first.rotation)
        }
        if (clampedTime >= keyframes.last().timestampMs) {
            val last = keyframes.last()
            return InterpolatedTransform(last.scale, last.positionX, last.positionY, last.rotation)
        }

        for (i in 0 until keyframes.size - 1) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]

            if (clampedTime in k1.timestampMs..k2.timestampMs) {
                val span = (k2.timestampMs - k1.timestampMs).coerceAtLeast(1L)
                val rawProgress = (clampedTime - k1.timestampMs).toFloat() / span.toFloat()
                val easedProgress = k1.easing.interpolate(rawProgress)

                val scale = k1.scale + (k2.scale - k1.scale) * easedProgress
                val posX = k1.positionX + (k2.positionX - k1.positionX) * easedProgress
                val posY = k1.positionY + (k2.positionY - k1.positionY) * easedProgress
                val rot = k1.rotation + (k2.rotation - k1.rotation) * easedProgress

                return InterpolatedTransform(scale, posX, posY, rot)
            }
        }

        val fallback = keyframes.first()
        return InterpolatedTransform(fallback.scale, fallback.positionX, fallback.positionY, fallback.rotation)
    }
}
