package com.example.service

import com.example.model.AuthoritativeReferenceBlueprint
import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionEventType
import com.example.model.MotionMode
import com.example.model.MotionPreset
import com.example.model.SpeechSegment
import com.example.model.VideoProject
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

/**
 * ActionMotionEngine generates rich, multi-keyframe, action-synchronized motion
 * derived from the authoritative reference video (YouCut_20261001_194357373.mp4)
 * and tailored to the original video's speech cadence and audio energy.
 *
 * Eliminates the static 100% -> 105% zoom limitation by generating complex,
 * alternating zoom curves (e.g., 100% -> 118% -> 106% -> 124% -> 102%) with
 * synchronized X/Y camera reframing and expressive easing curves.
 */
object ActionMotionEngine {

    /**
     * Synthesizes multi-keyframe motion across all clips in the project according
     * to the selected MotionMode.
     */
    fun applyMotionToProject(
        project: VideoProject,
        mode: MotionMode = MotionMode.AUTO_MOTION
    ): VideoProject {
        if (project.clips.isEmpty()) return project

        val updatedClips = when (mode) {
            MotionMode.REFERENCE_MOTION -> generateReferenceBlueprintMotion(project)
            MotionMode.AUTO_MOTION -> generateActionSynchronizedMotion(project)
            MotionMode.CUSTOM_MOTION -> project.clips // Preserve custom keyframes
        }

        return project.copy(clips = updatedClips)
    }

    /**
     * Mode 1: REFERENCE MOTION
     * Maps the authoritative reference blueprint (YouCut_20261001_194357373.mp4) directly
     * across the target video's timeline, preserving the exact rhythm, velocity, and X/Y reframing.
     */
    private fun generateReferenceBlueprintMotion(project: VideoProject): List<ClipSegment> {
        val refEvents = AuthoritativeReferenceBlueprint.events
        val refDuration = AuthoritativeReferenceBlueprint.REFERENCE_DURATION_MS.toFloat()
        val targetDuration = project.effectiveDurationMs.coerceAtLeast(1000L).toFloat()
        val timeScale = targetDuration / refDuration

        return project.clips.mapIndexed { index, clip ->
            val clipStart = clip.startMs
            val clipEnd = clip.endMs
            val clipDuration = (clipEnd - clipStart).coerceAtLeast(200L)

            // Find overlapping reference events
            val normalizedStart = (clipStart.toFloat() / targetDuration) * refDuration
            val normalizedEnd = (clipEnd.toFloat() / targetDuration) * refDuration

            val matchingEvents = refEvents.filter {
                it.endMs >= normalizedStart && it.startMs <= normalizedEnd
            }

            if (matchingEvents.isNotEmpty()) {
                val primaryEvent = matchingEvents.maxByOrNull { it.intensity } ?: matchingEvents.first()
                generateMultiKeyframesFromEvent(clip, primaryEvent)
            } else {
                // Fallback cyclic pattern from reference
                val cyclicEvent = refEvents[index % refEvents.size]
                generateMultiKeyframesFromEvent(clip, cyclicEvent)
            }
        }
    }

