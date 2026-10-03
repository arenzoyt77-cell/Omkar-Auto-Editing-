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
            existingClips = project.clips,
            audioAmplitudes = project.audioAmplitudes
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
            existingClips = project.clips,
            audioAmplitudes = project.audioAmplitudes
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
            existingClips = project.clips,
            audioAmplitudes = project.audioAmplitudes
        )

        return project.copy(
            splitPoints = updatedSplits,
            clips = updatedClips
        )
    }

    /**
     * Regenerates action-synchronized or reference-based motion across all clips.
     */
    fun regenerateMotion(
        project: VideoProject,
        mode: com.example.model.MotionMode
    ): VideoProject {
        return ActionMotionEngine.applyMotionToProject(project, mode)
    }

    /**
     * Adds an intermediate keyframe at the playhead position within a clip.
     */
    fun addIntermediateKeyframe(
        project: VideoProject,
        clipId: String,
        timestampMs: Long,
        scale: Float = 1.15f,
        positionX: Float = 0f,
        positionY: Float = 0f,
        rotation: Float = 0f,
        easing: MotionCurve = MotionCurve.DYNAMIC_PUNCH
    ): VideoProject {
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                KeyframeEngine.addIntermediateKeyframe(clip, timestampMs, scale, positionX, positionY, rotation, easing)
            } else clip
        }
        return project.copy(clips = updatedClips)
    }

    /**
     * Deletes an intermediate keyframe.
     */
    fun deleteKeyframe(
        project: VideoProject,
        clipId: String,
        keyframeId: String
    ): VideoProject {
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                KeyframeEngine.removeIntermediateKeyframe(clip, keyframeId)
            } else clip
        }
        return project.copy(clips = updatedClips)
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
     * Trims the start timestamp of a clip.
     */
    fun trimClipStart(
        project: VideoProject,
        clipId: String,
        newStartMs: Long
    ): VideoProject {
        val targetClip = project.clips.firstOrNull { it.id == clipId } ?: return project
        val clampedStart = newStartMs.coerceIn(0L, targetClip.endMs - 100L)
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    startMs = clampedStart,
                    startKeyframe = clip.startKeyframe.copy(timestampMs = clampedStart)
                )
            } else clip
        }
        return project.copy(clips = updatedClips)
    }

    /**
     * Trims the end timestamp of a clip.
     */
    fun trimClipEnd(
        project: VideoProject,
        clipId: String,
        newEndMs: Long
    ): VideoProject {
        val targetClip = project.clips.firstOrNull { it.id == clipId } ?: return project
        val clampedEnd = newEndMs.coerceIn(targetClip.startMs + 100L, project.durationMs)
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    endMs = clampedEnd,
                    endKeyframe = clip.endKeyframe.copy(timestampMs = clampedEnd)
                )
            } else clip
        }
        return project.copy(clips = updatedClips)
    }

    /**
     * Deletes a clip from the project.
     */
    fun deleteClip(
        project: VideoProject,
        clipId: String
    ): VideoProject {
        if (project.clips.size <= 1) return project // Keep at least one clip
        val remainingClips = project.clips.filterNot { it.id == clipId }
            .mapIndexed { idx, clip -> clip.copy(index = idx + 1) }
        return project.copy(clips = remainingClips)
    }

    /**
     * Duplicates a clip and inserts it immediately after.
     */
    fun duplicateClip(
        project: VideoProject,
        clipId: String
    ): VideoProject {
        val index = project.clips.indexOfFirst { it.id == clipId }
        if (index == -1) return project
        val clip = project.clips[index]
        val duplicated = clip.copy(
            id = UUID.randomUUID().toString(),
            index = index + 2,
            startKeyframe = clip.startKeyframe.copy(id = UUID.randomUUID().toString()),
            endKeyframe = clip.endKeyframe.copy(id = UUID.randomUUID().toString())
        )
        val mutable = project.clips.toMutableList()
        mutable.add(index + 1, duplicated)
        val reindexed = mutable.mapIndexed { idx, c -> c.copy(index = idx + 1) }
        return project.copy(clips = reindexed)
    }

    /**
     * Reorders a clip by moving it from fromIndex to toIndex.
     */
    fun reorderClips(
        project: VideoProject,
        fromIndex: Int,
        toIndex: Int
    ): VideoProject {
        if (fromIndex !in project.clips.indices || toIndex !in project.clips.indices) return project
        val mutable = project.clips.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val reindexed = mutable.mapIndexed { idx, c -> c.copy(index = idx + 1) }
        return project.copy(clips = reindexed)
    }

    /**
     * Sets mute status for a clip.
     */
    fun setClipMuted(
        project: VideoProject,
        clipId: String,
        isMuted: Boolean
    ): VideoProject {
        val updated = project.clips.map {
            if (it.id == clipId) it.copy(isMuted = isMuted) else it
        }
        return project.copy(clips = updated)
    }

    /**
     * Sets audio volume for a clip (0.0f - 2.0f).
     */
    fun setClipVolume(
        project: VideoProject,
        clipId: String,
        volume: Float
    ): VideoProject {
        val clamped = volume.coerceIn(0f, 2f)
        val updated = project.clips.map {
            if (it.id == clipId) it.copy(volume = clamped) else it
        }
        return project.copy(clips = updated)
    }

    /**
     * Text Overlay operations
     */
    fun addTextOverlay(
        project: VideoProject,
        overlay: com.example.model.TextOverlay
    ): VideoProject {
        val updated = (project.textOverlays + overlay).sortedBy { it.startMs }
        return project.copy(textOverlays = updated)
    }

    fun updateTextOverlay(
        project: VideoProject,
        overlay: com.example.model.TextOverlay
    ): VideoProject {
        val updated = project.textOverlays.map {
            if (it.id == overlay.id) overlay else it
        }.sortedBy { it.startMs }
        return project.copy(textOverlays = updated)
    }

    fun deleteTextOverlay(
        project: VideoProject,
        overlayId: String
    ): VideoProject {
        val updated = project.textOverlays.filterNot { it.id == overlayId }
        return project.copy(textOverlays = updated)
    }

    /**
     * Finds which clip covers the current playhead position.
     */
    fun findActiveClip(clips: List<ClipSegment>, playheadMs: Long): ClipSegment? {
        return clips.firstOrNull { playheadMs in it.startMs until it.endMs }
            ?: clips.lastOrNull { playheadMs >= it.startMs }
    }
}
