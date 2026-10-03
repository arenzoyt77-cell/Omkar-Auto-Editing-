package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.ClipSegment
import com.example.model.EditorMode
import com.example.model.ExportSettings
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.SplitPoint
import com.example.model.TextOverlay
import com.example.model.VideoProject
import com.example.service.ExportProgressUpdate
import com.example.service.InterpolatedTransform
import com.example.service.MotionInterpolationEngine
import com.example.service.PlaybackState
import com.example.service.SampleVideoHelper
import com.example.service.SpeechAnalysisService
import com.example.service.TimelineEngine
import com.example.service.VideoExportService
import com.example.service.VideoPreviewEngine
import com.example.service.VideoSegmentationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class EditorTab(val title: String) {
    EDIT("Edit"),
    KEYFRAMES("Keyframes"),
    AUDIO("Audio"),
    TEXT("Text"),
    CAPTIONS("Captions"),
    EXPORT("Export")
}

/**
 * UI State for the VideoProcessingViewModel and Timeline Manager.
 */
data class VideoProcessingUiState(
    val project: VideoProject? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStatus: String = "",
    val isExporting: Boolean = false,
    val exportProgressUpdate: ExportProgressUpdate = ExportProgressUpdate(0f, 0, 0, 0L, 0L, 0f, 0, ""),
    val isExportComplete: Boolean = false,
    val exportedFile: File? = null,
    val exportedUri: Uri? = null,
    val selectedClipId: String? = null,
    val selectedSplitId: String? = null,
    val editingTextOverlay: TextOverlay? = null,
    val showTextOverlayDialog: Boolean = false,
    val errorMessage: String? = null,
    val currentMode: EditorMode = EditorMode.SMART,
    val currentMotionMode: com.example.model.MotionMode = com.example.model.MotionMode.AUTO_MOTION,
    val motionBlueprintSummary: com.example.model.BlueprintDebugSummary = com.example.model.AuthoritativeReferenceBlueprint.debugSummary,
    val showExportDialog: Boolean = false,
    val isPlayerReady: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isMuted: Boolean = false,
    val currentTab: EditorTab = EditorTab.EDIT
)

/**
 * VideoProcessingViewModel orchestrates the interaction between the Jetpack Compose UI
 * and the underlying Media3 components (ExoPlayer, MediaItem, Timeline) for video loading,
 * playback control, dynamic keyframe interpolation, and non-destructive timeline editing.
 */
