package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ClipSegment
import com.example.model.EditorMode
import com.example.model.ExportSettings
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.model.SplitPoint
import com.example.model.VideoProject
import com.example.service.PlaybackState
import com.example.service.SampleVideoHelper
import com.example.service.SpeechAnalysisService
import com.example.service.TimelineEngine
import com.example.service.VideoExportService
import com.example.service.VideoPreviewEngine
import com.example.service.VideoSegmentationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class VideoMakerUiState(
    val project: VideoProject? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val analysisStatus: String = "",
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportStatus: String = "",
    val exportedFile: File? = null,
    val exportedUri: Uri? = null,
    val selectedClipId: String? = null,
    val selectedSplitId: String? = null,
    val errorMessage: String? = null,
    val currentMode: EditorMode = EditorMode.SIMPLE,
    val showExportDialog: Boolean = false,
    val showExportSuccessDialog: Boolean = false
)

class VideoMakerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VideoMakerViewModel"
    }

    private val speechService = SpeechAnalysisService(application)
    private val segmentationService = VideoSegmentationService()
    val previewEngine = VideoPreviewEngine(application)
    private val exportService = VideoExportService(application)

    private val _uiState = MutableStateFlow(VideoMakerUiState())
    val uiState: StateFlow<VideoMakerUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = previewEngine.playbackState

    fun loadVideo(uri: Uri, fileName: String = "Selected Video") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.05f,
                    analysisStatus = "Reading video metadata...",
                    errorMessage = null
                )

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
                        val w = wStr?.toIntOrNull() ?: 1280
                        val h = hStr?.toIntOrNull() ?: 720
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

                previewEngine.prepareProject(initialProject) {
                    // Start auto analysis immediately upon loading video
                    analyzeCurrentVideo()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Failed to load video: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadSampleSpeechVideo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.05f,
                    analysisStatus = "Preparing sample video with speech...",
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
                Log.e(TAG, "Failed to create sample video: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Could not generate sample video: ${e.message}"
                )
            }
        }
    }

    fun analyzeCurrentVideo() {
        val currentProj = _uiState.value.project ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.1f,
                    analysisStatus = "Analyzing spoken thoughts & natural pauses...",
                    errorMessage = null
                )

                val audioResult = speechService.analyzeVideo(
                    videoUri = currentProj.videoUri,
                    durationMs = currentProj.durationMs
                ) { progress, status ->
                    _uiState.value = _uiState.value.copy(
                        analysisProgress = progress,
                        analysisStatus = status
                    )
                }

                _uiState.value = _uiState.value.copy(
                    analysisProgress = 0.95f,
                    analysisStatus = "Aligning split boundaries and applying cinematic keyframes..."
                )

                val (splits, clips) = segmentationService.generateSegmentation(
                    speechSegments = audioResult.speechSegments,
                    totalDurationMs = currentProj.durationMs,
                    frameRate = currentProj.frameRate
                )

                val updatedProject = currentProj.copy(
                    speechSegments = audioResult.speechSegments,
                    splitPoints = splits,
                    clips = clips,
                    audioAmplitudes = audioResult.amplitudes,
                    isAnalyzed = true
                )

                _uiState.value = _uiState.value.copy(
                    project = updatedProject,
                    isAnalyzing = false,
                    selectedClipId = clips.firstOrNull()?.id,
                    analysisStatus = "Analysis Complete"
                )

                previewEngine.prepareProject(updatedProject)

            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Analysis error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun setMode(mode: EditorMode) {
        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            project = _uiState.value.project?.copy(mode = mode)
        )
    }

    fun selectClip(clipId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedClipId = clipId,
            selectedSplitId = null
        )
        // If clip selected, jump playhead to its start
        val clip = _uiState.value.project?.clips?.firstOrNull { it.id == clipId }
        if (clip != null) {
            previewEngine.seekTo(clip.startMs)
        }
    }

    fun selectSplit(splitId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedSplitId = splitId,
            selectedClipId = null
        )
        val split = _uiState.value.project?.splitPoints?.firstOrNull { it.id == splitId }
        if (split != null) {
            previewEngine.seekTo(split.timestampMs)
        }
    }

    fun addSplitAtPlayhead() {
        val project = _uiState.value.project ?: return
        val currentPlayhead = previewEngine.playbackState.value.currentPositionMs
        val updated = TimelineEngine.addSplitPoint(project, currentPlayhead)
        _uiState.value = _uiState.value.copy(project = updated)
        previewEngine.prepareProject(updated)
    }

    fun moveSplit(splitId: String, newTimestampMs: Long) {
        val project = _uiState.value.project ?: return
        val updated = TimelineEngine.moveSplitPoint(project, splitId, newTimestampMs)
        _uiState.value = _uiState.value.copy(project = updated)
    }

    fun deleteSplit(splitId: String) {
        val project = _uiState.value.project ?: return
        val updated = TimelineEngine.deleteSplitPoint(project, splitId)
        _uiState.value = _uiState.value.copy(
            project = updated,
            selectedSplitId = null
        )
        previewEngine.prepareProject(updated)
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
    }

    fun setClipPreset(clipId: String, preset: MotionPreset) {
        val project = _uiState.value.project ?: return
        val updated = TimelineEngine.setClipPreset(project, clipId, preset)
        _uiState.value = _uiState.value.copy(project = updated)
    }

    fun updateExportSettings(settings: ExportSettings) {
        val project = _uiState.value.project ?: return
        _uiState.value = _uiState.value.copy(
            project = project.copy(exportSettings = settings)
        )
    }

    fun openExportDialog() {
        previewEngine.pause()
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun dismissExportSuccessDialog() {
        _uiState.value = _uiState.value.copy(showExportSuccessDialog = false)
    }

    fun startExport() {
        val project = _uiState.value.project ?: return
        dismissExportDialog()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportProgress = 0.01f,
                exportStatus = "Starting export...",
                errorMessage = null
            )

            exportService.exportProject(
                project = project,
                onProgress = { progress, status ->
                    _uiState.value = _uiState.value.copy(
                        exportProgress = progress,
                        exportStatus = status
                    )
                },
                onError = { err ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = err
                    )
                },
                onSuccess = { file, uri ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedFile = file,
                        exportedUri = uri,
                        showExportSuccessDialog = true
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        previewEngine.releasePlayer()
    }
}
