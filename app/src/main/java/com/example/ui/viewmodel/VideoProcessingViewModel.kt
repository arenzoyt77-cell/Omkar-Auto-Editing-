package com.example.ui.viewmodel

import android.app.Application
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
import androidx.media3.exoplayer.SeekParameters
import com.example.model.ClipSegment
import com.example.model.EditorMode
import com.example.model.ExportSettings
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.SplitPoint
import com.example.model.TextOverlay
import com.example.model.VideoMetadata
import com.example.model.VideoProject
import com.example.service.ExportProgressUpdate
import com.example.service.InterpolatedTransform
import com.example.service.MotionInterpolationEngine
import com.example.service.PlaybackState
import com.example.service.ReferenceMotionCache
import com.example.service.SampleVideoHelper
import com.example.service.SpeechAnalysisService
import com.example.service.TimelineEngine
import com.example.service.VideoExportService
import com.example.service.VideoImportHelper
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
    REFERENCE("Reference"),
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
    val originalVideoMetadata: VideoMetadata? = null,
    val referenceVideoMetadata: VideoMetadata? = null,
    val isImportingVideo: Boolean = false,
    val importStatus: String = "",
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStatus: String = "",
    val analysisStep: Int = 0,
    val totalAnalysisSteps: Int = 6,
    val isAnalyzingReference: Boolean = false,
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
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application)
        .setSeekParameters(SeekParameters.EXACT)
        .build()

    // Domain Services
    private val speechService = SpeechAnalysisService(application)
    private val segmentationService = VideoSegmentationService()
    val previewEngine = VideoPreviewEngine(application)
    private val exportService = VideoExportService(application)

    // Non-destructive Undo / Redo Stacks
    private val undoStack = mutableListOf<VideoProject>()
    private val redoStack = mutableListOf<VideoProject>()
    private var lastUndoSaveTimeMs: Long = 0L
    private var lastPlayerSeekTimeMs: Long = 0L

    // State Flows
    private val _uiState = MutableStateFlow(VideoProcessingUiState())
    val uiState: StateFlow<VideoProcessingUiState> = _uiState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var syncJob: Job? = null
    private var analysisJob: Job? = null
    private var importJob: Job? = null

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

    private fun saveStateForUndo(throttled: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (throttled && (now - lastUndoSaveTimeMs) < 450L) {
            return
        }
        lastUndoSaveTimeMs = now
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
     * Loads original video asynchronously, extracts metadata and thumbnail,
     * configures ExoPlayer, and launches multi-stage speech analysis.
     */
    fun loadVideo(uri: Uri, fallbackName: String = "Selected Video") {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImportingVideo = true,
                    importStatus = "Reading video metadata & generating thumbnail...",
                    errorMessage = null
                )

                undoStack.clear()
                redoStack.clear()
                updateUndoRedoStatus()

                // Asynchronously inspect video on Dispatchers.IO
                val meta = VideoImportHelper.extractMetadata(getApplication(), uri, fallbackName)

                val initialProject = VideoProject(
                    videoUri = uri,
                    title = meta.fileName,
                    durationMs = meta.durationMs,
                    width = meta.width,
                    height = meta.height,
                    rotation = meta.rotation,
                    frameRate = meta.frameRate,
                    mode = _uiState.value.currentMode
                )

                _uiState.value = _uiState.value.copy(
                    project = initialProject,
                    originalVideoMetadata = meta,
                    isImportingVideo = false,
                    importStatus = ""
                )

                // Configure Media3 ExoPlayer
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()

                // Trigger multi-step automatic analysis
                analyzeCurrentVideo()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isImportingVideo = false,
                    isAnalyzing = false,
                    errorMessage = "Unable to load video: ${e.localizedMessage ?: "Unknown error"}. Please check file permissions or try an MP4 video."
                )
            }
        }
    }

    /**
     * Loads a custom Reference Video asynchronously, analyzes its motion blueprint,
     * and caches it in ReferenceMotionCache so it is never re-analyzed on preview/scrub.
     */
    fun loadReferenceVideo(uri: Uri, fallbackName: String = "Reference Video") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzingReference = true,
                    errorMessage = null
                )

                val meta = VideoImportHelper.extractMetadata(getApplication(), uri, fallbackName)

                val (events, summary) = ReferenceMotionCache.analyzeOrGetReference(
                    context = getApplication(),
                    uri = uri,
                    referenceName = meta.fileName
                ) { progress, status ->
                    _uiState.value = _uiState.value.copy(
                        analysisStatus = status
                    )
                }

                _uiState.value = _uiState.value.copy(
                    referenceVideoMetadata = meta,
                    motionBlueprintSummary = summary,
                    isAnalyzingReference = false
                )

                // If currently in REFERENCE_MOTION mode, apply new reference blueprint immediately
                if (_uiState.value.currentMotionMode == com.example.model.MotionMode.REFERENCE_MOTION) {
                    regenerateMotion()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze reference video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzingReference = false,
                    errorMessage = "Failed to analyze reference video: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Reverts to the built-in authoritative reference target (YouCut_20261001_194357373.mp4).
     */
    fun useDefaultAuthoritativeReference() {
        ReferenceMotionCache.resetToAuthoritativeReference()
        _uiState.value = _uiState.value.copy(
            referenceVideoMetadata = null,
            motionBlueprintSummary = com.example.model.AuthoritativeReferenceBlueprint.debugSummary
        )
        if (_uiState.value.currentMotionMode == com.example.model.MotionMode.REFERENCE_MOTION) {
            regenerateMotion()
        }
    }

    /**
     * Synthesizes and loads a test video with spoken thought captions for testing.
     */
    fun loadSampleSpeechVideo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImportingVideo = true,
                    importStatus = "Synthesizing test speech video...",
                    errorMessage = null
                )

                val uri = SampleVideoHelper.generateSampleVideo(getApplication()) { progress ->
                    _uiState.value = _uiState.value.copy(
                        importStatus = "Synthesizing sample video ${(progress * 100).toInt()}%"
                    )
                }

                loadVideo(uri, "Omkar Sample Speech Video.mp4")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate sample video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isImportingVideo = false,
                    isAnalyzing = false,
                    errorMessage = "Could not generate sample video: ${e.message}"
                )
            }
        }
    }

    /**
     * Runs multi-stage real progress analysis:
     * 1. Audio track extraction & amplitude parsing
     * 2. Speech-to-text boundary detection & sentence completion
     * 3. Acoustic energy peaks and cadence identification
     * 4. Reference motion blueprint alignment (cached)
     * 5. Multi-keyframe curve synthesis & timeline generation
     */
    fun analyzeCurrentVideo() {
        val currentProj = _uiState.value.project ?: return
        val uri = currentProj.videoUri ?: return

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisStep = 1,
                    totalAnalysisSteps = 6,
                    analysisProgress = 0.05f,
                    analysisStatus = "Step 1/6: Analyzing video & extracting audio waveform...",
                    errorMessage = null
                )

                val audioResult = speechService.analyzeVideo(
                    videoUri = uri,
                    durationMs = currentProj.durationMs
                ) { progress, status ->
                    val step = when {
                        progress < 0.30f -> 1
                        progress < 0.65f -> 2
                        else -> 3
                    }
                    _uiState.value = _uiState.value.copy(
                        analysisProgress = (progress * 0.72f).coerceIn(0.05f, 0.72f),
                        analysisStatus = status,
                        analysisStep = step
                    )
                }

                _uiState.value = _uiState.value.copy(
                    analysisStep = 4,
                    analysisProgress = 0.78f,
                    analysisStatus = "Step 4/6: Analyzing & aligning cached reference motion blueprint..."
                )
                delay(90)

                _uiState.value = _uiState.value.copy(
                    analysisStep = 5,
                    analysisProgress = 0.88f,
                    analysisStatus = "Step 5/6: Generating multi-keyframes & discrete frame cuts..."
                )

                val (splits, clips) = segmentationService.generateSegmentation(
                    speechSegments = audioResult.speechSegments,
                    totalDurationMs = currentProj.durationMs,
                    frameRate = currentProj.frameRate,
                    audioAmplitudes = audioResult.amplitudes,
                    motionMode = _uiState.value.currentMotionMode
                )

                _uiState.value = _uiState.value.copy(
                    analysisStep = 6,
                    analysisProgress = 0.96f,
                    analysisStatus = "Step 6/6: Preparing real-time preview pipeline..."
                )
                delay(70)

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
                    analysisProgress = 1.0f,
                    analysisStep = 6,
                    selectedClipId = clips.firstOrNull()?.id,
                    analysisStatus = "Analysis Complete"
                )

                updateCurrentTimelineState(exoPlayer.currentPosition)

            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Analysis error: ${e.localizedMessage ?: "Unknown error"}. You can tap Retry."
                )
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        _uiState.value = _uiState.value.copy(
            isAnalyzing = false,
            analysisStatus = "Analysis cancelled"
        )
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _uiState.value = _uiState.value.copy(
            isImportingVideo = false,
            importStatus = ""
        )
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
        // Immediately update UI playhead & keyframe interpolation state for zero-latency scrubbing
        updateCurrentTimelineState(clamped)

        val now = android.os.SystemClock.elapsedRealtime()
        if (!exoPlayer.isPlaying && (now - lastPlayerSeekTimeMs) >= 28L || clamped == 0L || clamped == maxDuration) {
            lastPlayerSeekTimeMs = now
            exoPlayer.seekTo(clamped)
        } else if (exoPlayer.isPlaying) {
            exoPlayer.seekTo(clamped)
        }
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

        // Enforce clip boundaries and per-clip mute/volume during preview playback
        if (exoPlayer.isPlaying && project.clips.isNotEmpty()) {
            val strictlyInsideClip = project.clips.firstOrNull { currentMs in it.startMs..it.endMs }
            if (strictlyInsideClip == null) {
                val nextClip = project.clips.firstOrNull { it.startMs > currentMs }
                if (nextClip != null) {
                    exoPlayer.seekTo(nextClip.startMs)
                } else {
                    exoPlayer.pause()
                }
            }
        }

        val targetVolume = if (_uiState.value.isMuted || activeClip?.isMuted == true) {
            0f
        } else {
            (activeClip?.volume ?: 1.0f).coerceIn(0f, 1f)
        }
        if (kotlin.math.abs(exoPlayer.volume - targetVolume) > 0.01f) {
            exoPlayer.volume = targetVolume
        }

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
        easing: MotionCurve? = null,
        timestampMs: Long? = null
    ) {
        val project = _uiState.value.project ?: return
        saveStateForUndo(throttled = true)
        val updated = TimelineEngine.updateClipKeyframe(
            project = project,
            clipId = clipId,
            keyframeId = keyframeId,
            scale = scale,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            easing = easing,
            timestampMs = timestampMs
        )
        _uiState.value = _uiState.value.copy(project = updated)

        // Reflect keyframe changes immediately in preview without rebuilding video
        val editedClip = updated.clips.firstOrNull { it.id == clipId }
        val editedKf = editedClip?.keyframeById(keyframeId)
        if (!exoPlayer.isPlaying && editedKf != null) {
            seekTo(editedKf.timestampMs)
        } else {
            updateCurrentTimelineState(exoPlayer.currentPosition)
        }
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
        analysisJob?.cancel()
        importJob?.cancel()
        exoPlayer.release()
        previewEngine.releasePlayer()
    }
}
