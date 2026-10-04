package com.example.service

import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import java.util.UUID

object KeyframeEngine {

    /**
     * Generates a pair of start and end keyframes for a clip, automatically applying
     * subtle and cinematic default motion (e.g. 100% -> 108-115% scale with Ease-In-Out/Cinematic S-curve).
     * Alternates direction intelligently to maintain engaging video flow.
     */
    fun createDefaultKeyframesForClip(
        clipIndex: Int,
        startMs: Long,
        endMs: Long,
        preset: MotionPreset? = null
    ): Pair<Keyframe, Keyframe> {
        val chosenPreset = preset ?: if (clipIndex % 2 == 0) {
            MotionPreset.CINEMATIC_PUSH_IN
        } else {
            MotionPreset.GENTLE_PULL_OUT
        }

        val startKeyframe = Keyframe(
            id = UUID.randomUUID().toString(),
            timestampMs = startMs,
            scale = chosenPreset.startScale,
            positionX = 0f,
            positionY = 0f,
            rotation = 0f,
            easing = chosenPreset.defaultCurve
        )

        val endKeyframe = Keyframe(
            id = UUID.randomUUID().toString(),
            timestampMs = endMs,
            scale = chosenPreset.endScale,
            positionX = 0f,
            positionY = 0f,
            rotation = 0f,
            easing = chosenPreset.defaultCurve
        )

        return Pair(startKeyframe, endKeyframe)
    }

    /**
     * Applies a motion preset to an existing clip.
     */
    fun applyPresetToClip(clip: ClipSegment, preset: MotionPreset): ClipSegment {
        val startKeyframe = clip.startKeyframe.copy(
            scale = preset.startScale,
            positionX = 0f,
            positionY = 0f,
            rotation = 0f,
            easing = preset.defaultCurve
        )
        val endKeyframe = clip.endKeyframe.copy(
            scale = preset.endScale,
            positionX = 0f,
            positionY = 0f,
            rotation = 0f,
            easing = preset.defaultCurve
        )

        return clip.copy(
            startKeyframe = startKeyframe,
            endKeyframe = endKeyframe,
            intermediateKeyframes = emptyList(),
            motionPreset = preset
        )
    }

    /**
     * Updates keyframe values (scale, position, rotation, curve, or timestamp) for a clip.
     */
    fun updateKeyframe(
        clip: ClipSegment,
        keyframeId: String,
        scale: Float? = null,
        positionX: Float? = null,
        positionY: Float? = null,
        rotation: Float? = null,
        easing: MotionCurve? = null,
        timestampMs: Long? = null
    ): ClipSegment {
        if (clip.startKeyframe.id == keyframeId) {
            val updated = clip.startKeyframe.copy(
                scale = scale ?: clip.startKeyframe.scale,
                positionX = positionX ?: clip.startKeyframe.positionX,
                positionY = positionY ?: clip.startKeyframe.positionY,
                rotation = rotation ?: clip.startKeyframe.rotation,
                easing = easing ?: clip.startKeyframe.easing
            )
            return clip.copy(startKeyframe = updated, motionPreset = MotionPreset.CUSTOM)
        }

        if (clip.endKeyframe.id == keyframeId) {
            val updated = clip.endKeyframe.copy(
                scale = scale ?: clip.endKeyframe.scale,
                positionX = positionX ?: clip.endKeyframe.positionX,
                positionY = positionY ?: clip.endKeyframe.positionY,
                rotation = rotation ?: clip.endKeyframe.rotation,
                easing = easing ?: clip.endKeyframe.easing
            )
            return clip.copy(endKeyframe = updated, motionPreset = MotionPreset.CUSTOM)
        }

        val minTs = (clip.startMs + 40L).coerceAtMost(clip.endMs)
        val maxTs = (clip.endMs - 40L).coerceAtLeast(minTs)

        val updatedIntermediates = clip.intermediateKeyframes.map { kf ->
            if (kf.id == keyframeId) {
                kf.copy(
                    timestampMs = timestampMs?.coerceIn(minTs, maxTs) ?: kf.timestampMs,
                    scale = scale ?: kf.scale,
                    positionX = positionX ?: kf.positionX,
                    positionY = positionY ?: kf.positionY,
                    rotation = rotation ?: kf.rotation,
                    easing = easing ?: kf.easing
                )
            } else kf
        }.sortedBy { it.timestampMs }

        return clip.copy(
            intermediateKeyframes = updatedIntermediates,
            motionPreset = MotionPreset.CUSTOM
        )
    }

    /**
     * Adds an intermediate keyframe at a specified timestamp.
     */
    fun addIntermediateKeyframe(
        clip: ClipSegment,
        timestampMs: Long,
        scale: Float = 1.10f,
        positionX: Float = 0f,
        positionY: Float = 0f,
        rotation: Float = 0f,
        easing: MotionCurve = MotionCurve.EASE_IN_OUT
    ): ClipSegment {
        val clampedTime = timestampMs.coerceIn(clip.startMs + 50L, clip.endMs - 50L)
        val newKf = Keyframe(
            id = UUID.randomUUID().toString(),
            timestampMs = clampedTime,
            scale = scale,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            easing = easing
        )
        val list = clip.intermediateKeyframes.toMutableList()
        list.add(newKf)
        list.sortBy { it.timestampMs }

        return clip.copy(
            intermediateKeyframes = list,
            motionPreset = MotionPreset.CUSTOM
        )
    }

    /**
     * Removes an intermediate keyframe.
     */
    fun removeIntermediateKeyframe(clip: ClipSegment, keyframeId: String): ClipSegment {
        val list = clip.intermediateKeyframes.filterNot { it.id == keyframeId }
        return clip.copy(
            intermediateKeyframes = list,
            motionPreset = if (list.isEmpty()) clip.motionPreset else MotionPreset.CUSTOM
        )
    }
}