    /**
     * Mode 2: AUTO MOTION (Action-Synchronized Editing)
     * Analyzes the original video's speech thoughts, acoustic amplitude bursts, and cadence
     * to place dynamic punch zooms on dialogue peaks and smooth cinematic push/pull on narrative sections.
     */
    private fun generateActionSynchronizedMotion(project: VideoProject): List<ClipSegment> {
        val amplitudes = project.audioAmplitudes
        val avgAmplitude = if (amplitudes.isNotEmpty()) amplitudes.average().toFloat() else 0.2f

        return project.clips.mapIndexed { index, clip ->
            val clipDuration = clip.durationMs.coerceAtLeast(300L)
            val clipSpeech = project.speechSegments.firstOrNull { it.startMs in clip.startMs..clip.endMs || clip.startMs in it.startMs..it.endMs }

            // Calculate acoustic energy density in this clip
            val clipEnergy = calculateClipEnergy(clip, project.durationMs, amplitudes)
            val isHighEnergy = clipEnergy > (avgAmplitude * 1.35f) || (clipSpeech != null && (clipSpeech.text.contains("!") || clipSpeech.text.contains("?")))

            val keyframes = mutableListOf<Keyframe>()

            // 1. Start Keyframe
            val startScale = if (index > 0 && index % 2 == 1) 1.05f else 1.00f
            keyframes.add(
                Keyframe(
                    id = UUID.randomUUID().toString(),
                    timestampMs = clip.startMs,
                    scale = startScale,
                    positionX = 0f,
                    positionY = 0f,
                    rotation = 0f,
                    easing = MotionCurve.CUSTOM_BEZIER
                )
            )

            if (isHighEnergy) {
                // HIGH-ENERGY MOMENT: Multi-keyframe punch zoom with X/Y reframing
                // e.g. 100% -> 122% Punch Peak -> 112% Secondary Peak -> 102% Settle
                val punchTime = clip.startMs + (clipDuration * 0.32f).toLong()
                val reboundTime = clip.startMs + (clipDuration * 0.68f).toLong()

                val panX = if (index % 2 == 0) 3.5f else -3.5f
                val panY = if (index % 3 == 0) -2.5f else 2.0f

                // Intermediate Keyframe 1: Peak Punch Zoom
                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = punchTime,
                        scale = (1.20f + (clipEnergy * 0.08f)).coerceIn(1.18f, 1.28f),
                        positionX = panX,
                        positionY = panY,
                        rotation = 0f,
                        easing = MotionCurve.DYNAMIC_PUNCH
                    )
                )

                // Intermediate Keyframe 2: Secondary Accent
                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = reboundTime,
                        scale = 1.10f,
                        positionX = -panX * 0.4f,
                        positionY = -panY * 0.3f,
                        rotation = 0f,
                        easing = MotionCurve.CUBIC
                    )
                )

                // End Keyframe
                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = clip.endMs,
                        scale = 1.02f,
                        positionX = 0f,
                        positionY = 0f,
                        rotation = 0f,
                        easing = MotionCurve.EASE_OUT
                    )
                )

            } else {
                // DIALOGUE / NARRATIVE MOMENT: Smooth push-in with intermediate hold & reframing
                // e.g. 100% -> 110% Push-In -> 114% Drift -> 106% Settle
                val midTime1 = clip.startMs + (clipDuration * 0.45f).toLong()
                val midTime2 = clip.startMs + (clipDuration * 0.80f).toLong()

                val panX = if (index % 2 == 0) 2.0f else -2.0f

                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = midTime1,
                        scale = 1.12f,
                        positionX = panX,
                        positionY = -1.5f,
                        rotation = 0f,
                        easing = MotionCurve.CINEMATIC_SLOW
                    )
                )

                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = midTime2,
                        scale = 1.15f,
                        positionX = panX * 1.2f,
                        positionY = -1.0f,
                        rotation = 0f,
                        easing = MotionCurve.SMOOTHSTEP
                    )
                )

                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = clip.endMs,
                        scale = 1.06f,
                        positionX = 0f,
                        positionY = 0f,
                        rotation = 0f,
                        easing = MotionCurve.EASE_IN_OUT
                    )
                )
            }

            val sorted = keyframes.sortedBy { it.timestampMs }
            val startKf = sorted.first()
            val endKf = sorted.last()
            val intermediates = if (sorted.size > 2) sorted.subList(1, sorted.size - 1) else emptyList()

            clip.copy(
                startKeyframe = startKf,
                endKeyframe = endKf,
                intermediateKeyframes = intermediates,
                motionPreset = MotionPreset.AUTO_ACTION
            )
        }
    }

    /**
     * Synthesizes 3 to 5 keyframes within a clip modeled after a specific MotionEvent.
     */
    private fun generateMultiKeyframesFromEvent(
        clip: ClipSegment,
        event: com.example.model.MotionEvent
    ): ClipSegment {
        val duration = clip.durationMs.coerceAtLeast(300L)
        val keyframes = mutableListOf<Keyframe>()

        // 1. Start Keyframe
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = clip.startMs,
                scale = event.startScale,
                positionX = event.startX * 100f,
                positionY = event.startY * 100f,
                rotation = 0f,
                easing = event.curve
            )
        )

        // 2. Peak Accent Keyframe (at 35% of duration)
        val peakTime = clip.startMs + (duration * 0.35f).toLong()
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = peakTime,
                scale = event.peakScale,
                positionX = event.peakX * 100f,
                positionY = event.peakY * 100f,
                rotation = 0f,
                easing = event.curve
            )
        )

        // 3. Secondary Settle / Rebound Keyframe (at 72% of duration)
        if (duration > 800L) {
            val reboundTime = clip.startMs + (duration * 0.72f).toLong()
            val midScale = (event.peakScale + event.endScale) / 2f
            keyframes.add(
                Keyframe(
                    id = UUID.randomUUID().toString(),
                    timestampMs = reboundTime,
                    scale = midScale,
                    positionX = (event.peakX + event.endX) * 50f,
                    positionY = (event.peakY + event.endY) * 50f,
                    rotation = 0f,
                    easing = MotionCurve.CUBIC
                )
            )
        }

        // 4. End Keyframe
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = clip.endMs,
                scale = event.endScale,
                positionX = event.endX * 100f,
                positionY = event.endY * 100f,
                rotation = 0f,
                easing = MotionCurve.EASE_OUT
            )
        )

        val sorted = keyframes.sortedBy { it.timestampMs }
        val startKf = sorted.first()
        val endKf = sorted.last()
        val intermediates = if (sorted.size > 2) sorted.subList(1, sorted.size - 1) else emptyList()

        return clip.copy(
            startKeyframe = startKf,
            endKeyframe = endKf,
            intermediateKeyframes = intermediates,
            motionPreset = MotionPreset.REFERENCE_MOTION
        )
    }

    private fun calculateClipEnergy(
        clip: ClipSegment,
        totalDurationMs: Long,
        amplitudes: List<Float>
    ): Float {
        if (amplitudes.isEmpty() || totalDurationMs <= 0) return 0.2f
        val startFraction = (clip.startMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val endFraction = (clip.endMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

        val startIdx = (startFraction * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
        val endIdx = (endFraction * amplitudes.size).toInt().coerceIn(startIdx, amplitudes.size)

        val subList = amplitudes.subList(startIdx, endIdx)
        return if (subList.isNotEmpty()) subList.average().toFloat() else 0.2f
    }
}
