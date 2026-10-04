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
     * Maps the active cached reference blueprint (authoritative YouCut_20261001_194357373.mp4 or custom)
     * across the target video's timeline, preserving the exact rhythm, velocity, and X/Y reframing.
     */
    private fun generateReferenceBlueprintMotion(project: VideoProject): List<ClipSegment> {
        val refEvents = ReferenceMotionCache.getActiveEvents().ifEmpty { AuthoritativeReferenceBlueprint.events }
        val refSummary = ReferenceMotionCache.getActiveSummary()
        val refDuration = refSummary.referenceDurationMs.coerceAtLeast(1000L).toFloat()
        val targetDuration = project.effectiveDurationMs.coerceAtLeast(1000L).toFloat()

        return project.clips.mapIndexed { index, clip ->
            val clipStart = clip.startMs
            val clipEnd = clip.endMs

            // Find overlapping reference events
            val normalizedStart = (clipStart.toFloat() / targetDuration) * refDuration
            val normalizedEnd = (clipEnd.toFloat() / targetDuration) * refDuration

            val matchingEvents = refEvents.filter {
                it.endMs >= normalizedStart && it.startMs <= normalizedEnd
            }

            if (matchingEvents.size >= 2 && clip.durationMs >= 1600L) {
                generateMultiEventKeyframes(clip, matchingEvents.take(3))
            } else if (matchingEvents.isNotEmpty()) {
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
     * to place dynamic punch zooms, push-ins, pull-outs, alternating zooms, and zoom reversals
     * with synchronized X/Y camera reframing and variable easing curves.
     */
    private fun generateActionSynchronizedMotion(project: VideoProject): List<ClipSegment> {
        val amplitudes = project.audioAmplitudes
        val avgAmplitude = if (amplitudes.isNotEmpty()) amplitudes.average().toFloat() else 0.2f
        val refEvents = AuthoritativeReferenceBlueprint.events

        return project.clips.mapIndexed { index, clip ->
            val clipDuration = clip.durationMs.coerceAtLeast(300L)
            val clipSpeech = project.speechSegments.firstOrNull {
                it.startMs in clip.startMs..clip.endMs || clip.startMs in it.startMs..it.endMs
            }

            // Calculate acoustic energy density and peak position in this clip
            val clipEnergy = calculateClipEnergy(clip, project.durationMs, amplitudes)
            val peakFraction = findPeakEnergyFraction(clip, project.durationMs, amplitudes)
            val hasExclamation = clipSpeech?.text?.let { it.contains("!") || it.contains("?") } == true
            val isHighEnergy = clipEnergy > (avgAmplitude * 1.25f) || hasExclamation

            // Also draw inspiration from the authoritative reference event at this relative timeline position
            val refEvent = refEvents[index % refEvents.size]
            val keyframes = mutableListOf<Keyframe>()

            val patternIndex = index % 5
            when {
                isHighEnergy && patternIndex % 2 == 0 -> {
                    // PATTERN A: Action Climax / Dynamic Punch Zoom + Settle + Secondary Accent
                    val punchRatio = peakFraction.coerceIn(0.25f, 0.45f)
                    val settleRatio = (punchRatio + 0.33f).coerceIn(0.60f, 0.82f)
                    val punchTime = clip.startMs + (clipDuration * punchRatio).toLong()
                    val settleTime = clip.startMs + (clipDuration * settleRatio).toLong()

                    val panX = if (index % 2 == 0) 3.8f else -3.8f
                    val panY = if (index % 3 == 0) -2.8f else 2.2f
                    val peakZoom = (1.21f + (clipEnergy * 0.07f)).coerceIn(1.19f, 1.28f)

                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.startMs,
                            scale = refEvent.startScale.coerceIn(1.00f, 1.08f),
                            positionX = refEvent.startX * 100f,
                            positionY = refEvent.startY * 100f,
                            rotation = 0f,
                            easing = MotionCurve.DYNAMIC_PUNCH
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = punchTime,
                            scale = peakZoom,
                            positionX = panX,
                            positionY = panY,
                            rotation = 0f,
                            easing = MotionCurve.CUBIC
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = settleTime,
                            scale = 1.09f,
                            positionX = -panX * 0.45f,
                            positionY = -panY * 0.35f,
                            rotation = 0f,
                            easing = MotionCurve.CUSTOM_BEZIER
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.endMs,
                            scale = 1.15f,
                            positionX = panX * 0.25f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_OUT
                        )
                    )
                }

                patternIndex == 1 -> {
                    // PATTERN B: Alternating Rhythm Zoom (Zoom In -> Pull Out -> Re-Punch)
                    val t1 = clip.startMs + (clipDuration * 0.28f).toLong()
                    val t2 = clip.startMs + (clipDuration * 0.58f).toLong()
                    val t3 = clip.startMs + (clipDuration * 0.82f).toLong()

                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.startMs,
                            scale = 1.04f,
                            positionX = -1.5f,
                            positionY = 1.0f,
                            rotation = 0f,
                            easing = MotionCurve.CUSTOM_BEZIER
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t1,
                            scale = 1.19f,
                            positionX = 3.0f,
                            positionY = -2.0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_IN_OUT
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t2,
                            scale = 1.06f,
                            positionX = -2.2f,
                            positionY = 1.2f,
                            rotation = 0f,
                            easing = MotionCurve.DYNAMIC_PUNCH
                        )
                    )
                    if (clipDuration > 1100L) {
                        keyframes.add(
                            Keyframe(
                                id = UUID.randomUUID().toString(),
                                timestampMs = t3,
                                scale = 1.22f,
                                positionX = 2.5f,
                                positionY = -1.8f,
                                rotation = 0f,
                                easing = MotionCurve.EASE_OUT
                            )
                        )
                    }
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.endMs,
                            scale = 1.08f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_OUT
                        )
                    )
                }

                patternIndex == 2 -> {
                    // PATTERN C: Gentle Pull-Out from Close-Up + Zoom Reversal Push
                    val t1 = clip.startMs + (clipDuration * 0.42f).toLong()
                    val t2 = clip.startMs + (clipDuration * 0.76f).toLong()

                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.startMs,
                            scale = 1.20f,
                            positionX = 2.8f,
                            positionY = -2.0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_OUT
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t1,
                            scale = 1.03f,
                            positionX = -1.8f,
                            positionY = 1.0f,
                            rotation = 0f,
                            easing = MotionCurve.SMOOTHSTEP
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t2,
                            scale = 1.14f,
                            positionX = 1.5f,
                            positionY = -1.2f,
                            rotation = 0f,
                            easing = MotionCurve.CINEMATIC_SLOW
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.endMs,
                            scale = 1.05f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_IN_OUT
                        )
                    )
                }

                patternIndex == 3 -> {
                    // PATTERN D: Quick Reaction Zoom + Reframing Pan + Cushioned Reset
                    val t1 = clip.startMs + (clipDuration * 0.30f).toLong()
                    val t2 = clip.startMs + (clipDuration * 0.66f).toLong()

                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.startMs,
                            scale = 1.02f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.CUBIC
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t1,
                            scale = 1.23f,
                            positionX = -3.4f,
                            positionY = -2.4f,
                            rotation = 0f,
                            easing = MotionCurve.DYNAMIC_PUNCH
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = t2,
                            scale = 1.17f,
                            positionX = 2.8f,
                            positionY = 1.6f,
                            rotation = 0f,
                            easing = MotionCurve.SMOOTHSTEP
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.endMs,
                            scale = 1.00f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_OUT
                        )
                    )
                }

                else -> {
                    // PATTERN E: Escalating Two-Step Push-In with X/Y Drift & Release
                    val midTime1 = clip.startMs + (clipDuration * 0.38f).toLong()
                    val midTime2 = clip.startMs + (clipDuration * 0.74f).toLong()
                    val panX = if (index % 2 == 0) 2.5f else -2.5f

                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.startMs,
                            scale = 1.00f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.CINEMATIC_SLOW
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = midTime1,
                            scale = 1.14f,
                            positionX = panX,
                            positionY = -1.8f,
                            rotation = 0f,
                            easing = MotionCurve.CUSTOM_BEZIER
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = midTime2,
                            scale = 1.21f,
                            positionX = -panX * 0.8f,
                            positionY = 1.4f,
                            rotation = 0f,
                            easing = MotionCurve.SMOOTHSTEP
                        )
                    )
                    keyframes.add(
                        Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = clip.endMs,
                            scale = 1.07f,
                            positionX = 0f,
                            positionY = 0f,
                            rotation = 0f,
                            easing = MotionCurve.EASE_IN_OUT
                        )
                    )
                }
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
     * Synthesizes a multi-event keyframe sequence when a clip spans 2-3 reference events.
     */
    private fun generateMultiEventKeyframes(
        clip: ClipSegment,
        events: List<com.example.model.MotionEvent>
    ): ClipSegment {
        val duration = clip.durationMs.coerceAtLeast(400L)
        val keyframes = mutableListOf<Keyframe>()
        val firstEvent = events.first()
        val lastEvent = events.last()

        // Start keyframe
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = clip.startMs,
                scale = firstEvent.startScale,
                positionX = firstEvent.startX * 100f,
                positionY = firstEvent.startY * 100f,
                rotation = 0f,
                easing = firstEvent.curve
            )
        )

        val stepSpan = duration / events.size.coerceAtLeast(1)
        events.forEachIndexed { idx, ev ->
            val segStart = clip.startMs + idx * stepSpan
            val peakTs = (segStart + (stepSpan * 0.42f).toLong()).coerceIn(clip.startMs + 60L, clip.endMs - 60L)
            keyframes.add(
                Keyframe(
                    id = UUID.randomUUID().toString(),
                    timestampMs = peakTs,
                    scale = ev.peakScale,
                    positionX = ev.peakX * 100f,
                    positionY = ev.peakY * 100f,
                    rotation = 0f,
                    easing = ev.curve
                )
            )
            if (idx < events.size - 1) {
                val valleyTs = (segStart + (stepSpan * 0.85f).toLong()).coerceIn(clip.startMs + 80L, clip.endMs - 80L)
                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = valleyTs,
                        scale = ev.endScale,
                        positionX = ev.endX * 100f,
                        positionY = ev.endY * 100f,
                        rotation = 0f,
                        easing = MotionCurve.EASE_IN_OUT
                    )
                )
            }
        }

        // End keyframe
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = clip.endMs,
                scale = lastEvent.endScale,
                positionX = lastEvent.endX * 100f,
                positionY = lastEvent.endY * 100f,
                rotation = 0f,
                easing = MotionCurve.EASE_OUT
            )
        )

        val sorted = keyframes.sortedBy { it.timestampMs }.distinctBy { it.timestampMs }
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

    private fun findPeakEnergyFraction(
        clip: ClipSegment,
        totalDurationMs: Long,
        amplitudes: List<Float>
    ): Float {
        if (amplitudes.isEmpty() || totalDurationMs <= 0) return 0.35f
        val startFraction = (clip.startMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val endFraction = (clip.endMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

        val startIdx = (startFraction * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
        val endIdx = (endFraction * amplitudes.size).toInt().coerceIn(startIdx, amplitudes.size)
        val subList = amplitudes.subList(startIdx, endIdx)
        if (subList.isEmpty()) return 0.35f

        var maxIdx = 0
        var maxVal = -1f
        subList.forEachIndexed { idx, value ->
            if (value > maxVal) {
                maxVal = value
                maxIdx = idx
            }
        }
        return (maxIdx.toFloat() / subList.size.coerceAtLeast(1).toFloat()).coerceIn(0.25f, 0.50f)
    }
}
