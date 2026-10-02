package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import com.example.model.ClipSegment
import com.example.model.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val activeClip: ClipSegment? = null,
    val transform: InterpolatedTransform = InterpolatedTransform(),
    val currentSubtitle: String = "",
    val activeWord: String = ""
)

class VideoPreviewEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoPreviewEngine"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var currentProject: VideoProject? = null
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun attachSurface(surface: Surface) {
        this.surface = surface
        mediaPlayer?.setSurface(surface)
    }

    fun detachSurface() {
        mediaPlayer?.setSurface(null)
        surface = null
    }

    fun prepareProject(project: VideoProject, onPrepared: () -> Unit = {}) {
        currentProject = project
        releasePlayer()

        try {
            val player = MediaPlayer().apply {
                setDataSource(context, project.videoUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MediaPlayer prepared, duration=${mp.duration}ms")
                    if (surface != null && surface!!.isValid) {
                        mp.setSurface(surface)
                    }
                    onPrepared()
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    stopSyncLoop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare MediaPlayer: ${e.message}", e)
        }
    }

    fun play() {
        mediaPlayer?.let { player ->
            try {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startSyncLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting playback: ${e.message}")
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopSyncLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing playback: ${e.message}")
            }
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                val clamped = positionMs.coerceIn(0L, (player.duration.toLong()).coerceAtLeast(100L))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    player.seekTo(clamped, MediaPlayer.SEEK_CLOSEST)
                } else {
                    player.seekTo(clamped.toInt())
                }
                updatePlaybackState(clamped)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking: ${e.message}")
            }
        }
    }

    private fun startSyncLoop() {
        stopSyncLoop()
        syncJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentMs = player.currentPosition.toLong()
                        updatePlaybackState(currentMs)
                    }
                }
                delay(16) // ~60fps sync rate for silky smooth keyframe motion
            }
        }
    }

    private fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun updatePlaybackState(currentMs: Long) {
        val project = currentProject ?: return
        val activeClip = TimelineEngine.findActiveClip(project.clips, currentMs)

        val transform = if (activeClip != null) {
            MotionInterpolationEngine.interpolateAt(activeClip, currentMs)
        } else {
            InterpolatedTransform()
        }

        // Find active speech segment and active word
        val activeSpeech = project.speechSegments.firstOrNull { currentMs in it.startMs..it.endMs }
        val subtitle = activeSpeech?.text ?: ""
        val activeWord = activeSpeech?.words?.firstOrNull { currentMs in it.startMs..it.endMs }?.word ?: ""

        _playbackState.value = PlaybackState(
            isPlaying = mediaPlayer?.isPlaying == true,
            currentPositionMs = currentMs,
            activeClip = activeClip,
            transform = transform,
            currentSubtitle = subtitle,
            activeWord = activeWord
        )
    }

    fun releasePlayer() {
        stopSyncLoop()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }
}
