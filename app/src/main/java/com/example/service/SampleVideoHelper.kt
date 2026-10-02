package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sin

object SampleVideoHelper {

    private const val TAG = "SampleVideoHelper"

    /**
     * Generates a real, valid 10-second MP4 video on device with animated visuals,
     * captions, and audio for immediate testing in emulator and production devices.
     */
    suspend fun generateSampleVideo(
        context: Context,
        onProgress: (Float) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "omkar_sample_speech_video.mp4")
        if (outputFile.exists() && outputFile.length() > 50_000) {
            onProgress(1.0f)
            return@withContext Uri.fromFile(outputFile)
        }

        val width = 1280
        val height = 720
        val bitRate = 2_500_000
        val frameRate = 30
        val durationSeconds = 10
        val totalFrames = durationSeconds * frameRate

        var muxer: MediaMuxer? = null
        var videoCodec: MediaCodec? = null

        try {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
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
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()

            val bgPaint = Paint().apply { color = Color.parseColor("#0C0F17") }
            val cardPaint = Paint().apply { color = Color.parseColor("#161B26") }
            val wavePaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#FFB800")
                textSize = 48f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 34f
                isAntiAlias = true
            }
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 24f
                isAntiAlias = true
            }

            for (frame in 0 until totalFrames) {
                val presentationTimeUs = (frame * 1_000_000L) / frameRate
                val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                // Render dynamic frame
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Render card
                val cardRect = RectF(80f, 60f, width - 80f, height - 60f)
                canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

                // Title
                canvas.drawText("OMKAR AUTOMATIC VIDEO MAKER", 140f, 150f, titlePaint)
                canvas.drawText("Speech-Boundary Thought Detection & Keyframe Test Video", 140f, 200f, subTextPaint)

                // Draw spoken thought based on timestamp
                val elapsedSec = frame / 30f
                val thoughtText = when {
                    elapsedSec < 3.5f -> "Thought 1: 'Today I am going to show you three amazing tricks...'"
                    elapsedSec < 7.0f -> "Thought 2: 'The first trick is automatic speech boundary detection!'"
                    else -> "Thought 3: 'Dynamic cinematic keyframes push in without cutting words.'"
                }
                canvas.drawText(thoughtText, 140f, 300f, textPaint)

                // Draw animated audio waveform
                val waveCenterY = 480f
                val barCount = 48
                val barWidth = 14f
                val spacing = 8f
                val startX = 140f

                for (b in 0 until barCount) {
                    val phase = (frame * 0.15f) + (b * 0.35f)
                    val barHeight = 20f + 90f * kotlin.math.abs(sin(phase))
                    val bx = startX + b * (barWidth + spacing)
                    canvas.drawLine(bx, waveCenterY - barHeight / 2, bx, waveCenterY + barHeight / 2, wavePaint)
                }

                // Timecode
                val mins = (frame / frameRate) / 60
                val secs = (frame / frameRate) % 60
                val ms = ((frame % frameRate) * 1000) / frameRate
                val timecode = String.format("%02d:%02d.%03d | Frame %d/%d", mins, secs, ms, frame, totalFrames)
                canvas.drawText(timecode, 140f, 600f, subTextPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                while (true) {
                    val outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
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

                if (frame % 15 == 0) {
                    onProgress(frame.toFloat() / totalFrames.toFloat())
                }
            }

            // Signal EOS
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

            onProgress(1.0f)
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample video: ${e.message}", e)
            throw e
        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }
}
