package com.example

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.VideoProject
import com.example.service.MotionInterpolationEngine
import com.example.service.TimelineEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaPipelineTest {

    @Test
    fun testRetrieverInit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val retriever = MediaMetadataRetriever()
        assertNotNull(retriever)
    }

    @Test
    fun testFileProviderUriGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "test_output.mp4").apply {
            writeBytes(ByteArray(1024))
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", testFile)
        assertNotNull(uri)
        assertTrue(uri.toString().startsWith("content://"))
    }

    @Test
    fun testTimelineClipInterpolation() {
        val clip = ClipSegment(
            index = 1,
            startMs = 0L,
            endMs = 3000L,
            speechText = "Test Speech",
            startKeyframe = Keyframe(timestampMs = 0L, scale = 1.0f, easing = MotionCurve.CINEMATIC_SLOW),
            endKeyframe = Keyframe(timestampMs = 3000L, scale = 1.15f, easing = MotionCurve.CINEMATIC_SLOW)
        )

        val transformStart = MotionInterpolationEngine.interpolateAt(clip, 0L)
        assertEquals(1.0f, transformStart.scale, 0.01f)

        val transformMid = MotionInterpolationEngine.interpolateAt(clip, 1500L)
        assertTrue(transformMid.scale > 1.0f && transformMid.scale < 1.15f)

        val transformEnd = MotionInterpolationEngine.interpolateAt(clip, 3000L)
        assertEquals(1.15f, transformEnd.scale, 0.01f)
    }

    @Test
    fun testTrimAndSplitOperations() {
        val initialClip = ClipSegment(
            index = 1,
            startMs = 0L,
            endMs = 5000L,
            speechText = "Thought",
            startKeyframe = Keyframe(timestampMs = 0L, scale = 1.0f),
            endKeyframe = Keyframe(timestampMs = 5000L, scale = 1.10f)
        )
        val project = VideoProject(
            videoUri = Uri.parse("file:///dummy.mp4"),
            durationMs = 5000L,
            clips = listOf(initialClip)
        )

        val trimmed = TimelineEngine.trimClipStart(project, initialClip.id, 1000L)
        assertEquals(1000L, trimmed.clips[0].startMs)

        val trimmedEnd = TimelineEngine.trimClipEnd(trimmed, initialClip.id, 4000L)
        assertEquals(4000L, trimmedEnd.clips[0].endMs)
    }
}
