package com.example.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class MotionCurve(val displayName: String, val description: String) {
    EASE_IN_OUT(
        displayName = "Ease In-Out",
        description = "Smooth cinematic acceleration and deceleration"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return if (clamped < 0.5f) {
                4f * clamped * clamped * clamped
            } else {
                1f - (-2f * clamped + 2f).pow(3) / 2f
            }
        }
    },

    CINEMATIC_SLOW(
        displayName = "Cinematic Slow",
        description = "Subtle Hermite S-curve for high-end documentary feel"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return clamped * clamped * (3f - 2f * clamped)
        }
    },

    EASE_IN(
        displayName = "Ease In",
        description = "Starts slowly, smoothly accelerates towards end"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return clamped * clamped
        }
    },

    EASE_OUT(
        displayName = "Ease Out",
        description = "Fast start with gentle cushioned settle"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return 1f - (1f - clamped) * (1f - clamped)
        }
    },

    DYNAMIC_PUNCH(
        displayName = "Dynamic Punch",
        description = "Energetic punch with slight overshoot for emphasis"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            val c4 = (2f * PI.toFloat()) / 3f
            return if (clamped == 0f) 0f
            else if (clamped == 1f) 1f
            else (2f.pow(-10f * clamped) * sin((clamped * 10f - 0.75f) * c4) + 1f).coerceIn(0f, 1.15f)
        }
    },

    CUBIC(
        displayName = "Cubic Easing",
        description = "Rapid cubic acceleration with polished deceleration"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return clamped * clamped * clamped
        }
    },

    SMOOTHSTEP(
        displayName = "Smoothstep",
        description = "Classic graphics Hermite interpolation"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return clamped * clamped * (3f - 2f * clamped)
        }
    },

    CUSTOM_BEZIER(
        displayName = "Custom Bezier",
        description = "Expressive cubic bezier curve modelled from reference video"
    ) {
        override fun interpolate(t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            // Cubic Bezier approximation with control points (0.25, 0.1, 0.25, 1.0)
            val u = 1f - clamped
            return 3f * u * u * clamped * 0.1f + 3f * u * clamped * clamped * 1.0f + clamped * clamped * clamped
        }
    },

    LINEAR(
        displayName = "Linear",
        description = "Constant speed with zero curve modulation"
    ) {
        override fun interpolate(t: Float): Float {
            return t.coerceIn(0f, 1f)
        }
    };

    abstract fun interpolate(t: Float): Float
}