@OptIn(UnstableApi::class)
open class VideoProcessingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VideoProcessingVM"
        private const val SYNC_INTERVAL_MS = 16L // ~60 FPS keyframe evaluation
    }

    // Media3 Core Components
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    // Domain Services
    private val speechService = SpeechAnalysisService(application)
    private val segmentationService = VideoSegmentationService()
    val previewEngine = VideoPreviewEngine(application)
    private val exportService = VideoExportService(application)

    // Non-destructive Undo / Redo Stacks
    private val undoStack = mutableListOf<VideoProject>()
    private val redoStack = mutableListOf<VideoProject>()

    // State Flows
    private val _uiState = MutableStateFlow(VideoProcessingUiState())
    val uiState: StateFlow<VideoProcessingUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var syncJob: Job? = null

    init {
        setupPlayerListeners()
    }

    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startTimelineSync()
                } else {
                    stopTimelineSync()
                    updateCurrentTimelineState(exoPlayer.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _uiState.value = _uiState.value.copy(isPlayerReady = true)
                        updateCurrentTimelineState(exoPlayer.currentPosition)
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.value = _playbackState.value.copy(isPlaying = false)
                        stopTimelineSync()
                    }
                    Player.STATE_BUFFERING -> {}
                    Player.STATE_IDLE -> {
                        _uiState.value = _uiState.value.copy(isPlayerReady = false)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val currentProj = _uiState.value.project ?: return
                if (videoSize.width > 0 && videoSize.height > 0) {
                    _uiState.value = _uiState.value.copy(
                        project = currentProj.copy(
                            width = videoSize.width,
                            height = videoSize.height
                        )
                    )
                }
            }
        })
    }

    private fun saveStateForUndo() {
        val current = _uiState.value.project ?: return
        undoStack.add(current)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoStatus()
    }

    private fun updateUndoRedoStatus() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.project ?: return
        redoStack.add(current)
        val prev = undoStack.removeAt(undoStack.lastIndex)
        _uiState.value = _uiState.value.copy(project = prev)
        updateUndoRedoStatus()
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.project ?: return
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.value = _uiState.value.copy(project = next)
        updateUndoRedoStatus()
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    /**
     * Attaches a Surface to Media3 ExoPlayer for hardware-accelerated rendering.
     */
    fun setVideoSurface(surface: Surface?) {
        exoPlayer.setVideoSurface(surface)
    }

    /**
     * Loads a video into the Media3 pipeline, retrieves metadata, and triggers
     * speech-boundary thought analysis.
     */
    fun loadVideo(uri: Uri, fileName: String = "Selected Video") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.05f,
                    analysisStatus = "Loading video with Media3...",
                    errorMessage = null
                )

                undoStack.clear()
                redoStack.clear()
                updateUndoRedoStatus()

                // Retrieve video metadata
                val (durationMs, width, height, rotation, fps) = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(getApplication(), uri)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

                        val dur = durStr?.toLongOrNull() ?: 10000L
                        val w = wStr?.toIntOrNull() ?: 1920
                        val h = hStr?.toIntOrNull() ?: 1080
                        val rot = rotStr?.toIntOrNull() ?: 0
                        val rate = frameRateStr?.toFloatOrNull() ?: 30.0f
                        arrayOf(dur, w, h, rot, rate)
                    } finally {
                        retriever.release()
                    }
                }

                val initialProject = VideoProject(
                    videoUri = uri,
                    title = fileName,
                    durationMs = durationMs as Long,
                    width = width as Int,
                    height = height as Int,
                    rotation = rotation as Int,
                    frameRate = fps as Float,
                    mode = _uiState.value.currentMode
                )

                _uiState.value = _uiState.value.copy(
                    project = initialProject,
                    isAnalyzing = false
                )

                // Configure Media3 ExoPlayer
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()

                // Trigger semantic analysis
                analyzeCurrentVideo()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video into Media3: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Failed to load video: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Synthesizes and loads a test video with spoken thought captions for testing.
     */
    fun loadSampleSpeechVideo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.05f,
                    analysisStatus = "Synthesizing test speech video...",
                    errorMessage = null
                )

                val uri = SampleVideoHelper.generateSampleVideo(getApplication()) { progress ->
                    _uiState.value = _uiState.value.copy(
                        analysisProgress = 0.05f + progress * 0.25f,
                        analysisStatus = "Synthesizing test video ${(progress * 100).toInt()}%"
                    )
                }

                loadVideo(uri, "Omkar Sample Speech Video.mp4")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate sample video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Could not generate sample video: ${e.message}"
                )
            }
        }
    }

    /**
     * Runs speech-to-text analysis, detects completed spoken thoughts,
     * snaps split points to discrete video frames, and applies cinematic keyframes.
     */
    fun analyzeCurrentVideo() {
        val currentProj = _uiState.value.project ?: return
        val uri = currentProj.videoUri ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.1f,
                    analysisStatus = "Detecting spoken thoughts & utterance boundaries...",
                    errorMessage = null
                )

                val audioResult = speechService.analyzeVideo(
                    videoUri = uri,
                    durationMs = currentProj.durationMs
                ) { progress, status ->
                    _uiState.value = _uiState.value.copy(
                        analysisProgress = progress,
                        analysisStatus = status
                    )
                }

                _uiState.value = _uiState.value.copy(
                    analysisProgress = 0.92f,
                    analysisStatus = "Aligning frame boundaries & calculating keyframe curves..."
                )

                val (splits, clips) = segmentationService.generateSegmentation(
                    speechSegments = audioResult.speechSegments,
                    totalDurationMs = currentProj.durationMs,
                    frameRate = currentProj.frameRate,
                    audioAmplitudes = audioResult.amplitudes,
                    motionMode = _uiState.value.currentMotionMode
                )

                val updatedProject = currentProj.copy(
                    speechSegments = audioResult.speechSegments,
                    splitPoints = splits,
                    clips = clips,
                    audioAmplitudes = audioResult.amplitudes,
                    isAnalyzed = true
                )

                saveStateForUndo()
                _uiState.value = _uiState.value.copy(
                    project = updatedProject,
                    isAnalyzing = false,
                    selectedClipId = clips.firstOrNull()?.id,
                    analysisStatus = "Analysis Complete"
                )

                updateCurrentTimelineState(exoPlayer.currentPosition)

            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Analysis error: ${e.localizedMessage}"
                )
            }
        }
    }

    // Playback Controls
    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val maxDuration = _uiState.value.project?.effectiveDurationMs ?: exoPlayer.duration
        val clamped = positionMs.coerceIn(0L, maxDuration.coerceAtLeast(0L))
        exoPlayer.seekTo(clamped)
        updateCurrentTimelineState(clamped)
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        exoPlayer.volume = if (newMuted) 0f else 1f
        _uiState.value = _uiState.value.copy(isMuted = newMuted)
    }

    // Timeline Management
    private fun startTimelineSync() {
        stopTimelineSync()
        syncJob = viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition
                    updateCurrentTimelineState(currentPos)
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun stopTimelineSync() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun updateCurrentTimelineState(currentMs: Long) {
        val project = _uiState.value.project ?: return
        val activeClip = TimelineEngine.findActiveClip(project.clips, currentMs)

        val transform = if (activeClip != null) {
            MotionInterpolationEngine.interpolateAt(activeClip, currentMs)
        } else {
            InterpolatedTransform()
        }

        val activeSpeech = project.speechSegments.firstOrNull { currentMs in it.startMs..it.endMs }
        val subtitle = activeSpeech?.text ?: ""
        val activeWord = activeSpeech?.words?.firstOrNull { currentMs in it.startMs..it.endMs }?.word ?: ""

        _playbackState.value = PlaybackState(
            isPlaying = exoPlayer.isPlaying,
            currentPositionMs = currentMs,
            activeClip = activeClip,
            transform = transform,
            currentSubtitle = subtitle,
            activeWord = activeWord
        )
    }

    fun setMode(mode: EditorMode) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            project = _uiState.value.project?.copy(mode = mode)
        )
    }

    fun setEditorTab(tab: EditorTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectClip(clipId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedClipId = clipId,
            selectedSplitId = null
        )
        val clip = _uiState.value.project?.clips?.firstOrNull { it.id == clipId }
        if (clip != null) {
            seekTo(clip.startMs)
        }
    }

    fun selectSplit(splitId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedSplitId = splitId,
            selectedClipId = null
        )
        val split = _uiState.value.project?.splitPoints?.firstOrNull { it.id == splitId }
        if (split != null) {
            seekTo(split.timestampMs)
        }
    }

    fun addSplitAtPlayhead() {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val currentPlayhead = exoPlayer.currentPosition
        val updated = TimelineEngine.addSplitPoint(project, currentPlayhead)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(currentPlayhead)
    }

    fun moveSplit(splitId: String, newTimestampMs: Long) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.moveSplitPoint(project, splitId, newTimestampMs)
        _uiState.value = _uiState.value.copy(project = updated)
    }

    fun deleteSplit(splitId: String) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.deleteSplitPoint(project, splitId)
        _uiState.value = _uiState.value.copy(
            project = updated,
            selectedSplitId = null
        )
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    fun trimClipStart(clipId: String, newStartMs: Long) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.trimClipStart(project, clipId, newStartMs)
        _uiState.value = _uiState.value.copy(project = updated)
        seekTo(newStartMs)
    }

    fun trimClipEnd(clipId: String, newEndMs: Long) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.trimClipEnd(project, clipId, newEndMs)
        _uiState.value = _uiState.value.copy(project = updated)
        seekTo(newEndMs)
    }

    fun deleteClip(clipId: String) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.deleteClip(project, clipId)
        _uiState.value = _uiState.value.copy(
            project = updated,
            selectedClipId = updated.clips.firstOrNull()?.id
        )
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    fun duplicateClip(clipId: String) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.duplicateClip(project, clipId)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    fun toggleClipMute(clipId: String) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        val updated = TimelineEngine.setClipMuted(project, clipId, !clip.isMuted)
        _uiState.value = _uiState.value.copy(project = updated)
    }

    fun setClipVolume(clipId: String, volume: Float) {
        val project = _uiState.value.project ?: return
        val updated = TimelineEngine.setClipVolume(project, clipId, volume)
        _uiState.value = _uiState.value.copy(project = updated)
    }

    fun updateKeyframe(
        clipId: String,
        keyframeId: String,
        scale: Float? = null,
        positionX: Float? = null,
        positionY: Float? = null,
        rotation: Float? = null,
        easing: MotionCurve? = null
    ) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.updateClipKeyframe(
            project = project,
            clipId = clipId,
            keyframeId = keyframeId,
            scale = scale,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            easing = easing
        )
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    fun setClipPreset(clipId: String, preset: MotionPreset) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.setClipPreset(project, clipId, preset)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    /**
     * Changes the overall motion mode (REFERENCE MOTION, AUTO MOTION, CUSTOM MOTION)
     * and re-applies motion curves across clips.
     */
    fun setMotionMode(mode: com.example.model.MotionMode) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(currentMotionMode = mode)
        val proj = _uiState.value.project ?: return
        val updated = TimelineEngine.regenerateMotion(proj, mode)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    /**
     * Regenerates motion curves across clips while preserving the original video and cuts.
     */
    fun regenerateMotion() {
        val proj = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.regenerateMotion(proj, _uiState.value.currentMotionMode)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    /**
     * Adds an intermediate keyframe at current playhead position in the selected clip.
     */
    fun addKeyframeAtPlayhead(
        clipId: String,
        scale: Float = 1.15f,
        posX: Float = 0f,
        posY: Float = 0f,
        curve: MotionCurve = MotionCurve.DYNAMIC_PUNCH
    ) {
        val proj = _uiState.value.project ?: return
        saveStateForUndo()
        val currentPlayhead = exoPlayer.currentPosition
        val updated = TimelineEngine.addIntermediateKeyframe(
            project = proj,
            clipId = clipId,
            timestampMs = currentPlayhead,
            scale = scale,
            positionX = posX,
            positionY = posY,
            rotation = 0f,
            easing = curve
        )
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(currentPlayhead)
    }

    /**
     * Deletes a keyframe from the specified clip.
     */
    fun deleteKeyframe(clipId: String, keyframeId: String) {
        val proj = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.deleteKeyframe(proj, clipId, keyframeId)
        _uiState.value = _uiState.value.copy(project = updated)
        updateCurrentTimelineState(exoPlayer.currentPosition)
    }

    // Text Overlay Management
    fun openTextOverlayDialog(overlay: TextOverlay? = null) {
        _uiState.value = _uiState.value.copy(
            showTextOverlayDialog = true,
            editingTextOverlay = overlay
        )
    }

    fun dismissTextOverlayDialog() {
        _uiState.value = _uiState.value.copy(
            showTextOverlayDialog = false,
            editingTextOverlay = null
        )
    }

    fun saveTextOverlay(overlay: TextOverlay) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val exists = project.textOverlays.any { it.id == overlay.id }
        val updated = if (exists) {
            TimelineEngine.updateTextOverlay(project, overlay)
        } else {
            TimelineEngine.addTextOverlay(project, overlay)
        }
        _uiState.value = _uiState.value.copy(
            project = updated,
            showTextOverlayDialog = false,
            editingTextOverlay = null
        )
    }

    fun deleteTextOverlay(overlayId: String) {
        val project = _uiState.value.project ?: return
        saveStateForUndo()
        val updated = TimelineEngine.deleteTextOverlay(project, overlayId)
        _uiState.value = _uiState.value.copy(
            project = updated,
            showTextOverlayDialog = false,
            editingTextOverlay = null
        )
    }

    // Export Management
    fun updateExportSettings(settings: ExportSettings) {
        val project = _uiState.value.project ?: return
        _uiState.value = _uiState.value.copy(
            project = project.copy(exportSettings = settings)
        )
    }

    fun openExportDialog() {
        pause()
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun startExport() {
        val project = _uiState.value.project ?: return
        dismissExportDialog()
        pause()

        val totalFrames = ((project.effectiveDurationMs * project.exportSettings.fps) / 1000L).toInt().coerceAtLeast(1)

        _uiState.value = _uiState.value.copy(
            isExporting = true,
            isExportComplete = false,
            errorMessage = null,
            exportProgressUpdate = ExportProgressUpdate(
                progress = 0.01f,
                currentFrame = 0,
                totalFrames = totalFrames,
                currentDurationMs = 0L,
                totalDurationMs = project.effectiveDurationMs,
                fpsSpeed = 0f,
                etaSeconds = 0,
                stage = "Initializing hardware acceleration pipeline..."
            )
        )

        viewModelScope.launch {
            exportService.exportProject(
                project = project,
                onProgressUpdate = { update ->
                    _uiState.value = _uiState.value.copy(
                        exportProgressUpdate = update,
                        isExporting = update.progress < 1.0f,
                        isExportComplete = update.progress >= 1.0f
                    )
                },
                onError = { err ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        isExportComplete = false,
                        errorMessage = err
                    )
                },
                onSuccess = { file, uri ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        isExportComplete = true,
                        exportedFile = file,
                        exportedUri = uri
                    )
                }
            )
        }
    }

    fun cancelExport() {
        exportService.cancelExport()
        _uiState.value = _uiState.value.copy(
            isExporting = false,
            isExportComplete = false
        )
    }

    fun dismissExportComplete() {
        _uiState.value = _uiState.value.copy(
            isExportComplete = false,
            isExporting = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimelineSync()
        exoPlayer.release()
        previewEngine.releasePlayer()
    }
}
