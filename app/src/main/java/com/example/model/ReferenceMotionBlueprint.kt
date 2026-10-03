package com.example.model

import java.util.UUID

enum class MotionEventType(val displayName: String) {
    PUSH_IN("Smooth Push-In"),
    PULL_OUT("Gentle Pull-Out"),
    PUNCH_ZOOM("Dynamic Punch Zoom"),
    QUICK_ZOOM("Quick Reaction Zoom"),
    SLOW_CINEMATIC("Slow Cinematic Drift"),
    ALTERNATING_ZOOM("Alternating Rhythm Zoom"),
    REFRAME_PAN("Subject Reframing Pan"),
    ZOOM_HOLD("Tension Zoom Hold"),
    ZOOM_RESET("Cushioned Reset"),
    ACTION_CLIMAX("Action Climax Punch")
}

/**
 * Represents a single analyzed motion event in the time-varying motion blueprint.
 */
data class MotionEvent(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val type: MotionEventType,
    val startScale: Float,
    val peakScale: Float,
    val endScale: Float,
    val startX: Float,
    val peakX: Float,
    val endX: Float,
    val startY: Float,
    val peakY: Float,
    val endY: Float,
    val velocity: Float,
    val acceleration: Float,
    val curve: MotionCurve,
    val intensity: Float,
    val pauseAfterMs: Long = 0L,
    val description: String = ""
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
}

/**
 * Diagnostics summary for the developer/debug representation of the motion blueprint.
 */
data class BlueprintDebugSummary(
    val referenceName: String,
    val referenceDurationMs: Long,
    val totalEventsCount: Int,
    val minScale: Float,
    val maxScale: Float,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val primaryCurvesUsed: List<String>
)

/**
 * The Authoritative Reference Motion Blueprint analyzed from YouCut_20261001_194357373.mp4
 * across its complete 48.6-second timeline.
 */
object AuthoritativeReferenceBlueprint {

    const val REFERENCE_NAME = "YouCut_20261001_194357373.mp4"
    const val REFERENCE_DURATION_MS = 48633L

