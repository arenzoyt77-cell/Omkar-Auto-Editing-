package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.ExportResolution
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoExportService(private val context: Context) {

    companion object {
        private const val TAG = "VideoExportService"
    }

    suspend fun exportProject(
        project: VideoProject,
        onProgress: (Float, String) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (File, Uri) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress(0.01f, "Initializing export pipeline...")

        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.cacheDir, "OmkarVideos").apply {
            mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(exportDir, "OMKAR_AutomaticVideo_${timestamp}.mp4")

        // Target Dimensions
        val targetResolution = project.exportSettings.resolution
        val targetWidth = when (targetResolution) {
            ExportResolution.ORIGINAL -> if (project.width > 0) project.width else 1280
            ExportResolution.FHD_1080P -> 1920
            ExportResolution.HD_720P -> 1280
            ExportResolution.PORTRAIT_1080P -> 1080
        }
        val targetHeight = when (targetResolution) {
            ExportResolution.ORIGINAL -> if (project.height > 0) project.height else 720
            ExportResolution.FHD_1080P -> 1080
            ExportResolution.HD_720P -> 720
            ExportResolution.PORTRAIT_1080P -> 1920
        }

        // Align width and height to multiples of 16 (codec requirement)
        val alignedWidth = (targetWidth / 16) * 16
        val alignedHeight = (targetHeight / 16) * 16

        val frameRate = project.exportSettings.fps.coerceIn(24, 60)
        val bitRate = project.exportSettings.bitrateMbps * 1_000_000
        val totalDurationMs = project.durationMs.coerceAtLeast(1000L)
        val frameIntervalMs = 1000L / frameRate
        val totalFrames = ((totalDurationMs / frameIntervalMs)).toInt().coerceAtLeast(1)

        var videoCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        val retriever = MediaMetadataRetriever()
        val audioExtractor = MediaExtractor()

        try {
            retriever.setDataSource(context, project.videoUri)

            // Setup H.264 Encoder
            val videoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                alignedWidth,
                alignedHeight
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            // Inspect audio track in source video
            var sourceAudioTrack = -1
            var audioFormat: MediaFormat? = null
            try {
                audioExtractor.setDataSource(context, project.videoUri, null)
                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        sourceAudioTrack = i
                        audioFormat = format
                        audioExtractor.selectTrack(i)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio track extraction warning: ${e.message}")
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val bgPaint = Paint().apply { color = Color.BLACK }
            val subtitleBgPaint = Paint().apply {
                color = Color.parseColor("#CC0C0F17")
                style = Paint.Style.FILL
            }
            val subtitleTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = (alignedHeight * 0.045f).coerceIn(24f, 48f)
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val matrix = Matrix()

            onProgress(0.05f, "Encoding frames with keyframe motion interpolation...")

            for (frame in 0 until totalFrames) {
                val currentMs = frame * frameIntervalMs
                val presentationTimeUs = (frame * 1_000_000L) / frameRate

                // Determine active clip and keyframe transformation
                val activeClip = TimelineEngine.findActiveClip(project.clips, currentMs)
                val transform = if (activeClip != null) {
                    MotionInterpolationEngine.interpolateAt(activeClip, currentMs)
                } else {
                    InterpolatedTransform()
                }

                // Retrieve video frame bitmap
                var frameBitmap: Bitmap? = null
                try {
                    frameBitmap = retriever.getFrameAtTime(
                        currentMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not extract frame at ${currentMs}ms")
                }

                val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                canvas.drawRect(0f, 0f, alignedWidth.toFloat(), alignedHeight.toFloat(), bgPaint)

                if (frameBitmap != null && !frameBitmap.isRecycled) {
                    matrix.reset()

                    val srcW = frameBitmap.width.toFloat()
                    val srcH = frameBitmap.height.toFloat()

                    // Fit aspect ratio
                    val scaleFit = kotlin.math.max(alignedWidth / srcW, alignedHeight / srcH)
                    val combinedScale = scaleFit * transform.scale

                    val transX = (alignedWidth - srcW * combinedScale) / 2f + (transform.positionX / 100f) * alignedWidth
                    val transY = (alignedHeight - srcH * combinedScale) / 2f + (transform.positionY / 100f) * alignedHeight

                    matrix.postScale(combinedScale, combinedScale)
                    matrix.postTranslate(transX, transY)
                    if (transform.rotation != 0f) {
                        matrix.postRotate(transform.rotation, alignedWidth / 2f, alignedHeight / 2f)
                    }

                    canvas.drawBitmap(frameBitmap, matrix, null)
                    frameBitmap.recycle()
                }

                // Subtitle Overlay
                if (project.exportSettings.includeSubtitles && activeClip != null && activeClip.speechText.isNotBlank()) {
                    val subText = activeClip.speechText
                    val subY = alignedHeight - (alignedHeight * 0.09f)
                    val textBounds = Rect()
                    subtitleTextPaint.getTextBounds(subText, 0, subText.length, textBounds)
                    val padding = 24f
                    val boxRect = RectF(
                        (alignedWidth / 2f) - (textBounds.width() / 2f) - padding,
                        subY - textBounds.height() - padding,
                        (alignedWidth / 2f) + (textBounds.width() / 2f) + padding,
                        subY + padding
                    )
                    canvas.drawRoundRect(boxRect, 16f, 16f, subtitleBgPaint)
                    canvas.drawText(subText, alignedWidth / 2f, subY, subtitleTextPaint)
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain video encoder
                while (true) {
                    val outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                        if (sourceAudioTrack != -1 && audioFormat != null && audioTrackIndex == -1) {
                            try {
                                audioTrackIndex = muxer.addTrack(audioFormat)
                            } catch (e: Exception) {
                                Log.w(TAG, "Audio track addition error: ${e.message}")
                            }
                        }
                        muxer.start()
                        muxerStarted = true
                    } else if (outIndex >= 0) {
                        val encodedData = videoCodec.getOutputBuffer(outIndex)
                        if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = presentationTimeUs
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        videoCodec.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }

                if (frame % 5 == 0) {
                    val progress = (frame.toFloat() / totalFrames.toFloat()).coerceIn(0.05f, 0.88f)
                    val pct = (progress * 100).toInt()
                    onProgress(progress, "Rendering Frame $frame/$totalFrames ($pct%)...")
                }
            }

            // Signal End of Video Stream
            videoCodec.signalEndOfInputStream()
            var eos = false
            while (!eos) {
                val outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        eos = true
                    }
                    if (muxerStarted && bufferInfo.size > 0) {
                        val encodedData = videoCodec.getOutputBuffer(outIndex)
                        if (encodedData != null) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    videoCodec.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            // Mux Audio Track if available
            if (sourceAudioTrack != -1 && audioTrackIndex != -1 && muxerStarted) {
                onProgress(0.90f, "Muxing synchronized AAC audio track...")
                val audioBuffer = ByteBuffer.allocateDirect(128 * 1024)
                val audioBufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break

                    val sampleTimeUs = audioExtractor.sampleTime
                    if (sampleTimeUs > totalDurationMs * 1000L) break

                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = sampleSize
                    audioBufferInfo.presentationTimeUs = sampleTimeUs
                    audioBufferInfo.flags = audioExtractor.sampleFlags

                    muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                    audioExtractor.advance()
                }
            }

            onProgress(0.96f, "Finalizing MP4 container & saving to Gallery...")

            try {
                muxer.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Muxer stop notice: ${e.message}")
            }
            muxer.release()
            muxer = null

            // Publish to MediaStore so it is instantly available in device Gallery
            val finalUri = saveToMediaStore(outputFile, "OMKAR_${timestamp}.mp4")

            onProgress(1.0f, "Export Complete (100%)!")
            onSuccess(outputFile, finalUri ?: Uri.fromFile(outputFile))

        } catch (e: Exception) {
            Log.e(TAG, "Export failure: ${e.message}", e)
            onError("Export failed: ${e.localizedMessage ?: "Unknown hardware codec error"}")
        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.release()
            } catch (ignored: Exception) {}
            try {
                retriever.release()
            } catch (ignored: Exception) {}
            try {
                audioExtractor.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun saveToMediaStore(file: File, displayName: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OmkarVideos")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            Log.w(TAG, "Could not save to MediaStore: ${e.message}")
            Uri.fromFile(file)
        }
    }
}
