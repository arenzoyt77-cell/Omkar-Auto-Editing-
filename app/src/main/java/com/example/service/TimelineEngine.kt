package com.example.service

import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.SplitPoint
import com.example.model.VideoProject
import java.util.UUID

object TimelineEngine {

    private val segmentationService = VideoSegmentationService()

    /**
     * Adds a new split point at the given timestamp, ensuring it doesn't sever a spoken word
     * and snaps to a clean frame boundary.
     */
    fun addSplitPoint(
        project: VideoProject,
        timestampMs: Long
    ): VideoProject {
        val clampedTime = timestampMs.coerceIn(100L, project.durationMs - 100L)
        val allWords = project.speechSegments.flatMap { it.words }

        var safeTime = clampedTime
        for (word in allWords) {
            if (safeTime in word.startMs..word.endMs) {
                safeTime = word.endMs + 10L
                break
            }
        }

        val frameDurationMs = if (project.frameRate > 0) 1000.0 / project.frameRate else 33.333
        val frameIdx = (safeTime / frameDurationMs).toLong()
        val snappedTime = (frameIdx * frameDurationMs).toLong().coerceIn(100L, project.durationMs - 100L)

        // Don't add duplicate split within 300ms
        if (project.splitPoints.any { kotlin.math.abs(it.timestampMs - snappedTime) < 300L }) {
            return project
        }

        val newSplit = SplitPoint(
            id = UUID.randomUUID().toString(),
            timestampMs = snappedTime,
            reason = "Manual Split at ${snappedTime}ms",
            isAutomatic = false,
            frameIndex = frameIdx
        )

        val updatedSplits = (project.splitPoints + newSplit).sortedBy { it.timestampMs }
        val updatedClips = segmentationService.rebuildClipsFromSplits(
            splitPoints = updatedSplits,
            speechSegments = project.speechSegments,
            totalDurationMs = project.durationMs,
            existingClips = project.clips
        )

        return project.copy(
            splitPoints = updatedSplits,
            clips = updatedClips
        )
    }

    /**
     * Moves an existing split point to a new timestamp.
     */
    fun moveSplitPoint(
        project: VideoProject,
        splitId: String,
        newTimestampMs: Long
    ): VideoProject {
        val clampedTime = newTimestampMs.coerceIn(100L, project.durationMs - 100L)
        val allWords = project.speechSegments.flatMap { it.words }

        var safeTime = clampedTime
        for (word in allWords) {
            if (safeTime in word.startMs..word.endMs) {
                safeTime = word.endMs + 10L
                break
            }
        }

        val frameDurationMs = if (project.frameRate > 0) 1000.0 / project.frameRate else 33.333
        val frameIdx = (safeTime / frameDurationMs).toLong()
        val snappedTime = (frameIdx * frameDurationMs).toLong().coerceIn(100L, project.durationMs - 100L)

        val updatedSplits = project.splitPoints.map { split ->
            if (split.id == splitId) {
                split.copy(timestampMs = snappedTime, frameIndex = frameIdx)
            } else split
        }.sortedBy { it.timestampMs }

        val updatedClips = segmentationService.rebuildClipsFromSplits(
            splitPoints = updatedSplits,
            speechSegments = project.speechSegments,
            totalDurationMs = project.durationMs,
            existingClips = project.clips
        )

        return project.copy(
            splitPoints = updatedSplits,
            clips = updatedClips
        )
    }

    /**
     * Deletes a split point and merges the adjacent clips.
     */
    fun deleteSplitPoint(
        project: VideoProject,
        splitId: String
    ): VideoProject {
        val updatedSplits = project.splitPoints.filterNot { it.id == splitId }
        val updatedClips = segmentationService.rebuildClipsFromSplits(
            splitPoints = updatedSplits,
            speechSegments = project.speechSegments,
            totalDurationMs = project.durationMs,
            existingClips = project.clips
        )

        return project.copy(
            splitPoints = updatedSplits,
            clips = updatedClips
        )
    }

    /**
     * Modifies keyframe parameters for a specific clip.
     */
    fun updateClipKeyframe(
        project: VideoProject,
        clipId: String,
        keyframeId: String,
        scale: Float? = null,
        positionX: Float? = null,
        positionY: Float? = null,
        rotation: Float? = null,
        easing: MotionCurve? = null
    ): VideoProject {
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                KeyframeEngine.updateKeyframe(
                    clip = clip,
                    keyframeId = keyframeId,
                    scale = scale,
                    positionX = positionX,
                    positionY = positionY,
                    rotation = rotation,
                    easing = easing
                )
            } else clip
        }

        return project.copy(clips = updatedClips)
    }

    /**
     * Applies a motion preset to a specific clip.
     */
    fun setClipPreset(
        project: VideoProject,
        clipId: String,
        preset: MotionPreset
    ): VideoProject {
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                KeyframeEngine.applyPresetToClip(clip, preset)
            } else clip
        }

        return project.copy(clips = updatedClips)
    }

    /**
     * Finds which clip covers the current playhead position.
     */
    fun findActiveClip(clips: List<ClipSegment>, playheadMs: Long): ClipSegment? {
        return clips.firstOrNull { playheadMs in it.startMs until it.endMs }
            ?: clips.lastOrNull { playheadMs >= it.startMs }
    }
}