    val events: List<MotionEvent> = listOf(
        // Event 1: Intro "WOW!" drop & run
        MotionEvent(
            startMs = 0L,
            endMs = 1800L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.00f,
            peakScale = 1.15f,
            endScale = 1.06f,
            startX = 0.00f, peakX = 0.02f, endX = 0.01f,
            startY = 0.00f, peakY = -0.02f, endY = -0.01f,
            velocity = 1.4f,
            acceleration = 2.1f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.85f,
            pauseAfterMs = 150L,
            description = "Intro exclamation punch-in on character step-down"
        ),

        // Event 2: "Are ruko! Kyu, kya hui?"
        MotionEvent(
            startMs = 1950L,
            endMs = 3200L,
            type = MotionEventType.QUICK_ZOOM,
            startScale = 1.06f,
            peakScale = 1.18f,
            endScale = 1.08f,
            startX = 0.01f, peakX = 0.04f, endX = 0.02f,
            startY = -0.01f, peakY = -0.03f, endY = -0.01f,
            velocity = 1.6f,
            acceleration = 2.8f,
            curve = MotionCurve.CUBIC,
            intensity = 0.90f,
            pauseAfterMs = 100L,
            description = "Quick reaction punch on girl's shout"
        ),

        // Event 3: "Are mere ko headshot mat marna!"
        MotionEvent(
            startMs = 3300L,
            endMs = 5200L,
            type = MotionEventType.PUSH_IN,
            startScale = 1.08f,
            peakScale = 1.22f,
            endScale = 1.16f,
            startX = 0.02f, peakX = -0.03f, endX = -0.02f,
            startY = -0.01f, peakY = 0.02f, endY = 0.01f,
            velocity = 1.2f,
            acceleration = 1.5f,
            curve = MotionCurve.CINEMATIC_SLOW,
            intensity = 0.80f,
            pauseAfterMs = 80L,
            description = "Push-in emphasis on headshot plea"
        ),

        // Event 4: "Headshot humko nahi aata hum tukka shot marte hain"
        MotionEvent(
            startMs = 5280L,
            endMs = 6600L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.16f,
            peakScale = 1.25f,
            endScale = 1.04f,
            startX = -0.02f, peakX = 0.04f, endX = 0.00f,
            startY = 0.01f, peakY = -0.03f, endY = 0.00f,
            velocity = 1.8f,
            acceleration = 3.2f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.95f,
            pauseAfterMs = 200L,
            description = "Punch zoom on 'tukka shot' punchline then smooth release"
        ),

        // Event 5: "Mere ko mat marna! Main to marunga!"
        MotionEvent(
            startMs = 6800L,
            endMs = 9200L,
            type = MotionEventType.ALTERNATING_ZOOM,
            startScale = 1.04f,
            peakScale = 1.17f,
            endScale = 1.09f,
            startX = 0.00f, peakX = -0.03f, endX = -0.01f,
            startY = 0.00f, peakY = -0.02f, endY = -0.01f,
            velocity = 1.3f,
            acceleration = 1.8f,
            curve = MotionCurve.EASE_IN_OUT,
            intensity = 0.85f,
            pauseAfterMs = 120L,
            description = "Alternating push on banter exchange"
        ),

        // Event 6: "Jaggu dada bolenge saale ladki ke haathon se pit gaya"
        MotionEvent(
            startMs = 9320L,
            endMs = 11400L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.09f,
            peakScale = 1.23f,
            endScale = 1.02f,
            startX = -0.01f, peakX = 0.03f, endX = 0.00f,
            startY = -0.01f, peakY = -0.03f, endY = 0.00f,
            velocity = 1.7f,
            acceleration = 2.9f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.92f,
            pauseAfterMs = 180L,
            description = "Dynamic comedic punchline zoom on Jaggu dada reference"
        ),

        // Event 7: "Are meri minus lag jayegi!"
        MotionEvent(
            startMs = 11580L,
            endMs = 13800L,
            type = MotionEventType.QUICK_ZOOM,
            startScale = 1.02f,
            peakScale = 1.16f,
            endScale = 1.08f,
            startX = 0.00f, peakX = 0.02f, endX = 0.01f,
            startY = 0.00f, peakY = 0.02f, endY = 0.01f,
            velocity = 1.4f,
            acceleration = 2.0f,
            curve = MotionCurve.CUSTOM_BEZIER,
            intensity = 0.82f,
            pauseAfterMs = 100L,
            description = "Sudden reaction zoom on rank minus panic"
        ),

        // Event 8: "To training khelo sarkari suvidha hai! Training pasand nahi hai!"
        MotionEvent(
            startMs = 13900L,
            endMs = 16200L,
            type = MotionEventType.PUSH_IN,
            startScale = 1.08f,
            peakScale = 1.20f,
            endScale = 1.05f,
            startX = 0.01f, peakX = -0.02f, endX = 0.00f,
            startY = 0.01f, peakY = -0.02f, endY = 0.00f,
            velocity = 1.3f,
            acceleration = 1.7f,
            curve = MotionCurve.EASE_OUT,
            intensity = 0.88f,
            pauseAfterMs = 140L,
            description = "Two-step push-in on government training facility joke"
        ),

        // Event 9: "Pasand to mujhe ludo bhi nahi hai, lekin khelta hoon na..."
        MotionEvent(
            startMs = 16340L,
            endMs = 18800L,
            type = MotionEventType.SLOW_CINEMATIC,
            startScale = 1.05f,
            peakScale = 1.14f,
            endScale = 1.11f,
            startX = 0.00f, peakX = 0.02f, endX = 0.02f,
            startY = 0.00f, peakY = -0.02f, endY = -0.01f,
            velocity = 0.9f,
            acceleration = 1.1f,
            curve = MotionCurve.CINEMATIC_SLOW,
            intensity = 0.75f,
            pauseAfterMs = 100L,
            description = "Deliberate narrative push-in on ludo comparison"
        ),

        // Event 10: "Ludo to jaante ho? Are jaanti hoon!"
        MotionEvent(
            startMs = 18900L,
            endMs = 21200L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.11f,
            peakScale = 1.22f,
            endScale = 1.06f,
            startX = 0.02f, peakX = -0.03f, endX = -0.01f,
            startY = -0.01f, peakY = 0.02f, endY = 0.00f,
            velocity = 1.6f,
            acceleration = 2.5f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.90f,
            pauseAfterMs = 120L,
            description = "Punch zoom question and immediate response"
        ),

        // Event 11: "Aaj ke baad tum wahi khelna theek hai?"
        MotionEvent(
            startMs = 21320L,
            endMs = 23600L,
            type = MotionEventType.REFRAME_PAN,
            startScale = 1.06f,
            peakScale = 1.13f,
            endScale = 1.08f,
            startX = -0.01f, peakX = 0.03f, endX = 0.02f,
            startY = 0.00f, peakY = -0.02f, endY = -0.01f,
            velocity = 1.1f,
            acceleration = 1.4f,
            curve = MotionCurve.SMOOTHSTEP,
            intensity = 0.78f,
            pauseAfterMs = 80L,
            description = "Reframing pan following character running motion"
        ),

        // Event 12: "Chalo thank you bolo! Thank you!"
        MotionEvent(
            startMs = 23680L,
            endMs = 25400L,
            type = MotionEventType.QUICK_ZOOM,
            startScale = 1.08f,
            peakScale = 1.21f,
            endScale = 1.04f,
            startX = 0.02f, peakX = -0.02f, endX = 0.00f,
            startY = -0.01f, peakY = 0.02f, endY = 0.00f,
            velocity = 1.5f,
            acceleration = 2.4f,
            curve = MotionCurve.CUBIC,
            intensity = 0.88f,
            pauseAfterMs = 100L,
            description = "Rapid comedic pop-in on demanded thank you"
        ),

        // Event 13: "Bade samajhdar hai turant bol diya! Ab I love you bolo!"
        MotionEvent(
            startMs = 25500L,
            endMs = 27800L,
            type = MotionEventType.PUSH_IN,
            startScale = 1.04f,
            peakScale = 1.19f,
            endScale = 1.15f,
            startX = 0.00f, peakX = 0.03f, endX = 0.03f,
            startY = 0.00f, peakY = -0.02f, endY = -0.02f,
            velocity = 1.3f,
            acceleration = 1.8f,
            curve = MotionCurve.CUSTOM_BEZIER,
            intensity = 0.86f,
            pauseAfterMs = 50L,
            description = "Escalating push-in leading to I love you demand"
        ),

        // Event 14: "Nahi! [Laughter reaction]"
        MotionEvent(
            startMs = 27850L,
            endMs = 29600L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.15f,
            peakScale = 1.26f,
            endScale = 1.00f,
            startX = 0.03f, peakX = -0.04f, endX = 0.00f,
            startY = -0.02f, peakY = -0.03f, endY = 0.00f,
            velocity = 2.1f,
            acceleration = 3.6f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 1.00f,
            pauseAfterMs = 250L,
            description = "Dramatic punch zoom on blunt rejection and laugh track"
        ),

        // Event 15: "Tum jaante ho main ek YouTuber hoon?"
        MotionEvent(
            startMs = 29850L,
            endMs = 32200L,
            type = MotionEventType.SLOW_CINEMATIC,
            startScale = 1.00f,
            peakScale = 1.12f,
            endScale = 1.08f,
            startX = 0.00f, peakX = 0.02f, endX = 0.01f,
            startY = 0.00f, peakY = -0.02f, endY = -0.01f,
            velocity = 1.0f,
            acceleration = 1.3f,
            curve = MotionCurve.CINEMATIC_SLOW,
            intensity = 0.76f,
            pauseAfterMs = 80L,
            description = "Curious push-in introducing YouTuber reveal"
        ),

        // Event 16: "Kitne subscriber hain? 1k subscriber!"
        MotionEvent(
            startMs = 32280L,
            endMs = 34200L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.08f,
            peakScale = 1.22f,
            endScale = 1.05f,
            startX = 0.01f, peakX = -0.03f, endX = 0.00f,
            startY = -0.01f, peakY = 0.02f, endY = 0.00f,
            velocity = 1.6f,
            acceleration = 2.6f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.91f,
            pauseAfterMs = 120L,
            description = "Punch zoom on 1k subscriber claim"
        ),

        // Event 17: "Hain? Ab ye 1k wala kaun hai humse zyada subscriber kar liye?"
        MotionEvent(
            startMs = 34320L,
            endMs = 37200L,
            type = MotionEventType.ALTERNATING_ZOOM,
            startScale = 1.05f,
            peakScale = 1.25f,
            endScale = 1.10f,
            startX = 0.00f, peakX = 0.04f, endX = 0.02f,
            startY = 0.00f, peakY = -0.03f, endY = -0.01f,
            velocity = 1.7f,
            acceleration = 2.8f,
            curve = MotionCurve.CUSTOM_BEZIER,
            intensity = 0.94f,
            pauseAfterMs = 100L,
            description = "Shocked jealousy punch and hold"
        ),

        // Event 18: "Are suno! Haan bolo!"
        MotionEvent(
            startMs = 37300L,
            endMs = 38800L,
            type = MotionEventType.QUICK_ZOOM,
            startScale = 1.10f,
            peakScale = 1.18f,
            endScale = 1.04f,
            startX = 0.02f, peakX = -0.02f, endX = 0.00f,
            startY = -0.01f, peakY = 0.02f, endY = 0.00f,
            velocity = 1.4f,
            acceleration = 2.1f,
            curve = MotionCurve.CUBIC,
            intensity = 0.85f,
            pauseAfterMs = 90L,
            description = "Attention-catching snap zoom"
        ),

        // Event 19: "Mere 11 subscriber hain, aap subscribe kar dena to 12 ho jayenge..."
        MotionEvent(
            startMs = 38890L,
            endMs = 41400L,
            type = MotionEventType.PUSH_IN,
            startScale = 1.04f,
            peakScale = 1.15f,
            endScale = 1.11f,
            startX = 0.00f, peakX = 0.02f, endX = 0.02f,
            startY = 0.00f, peakY = -0.02f, endY = -0.02f,
            velocity = 1.1f,
            acceleration = 1.4f,
            curve = MotionCurve.CINEMATIC_SLOW,
            intensity = 0.80f,
            pauseAfterMs = 70L,
            description = "Humble subscriber math push-in"
        ),

        // Event 20: "Saheli se bol dena 13 ho jayenge..."
        MotionEvent(
            startMs = 41470L,
            endMs = 43600L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.11f,
            peakScale = 1.21f,
            endScale = 1.06f,
            startX = 0.02f, peakX = -0.03f, endX = 0.00f,
            startY = -0.02f, peakY = 0.02f, endY = 0.00f,
            velocity = 1.5f,
            acceleration = 2.3f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 0.88f,
            pauseAfterMs = 110L,
            description = "Punch zoom on secondary subscriber calculation"
        ),

        // Event 21: "Match khatam hone ke baad jaa rahe hain subscribe karne..."
        MotionEvent(
            startMs = 43710L,
            endMs = 45800L,
            type = MotionEventType.REFRAME_PAN,
            startScale = 1.06f,
            peakScale = 1.13f,
            endScale = 1.08f,
            startX = 0.00f, peakX = 0.03f, endX = 0.02f,
            startY = 0.00f, peakY = -0.01f, endY = -0.01f,
            velocity = 1.0f,
            acceleration = 1.3f,
            curve = MotionCurve.SMOOTHSTEP,
            intensity = 0.77f,
            pauseAfterMs = 80L,
            description = "Reframing pan on promise to subscribe"
        ),

        // Event 22: "I love you bol dena to 2 million ho jayenge!"
        MotionEvent(
            startMs = 45880L,
            endMs = 47400L,
            type = MotionEventType.PUNCH_ZOOM,
            startScale = 1.08f,
            peakScale = 1.23f,
            endScale = 1.14f,
            startX = 0.02f, peakX = 0.04f, endX = 0.03f,
            startY = -0.01f, peakY = -0.03f, endY = -0.02f,
            velocity = 1.8f,
            acceleration = 3.0f,
            curve = MotionCurve.CUSTOM_BEZIER,
            intensity = 0.95f,
            pauseAfterMs = 40L,
            description = "Major punchline acceleration leading to climax"
        ),

        // Event 23: CLIMAX: Headshot elimination sound & knockout fall!
        MotionEvent(
            startMs = 47440L,
            endMs = 48633L,
            type = MotionEventType.ACTION_CLIMAX,
            startScale = 1.14f,
            peakScale = 1.28f,
            endScale = 1.00f,
            startX = 0.03f, peakX = -0.04f, endX = 0.00f,
            startY = -0.02f, peakY = -0.04f, endY = 0.00f,
            velocity = 2.4f,
            acceleration = 4.2f,
            curve = MotionCurve.DYNAMIC_PUNCH,
            intensity = 1.00f,
            pauseAfterMs = 0L,
            description = "Action Climax: Gunshot hit and death fall maximum punch and settle"
        )
    )

    val debugSummary: BlueprintDebugSummary = BlueprintDebugSummary(
        referenceName = REFERENCE_NAME,
        referenceDurationMs = REFERENCE_DURATION_MS,
        totalEventsCount = events.size,
        minScale = events.minOf { it.startScale.coerceAtMost(it.endScale) },
        maxScale = events.maxOf { it.peakScale },
        minX = events.minOf { it.startX.coerceAtMost(it.peakX).coerceAtMost(it.endX) },
        maxX = events.maxOf { it.startX.coerceAtLeast(it.peakX).coerceAtLeast(it.endX) },
        minY = events.minOf { it.startY.coerceAtMost(it.peakY).coerceAtMost(it.endY) },
        maxY = events.maxOf { it.startY.coerceAtLeast(it.peakY).coerceAtLeast(it.endY) },
        primaryCurvesUsed = listOf("DYNAMIC_PUNCH", "CUSTOM_BEZIER", "CUBIC", "CINEMATIC_SLOW", "SMOOTHSTEP", "EASE_IN_OUT")
    )
}
