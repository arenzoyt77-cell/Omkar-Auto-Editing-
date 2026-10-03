package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object SampleVideoHelper {

    private const val TAG = "SampleVideoHelper"

    /**
     * Generates a standards-compliant, playable 10-second MP4 video on device with
     * H.264 video, AAC audio, spoken thought captions, and animated waveform.
     */
    suspend fun generateSampleVideo(
        context: Context,
        onProgress: (Float) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "omkar_sample_speech_video.mp4")
        if (outputFile.exists() && outputFile.length() > 50_000) {
            // Validate cached file before returning
            val isValid = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                retriever.release()
                hasVideo == "yes"
            } catch (_: Exception) { false }

            if (isValid) {
                onProgress(1.0f)
                return@withContext Uri.fromFile(outputFile)
            } else {
                outputFile.delete()
            }
        }

        val width = 1280
        val height = 720
        val bitRate = 2_500_000
        val frameRate = 30
        val durationSeconds = 10
        val totalFrames = durationSeconds * frameRate

        val sampleRate = 44100
        val channelCount = 2
        val audioBitRate = 128_000

        var muxer: MediaMuxer? = null
        var videoCodec: MediaCodec? = null
        var audioCodec: MediaCodec? = null

        try {
            // 1. Configure H.264 Video Encoder
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                try {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31)
                } catch (_: Exception) {}
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            // 2. Configure AAC Audio Encoder
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioCodec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            val bgPaint = Paint().apply { color = Color.parseColor("#0C0F17") }
            val cardPaint = Paint().apply { color = Color.parseColor("#161B26") }
            val wavePaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#FFB800")
                textSize = 46f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                isAntiAlias = true
            }
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 24f
                isAntiAlias = true
            }

            // Audio synthesis parameters: 440 Hz soft tone
            val pcmSamplesPerFrame = sampleRate / frameRate
            val pcmBytesPerFrame = pcmSamplesPerFrame * channelCount * 2
            val pcmBuffer = ByteBuffer.allocateDirect(pcmBytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)

            var audioPtsUs = 0L

            for (frame in 0 until totalFrames) {
                val presentationTimeUs = (frame * 1_000_000L) / frameRate

                // --- Video Render ---
                val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // Render card
                val cardRect = RectF(60f, 40f, width - 60f, height - 40f)
                canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

                // Title & Subtitle
                canvas.drawText("OMKAR AUTOMATIC VIDEO MAKER", 120f, 130f, titlePaint)
                canvas.drawText("Speech-Boundary Thought Detection & Keyframe Test Video", 120f, 180f, subTextPaint)

                // Thought transcript simulation
                val elapsedSec = frame / 30f
                val thoughtText = when {
                    elapsedSec < 3.3f -> "Thought 1: 'Today I am going to show you three amazing tricks...'"
                    elapsedSec < 6.8f -> "Thought 2: 'The first trick is automatic speech boundary detection!'"
                    else -> "Thought 3: 'Dynamic cinematic keyframes push in without cutting words.'"
                }
                canvas.drawText(thoughtText, 120f, 280f, textPaint)

                // Animated waveform
                val waveCenterY = 460f
                val barCount = 48
                val barWidth = 14f
                val spacing = 8f
                val startX = 120f

                for (b in 0 until barCount) {
                    val phase = (frame * 0.15f) + (b * 0.35f)
                    val barHeight = 20f + 85f * kotlin.math.abs(sin(phase))
                    val bx = startX + b * (barWidth + spacing)
                    canvas.drawLine(bx, waveCenterY - barHeight / 2, bx, waveCenterY + barHeight / 2, wavePaint)
                }

                val mins = (frame / frameRate) / 60
                val secs = (frame / frameRate) % 60
                val ms = ((frame % frameRate) * 1000) / frameRate
                val timecode = String.format("%02d:%02d.%03d | Frame %d/%d", mins, secs, ms, frame, totalFrames)
                canvas.drawText(timecode, 120f, 590f, subTextPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // --- Audio PCM Generation & Encoding ---
                pcmBuffer.clear()
                val freq = if (elapsedSec < 3.3f) 440.0 else if (elapsedSec < 6.8f) 523.25 else 659.25
                for (s in 0 until pcmSamplesPerFrame) {
                    val sampleTime = (audioPtsUs + (s * 1_000_000L / sampleRate)) / 1_000_000.0
                    val sampleVal = (sin(2.0 * PI * freq * sampleTime) * 6000.0).toInt().toShort()
                    pcmBuffer.putShort(sampleVal) // Left
                    pcmBuffer.putShort(sampleVal) // Right
                }
                pcmBuffer.flip()

                val audioInIdx = audioCodec.dequeueInputBuffer(10_000)
                if (audioInIdx >= 0) {
                    val inBuf = audioCodec.getInputBuffer(audioInIdx)
                    if (inBuf != null) {
                        inBuf.clear()
                        inBuf.put(pcmBuffer)
                        audioCodec.queueInputBuffer(audioInIdx, 0, pcmBytesPerFrame, audioPtsUs, 0)
                        audioPtsUs += (pcmSamplesPerFrame * 1_000_000L) / sampleRate
                    }
                }

                // --- Drain Video & Audio Encoders ---
                // Format Changed checks
                if (videoTrackIndex == -1) {
                    val outIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                    } else if (outIndex >= 0) {
                        videoCodec.releaseOutputBuffer(outIndex, false)
                    }
                }

                if (audioTrackIndex == -1) {
                    val outIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        audioTrackIndex = muxer.addTrack(audioCodec.outputFormat)
                    } else if (outIndex >= 0) {
                        audioCodec.releaseOutputBuffer(outIndex, false)
                    }
                }

                if (!muxerStarted && videoTrackIndex != -1 && audioTrackIndex != -1) {
                    muxer.start()
                    muxerStarted = true
                }

                // Drain Video Encoder
                while (muxerStarted) {
                    val outIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, 0)
                    if (outIndex >= 0) {
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            videoBufferInfo.size = 0
                        }
                        if (videoBufferInfo.size > 0) {
                            val data = videoCodec.getOutputBuffer(outIndex)
                            if (data != null) {
                                data.position(videoBufferInfo.offset)
                                data.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                muxer.writeSampleData(videoTrackIndex, data, videoBufferInfo)
                            }
                        }
                        videoCodec.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }

                // Drain Audio Encoder (Interleaved)
                while (muxerStarted) {
                    val outIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, 0)
                    if (outIndex >= 0) {
                        if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            audioBufferInfo.size = 0
                        }
                        if (audioBufferInfo.size > 0) {
                            val data = audioCodec.getOutputBuffer(outIndex)
                            if (data != null) {
                                data.position(audioBufferInfo.offset)
                                data.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                muxer.writeSampleData(audioTrackIndex, data, audioBufferInfo)
                            }
                        }
                        audioCodec.releaseOutputBuffer(outIndex, false)
                    } else {
                        break
                    }
                }

                if (frame % 15 == 0) {
                    onProgress(frame.toFloat() / totalFrames.toFloat())
                }
            }

            // Signal Video EOS
            try { videoCodec.signalEndOfInputStream() } catch (_: Exception) {}

            // Signal Audio EOS
            val audioEosIdx = audioCodec.dequeueInputBuffer(10_000)
            if (audioEosIdx >= 0) {
                audioCodec.queueInputBuffer(audioEosIdx, 0, 0, audioPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Drain remaining Video
            var videoEos = false
            while (!videoEos && muxerStarted) {
                val outIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, 10_000)
                if (outIndex >= 0) {
                    if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        videoEos = true
                    }
                    if (videoBufferInfo.size > 0) {
                        val data = videoCodec.getOutputBuffer(outIndex)
                        if (data != null) {
                            data.position(videoBufferInfo.offset)
                            data.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, data, videoBufferInfo)
                        }
                    }
                    videoCodec.releaseOutputBuffer(outIndex, false)
                } else {
                    break
                }
            }

            // Drain remaining Audio
            var audioEos = false
            while (!audioEos && muxerStarted) {
                val outIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, 10_000)
                if (outIndex >= 0) {
                    if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        audioEos = true
                    }
                    if (audioBufferInfo.size > 0) {
                        val data = audioCodec.getOutputBuffer(outIndex)
                        if (data != null) {
                            data.position(audioBufferInfo.offset)
                            data.limit(audioBufferInfo.offset + audioBufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, data, audioBufferInfo)
                        }
                    }
                    audioCodec.releaseOutputBuffer(outIndex, false)
                } else {
                    break
                }
            }

            onProgress(1.0f)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample video: ${e.message}", e)
            throw e
        } finally {
            try { videoCodec?.stop(); videoCodec?.release() } catch (_: Exception) {}
            try { audioCodec?.stop(); audioCodec?.release() } catch (_: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }

        Uri.fromFile(outputFile)
    }
}
