package com.example.service

import com.example.model.ClipSegment
import com.example.model.MotionMode
import com.example.model.MotionPreset
import com.example.model.SpeechSegment
import com.example.model.SplitPoint
import com.example.model.VideoProject
import java.util.UUID
import kotlin.math.roundToLong

class VideoSegmentationService {

    /**
     * Segments video based on speech-to-text semantic thought boundaries,
     * snaps splits to discrete video frames, and synthesizes action-synchronized
     * multi-keyframe motion based on the authoritative reference blueprint.
     */
    fun generateSegmentation(
        speechSegments: List<SpeechSegment>,
        totalDurationMs: Long,
        frameRate: Float = 30.0f,
        audioAmplitudes: List<Float> = emptyList(),
        motionMode: MotionMode = MotionMode.AUTO_MOTION
    ): Pair<List<SplitPoint>, List<ClipSegment>> {
        val frameDurationMs = if (frameRate > 0) 1000.0 / frameRate else 33.333

        // Step 1: Collect raw split points from speech boundaries
        val rawSplitCandidates = mutableListOf<Long>()

        for (i in 0 until speechSegments.size - 1) {
            val currentSegment = speechSegments[i]
            val nextSegment = speechSegments[i + 1]

            // Place split in the natural pause between thoughts
            val pauseMidpoint = currentSegment.endMs + (currentSegment.pauseDurationAfterMs / 2)
            rawSplitCandidates.add(pauseMidpoint.coerceAtMost(nextSegment.startMs))
        }

        // Collect all words across all segments to guarantee no word is severed
        val allWords = speechSegments.flatMap { it.words }

        // Step 2: Validate and snap each split candidate to an exact video frame
        val validatedSplitTimestamps = mutableListOf<Long>()

        for (candidate in rawSplitCandidates) {
            var adjustedTime = candidate
            for (word in allWords) {
                if (adjustedTime in word.startMs..word.endMs) {
                    adjustedTime = word.endMs + 10L
                    break
                }
            }

            val frameIndex = (adjustedTime / frameDurationMs).roundToLong()
            val snappedTime = (frameIndex * frameDurationMs).roundToLong().coerceIn(100L, totalDurationMs - 100L)

            val isTooClose = validatedSplitTimestamps.any { kotlin.math.abs(it - snappedTime) < 1500L }
            if (!isTooClose) {
                validatedSplitTimestamps.add(snappedTime)
            }
        }

        validatedSplitTimestamps.sort()

        // Step 3: Create SplitPoint models
        val splitPoints = validatedSplitTimestamps.mapIndexed { index, ts ->
            val frameIdx = (ts / frameDurationMs).roundToLong()
            SplitPoint(
                id = UUID.randomUUID().toString(),
                timestampMs = ts,
                reason = "Spoken Thought Boundary #${index + 1}",
                isAutomatic = true,
                frameIndex = frameIdx
            )
        }

        // Step 4: Build initial non-overlapping ClipSegments
        val initialClips = mutableListOf<ClipSegment>()
        val boundaries = mutableListOf<Long>()
        boundaries.add(0L)
        boundaries.addAll(validatedSplitTimestamps)
        boundaries.add(totalDurationMs)

        for (i in 0 until boundaries.size - 1) {
            val start = boundaries[i]
            val end = boundaries[i + 1]
            if (end <= start) continue

            val matchingSpeech = speechSegments
                .filter { it.startMs < end && it.endMs > start }
                .joinToString(" ") { it.text }

            val (startKf, endKf) = KeyframeEngine.createDefaultKeyframesForClip(
                clipIndex = i,
                startMs = start,
                endMs = end,
                preset = MotionPreset.AUTO_ACTION
            )

            initialClips.add(
                ClipSegment(
                    id = UUID.randomUUID().toString(),
                    index = i + 1,
                    startMs = start,
                    endMs = end,
                    speechText = matchingSpeech.ifEmpty { "Clip ${i + 1}" },
                    startKeyframe = startKf,
                    endKeyframe = endKf,
                    motionPreset = MotionPreset.AUTO_ACTION
                )
            )
        }

        // Step 5: Synthesize rich, multi-keyframe motion using ActionMotionEngine
        val dummyProject = VideoProject(
            durationMs = totalDurationMs,
            speechSegments = speechSegments,
            splitPoints = splitPoints,
            clips = initialClips,
            audioAmplitudes = audioAmplitudes
        )

        val motionAppliedProject = ActionMotionEngine.applyMotionToProject(dummyProject, motionMode)

        return Pair(splitPoints, motionAppliedProject.clips)
    }

    /**
     * Rebuilds clips from an updated list of split points while maintaining motion styling.
     */
    fun rebuildClipsFromSplits(
        splitPoints: List<SplitPoint>,
        speechSegments: List<SpeechSegment>,
        totalDurationMs: Long,
        existingClips: List<ClipSegment> = emptyList(),
        audioAmplitudes: List<Float> = emptyList(),
        motionMode: MotionMode = MotionMode.AUTO_MOTION
    ): List<ClipSegment> {
        val sortedSplits = splitPoints.map { it.timestampMs }.sorted()
        val boundaries = mutableListOf<Long>()
        boundaries.add(0L)
        boundaries.addAll(sortedSplits)
        boundaries.add(totalDurationMs)

        val newClips = mutableListOf<ClipSegment>()

        for (i in 0 until boundaries.size - 1) {
            val start = boundaries[i]
            val end = boundaries[i + 1]
            if (end <= start) continue

            val matchingSpeech = speechSegments
                .filter { it.startMs < end && it.endMs > start }
                .joinToString(" ") { it.text }

            val existingMatch = existingClips.getOrNull(i)
            val preset = existingMatch?.motionPreset ?: MotionPreset.AUTO_ACTION

            val (startKf, endKf) = KeyframeEngine.createDefaultKeyframesForClip(
                clipIndex = i,
                startMs = start,
                endMs = end,
                preset = preset
            )

            newClips.add(
                ClipSegment(
                    id = existingMatch?.id ?: UUID.randomUUID().toString(),
                    index = i + 1,
                    startMs = start,
                    endMs = end,
                    speechText = matchingSpeech.ifEmpty { "Clip ${i + 1}" },
                    startKeyframe = existingMatch?.startKeyframe ?: startKf,
                    endKeyframe = existingMatch?.endKeyframe ?: endKf,
                    intermediateKeyframes = existingMatch?.intermediateKeyframes ?: emptyList(),
                    motionPreset = preset
                )
            )
        }

        val dummyProject = VideoProject(
            durationMs = totalDurationMs,
            speechSegments = speechSegments,
            splitPoints = splitPoints,
            clips = newClips,
            audioAmplitudes = audioAmplitudes
        )

        return ActionMotionEngine.applyMotionToProject(dummyProject, motionMode).clips
    }
}
