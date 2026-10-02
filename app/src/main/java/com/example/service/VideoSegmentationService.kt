package com.example.service

import com.example.model.ClipSegment
import com.example.model.MotionPreset
import com.example.model.SpeechSegment
import com.example.model.SplitPoint
import java.util.UUID
import kotlin.math.roundToLong

class VideoSegmentationService {

    /**
     * Segments video based on speech-to-text semantic thought boundaries,
     * ensuring splits align to exact video frame boundaries without cutting spoken words.
     */
    fun generateSegmentation(
        speechSegments: List<SpeechSegment>,
        totalDurationMs: Long,
        frameRate: Float = 30.0f
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
            // Check if candidate collides with any spoken word
            var adjustedTime = candidate
            for (word in allWords) {
                if (adjustedTime in word.startMs..word.endMs) {
                    // Snap cleanly to after the word finishes
                    adjustedTime = word.endMs + 10L
                    break
                }
            }

            // Snap to nearest discrete video frame
            val frameIndex = (adjustedTime / frameDurationMs).roundToLong()
            val snappedTime = (frameIndex * frameDurationMs).roundToLong().coerceIn(100L, totalDurationMs - 100L)

            // Prevent split points too close together (< 1.5 seconds)
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
                reason = "Spoken Thought Boundary #$index",
                isAutomatic = true,
                frameIndex = frameIdx
            )
        }

        // Step 4: Build gapless, non-overlapping ClipSegments
        val clips = mutableListOf<ClipSegment>()
        val boundaries = mutableListOf<Long>()
        boundaries.add(0L)
        boundaries.addAll(validatedSplitTimestamps)
        boundaries.add(totalDurationMs)

        for (i in 0 until boundaries.size - 1) {
            val start = boundaries[i]
            val end = boundaries[i + 1]
            if (end <= start) continue

            // Associate speech text within this clip range
            val matchingSpeech = speechSegments
                .filter { it.startMs < end && it.endMs > start }
                .joinToString(" ") { it.text }

            val preset = if (i % 2 == 0) MotionPreset.CINEMATIC_PUSH_IN else MotionPreset.GENTLE_PULL_OUT
            val (startKf, endKf) = KeyframeEngine.createDefaultKeyframesForClip(
                clipIndex = i,
                startMs = start,
                endMs = end,
                preset = preset
            )

            clips.add(
                ClipSegment(
                    id = UUID.randomUUID().toString(),
                    index = i + 1,
                    startMs = start,
                    endMs = end,
                    speechText = matchingSpeech.ifEmpty { "Clip ${i + 1}" },
                    startKeyframe = startKf,
                    endKeyframe = endKf,
                    motionPreset = preset
                )
            )
        }

        return Pair(splitPoints, clips)
    }

    /**
     * Rebuilds clips from an updated list of split points (e.g. after user adds, moves, or deletes a split).
     */
    fun rebuildClipsFromSplits(
        splitPoints: List<SplitPoint>,
        speechSegments: List<SpeechSegment>,
        totalDurationMs: Long,
        existingClips: List<ClipSegment> = emptyList()
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
            val preset = existingMatch?.motionPreset
                ?: if (i % 2 == 0) MotionPreset.CINEMATIC_PUSH_IN else MotionPreset.GENTLE_PULL_OUT

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
                    startKeyframe = startKf,
                    endKeyframe = endKf,
                    motionPreset = preset
                )
            )
        }

        return newClips
    }
}
