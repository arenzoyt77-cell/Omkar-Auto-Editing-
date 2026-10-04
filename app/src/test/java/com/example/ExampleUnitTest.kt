package com.example

import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.SpeechSegment
import com.example.model.WordTimestamp
import com.example.service.KeyframeEngine
import com.example.service.MotionInterpolationEngine
import com.example.service.TimelineEngine
import com.example.service.VideoSegmentationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMotionCurveInterpolation() {
        // Linear
        assertEquals(0.0f, MotionCurve.LINEAR.interpolate(0.0f), 0.001f)
        assertEquals(0.5f, MotionCurve.LINEAR.interpolate(0.5f), 0.001f)
        assertEquals(1.0f, MotionCurve.LINEAR.interpolate(1.0f), 0.001f)

        // Ease In-Out
        assertEquals(0.0f, MotionCurve.EASE_IN_OUT.interpolate(0.0f), 0.001f)
        assertEquals(0.5f, MotionCurve.EASE_IN_OUT.interpolate(0.5f), 0.001f)
        assertEquals(1.0f, MotionCurve.EASE_IN_OUT.interpolate(1.0f), 0.001f)

        // Cinematic Slow
        assertEquals(0.0f, MotionCurve.CINEMATIC_SLOW.interpolate(0.0f), 0.001f)
        assertEquals(0.5f, MotionCurve.CINEMATIC_SLOW.interpolate(0.5f), 0.001f)
        assertEquals(1.0f, MotionCurve.CINEMATIC_SLOW.interpolate(1.0f), 0.001f)
    }

    @Test
    fun testKeyframeInterpolationAtMidpoint() {
        val (startKf, endKf) = KeyframeEngine.createDefaultKeyframesForClip(
            clipIndex = 0,
            startMs = 0L,
            endMs = 4000L,
            preset = MotionPreset.CINEMATIC_PUSH_IN
        )

        val clip = com.example.model.ClipSegment(
            index = 1,
            startMs = 0L,
            endMs = 4000L,
            speechText = "Test Thought",
            startKeyframe = startKf,
            endKeyframe = endKf
        )

        // Interpolate at t=0
        val t0 = MotionInterpolationEngine.interpolateAt(clip, 0L)
        assertEquals(1.00f, t0.scale, 0.01f)

        // Interpolate at midpoint t=2000
        val tMid = MotionInterpolationEngine.interpolateAt(clip, 2000L)
        assertTrue(tMid.scale in 1.01f..1.11f)

        // Interpolate at end t=4000
        val tEnd = MotionInterpolationEngine.interpolateAt(clip, 4000L)
        assertEquals(1.12f, tEnd.scale, 0.01f)
    }

    @Test
    fun testVideoSegmentationDoesNotCutWords() {
        val word1 = WordTimestamp("Today", 0L, 500L)
        val word2 = WordTimestamp("I", 520L, 700L)
        val word3 = WordTimestamp("am", 720L, 900L)
        val word4 = WordTimestamp("finishing.", 920L, 1800L)

        val speech1 = SpeechSegment(
            text = "Today I am finishing.",
            startMs = 0L,
            endMs = 1800L,
            words = listOf(word1, word2, word3, word4),
            pauseDurationAfterMs = 400L
        )

        val nextWord = WordTimestamp("Next", 2220L, 2600L)
        val speech2 = SpeechSegment(
            text = "Next trick is here.",
            startMs = 2220L,
            endMs = 4000L,
            words = listOf(nextWord),
            pauseDurationAfterMs = 300L
        )

        val segmentation = VideoSegmentationService()
        val (splits, clips) = segmentation.generateSegmentation(
            speechSegments = listOf(speech1, speech2),
            totalDurationMs = 5000L,
            frameRate = 30.0f
        )

        assertTrue(splits.isNotEmpty())
        val splitTime = splits.first().timestampMs
        // The split time must NOT fall within any word!
        val fallsInWord1 = splitTime in word1.startMs..word1.endMs
        val fallsInWord4 = splitTime in word4.startMs..word4.endMs
        val fallsInNext = splitTime in nextWord.startMs..nextWord.endMs

        assertTrue(!fallsInWord1 && !fallsInWord4 && !fallsInNext)
        assertEquals(2, clips.size)
    }

    @Test
    fun testReferenceAndDynamicMotionEnginesProduceMultiKeyframeDynamicMotion() {
        val segmentation = VideoSegmentationService()
        val (splits, baseClips) = segmentation.generateSegmentation(
            speechSegments = emptyList(),
            totalDurationMs = 15000L,
            frameRate = 30.0f
        )

        val amplitudes = List(150) { idx -> if (idx % 15 == 5) 0.92f else 0.35f }
        val baseProject = com.example.model.VideoProject(
            durationMs = 15000L,
            splitPoints = splits,
            clips = baseClips,
            audioAmplitudes = amplitudes
        )

        // 1. Test Reference Motion Mode (Authoritative YouCut_20261001_194357373.mp4 blueprint)
        val refProject = com.example.service.ActionMotionEngine.applyMotionToProject(
            project = baseProject,
            mode = com.example.model.MotionMode.REFERENCE_MOTION
        )
        val refClips = refProject.clips
        assertEquals(baseClips.size, refClips.size)
        // Verify multi-keyframe dynamic motion (not a simple 100% -> 105% static zoom)
        val hasMultiKeyframesRef = refClips.any { it.intermediateKeyframes.isNotEmpty() }
        val maxScaleRef = refClips.flatMap { it.allKeyframes() }.maxOf { it.scale }
        val hasXYReframingRef = refClips.flatMap { it.allKeyframes() }.any { kotlin.math.abs(it.positionX) > 0.1f || kotlin.math.abs(it.positionY) > 0.1f }
        assertTrue("Reference motion must generate intermediate keyframes", hasMultiKeyframesRef)
        assertTrue("Reference motion must reach dynamic zoom levels above 1.15x", maxScaleRef > 1.15f)
        assertTrue("Reference motion must include X/Y camera reframing", hasXYReframingRef)

        // 2. Test Auto Action-Synchronized Motion Mode
        val dynamicProject = com.example.service.ActionMotionEngine.applyMotionToProject(
            project = baseProject,
            mode = com.example.model.MotionMode.AUTO_MOTION
        )
        val dynamicClips = dynamicProject.clips
        assertTrue(dynamicClips.all { it.intermediateKeyframes.isNotEmpty() })
        val maxScaleDynamic = dynamicClips.flatMap { it.allKeyframes() }.maxOf { it.scale }
        assertTrue("Dynamic action motion must exceed simple 1.05x zoom", maxScaleDynamic > 1.15f)

        // 3. Test Custom / Classic Preset Fallback Mode
        val customProject = com.example.service.ActionMotionEngine.applyMotionToProject(
            project = baseProject,
            mode = com.example.model.MotionMode.CUSTOM_MOTION
        )
        assertEquals(baseClips.size, customProject.clips.size)
    }

    @Test
    fun testMoveIntermediateKeyframeTimestamp() {
        val (startKf, endKf) = KeyframeEngine.createDefaultKeyframesForClip(
            clipIndex = 0,
            startMs = 0L,
            endMs = 4000L,
            preset = MotionPreset.CINEMATIC_PUSH_IN
        )
        var clip = com.example.model.ClipSegment(
            index = 1,
            startMs = 0L,
            endMs = 4000L,
            speechText = "Keyframe move test",
            startKeyframe = startKf,
            endKeyframe = endKf
        )

        clip = KeyframeEngine.addIntermediateKeyframe(
            clip = clip,
            timestampMs = 1500L,
            scale = 1.28f,
            positionX = 2.0f,
            positionY = -1.5f
        )
        val midId = clip.intermediateKeyframes.first().id

        // Move keyframe to 2600ms
        val updatedClip = KeyframeEngine.updateKeyframe(
            clip = clip,
            keyframeId = midId,
            timestampMs = 2600L
        )
        assertEquals(2600L, updatedClip.intermediateKeyframes.first().timestampMs)

        // Verify interpolation peaks at the new moved timestamp (2600ms)
        val atMovedPeak = MotionInterpolationEngine.interpolateAt(updatedClip, 2600L)
        assertEquals(1.28f, atMovedPeak.scale, 0.01f)
    }
}
