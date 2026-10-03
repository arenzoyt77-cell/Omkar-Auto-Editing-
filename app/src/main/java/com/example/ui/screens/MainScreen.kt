package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.EditorMode
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarGreen
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.EditorTab
import com.example.ui.viewmodel.VideoProcessingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: VideoProcessingViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Real Android Media Pickers
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideo(uri, "Original Video")
        }
    }

    val referencePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadReferenceVideo(uri, "Reference Motion Video")
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OmkarGold,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OMKAR AUTOMATIC VIDEO MAKER",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Speech Detection • Hardware Accelerated Export",
                                fontSize = 10.sp,
                                color = OmkarGold
                            )
                        }
                    }
                },
                actions = {
                    // Undo & Redo Actions
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) Color.White else Color.DarkGray
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Color.DarkGray
                        )
                    }

                    // Mode Switcher: SIMPLE vs SMART
                    FilterChip(
                        selected = uiState.currentMode == EditorMode.SMART,
                        onClick = {
                            val nextMode = if (uiState.currentMode == EditorMode.SMART) EditorMode.SIMPLE else EditorMode.SMART
                            viewModel.setMode(nextMode)
                        },
                        label = {
                            Text(
                                text = if (uiState.currentMode == EditorMode.SMART) "SMART" else "SIMPLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmkarGold,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Top Export Button
                    if (uiState.project != null) {
                        ElevatedButton(
                            onClick = { viewModel.openExportDialog() },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = OmkarGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(32.dp)
                                .testTag("top_export_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "EXPORT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val project = uiState.project

            // Importing video feedback banner
            if (uiState.isImportingVideo) {
                ImportLoadingBanner(
                    statusText = uiState.importStatus,
                    onCancel = { viewModel.cancelImport() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (project == null) {
                // Initial State: Prompt User to Import Video or Use Speech Test Video
                VideoImportCard(
                    onImportClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onTrySampleVideo = { viewModel.loadSampleSpeechVideo() },
                    onSelectReferenceVideo = {
                        referencePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            } else {
                // Video is loaded: Real-Time Mobile Video Editor Workspace

                // Video Metadata details badge (filename, duration, resolution, size, fps)
                uiState.originalVideoMetadata?.let { meta ->
                    VideoMetadataCard(
                        metadata = meta,
                        onChangeVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onReAnalyze = { viewModel.analyzeCurrentVideo() }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 1. Large Video Preview Player showing exact keyframes, cuts & text overlays
                VideoPreviewPlayer(
                    playbackState = playbackState,
                    totalDurationMs = project.effectiveDurationMs,
                    onAttachSurface = { viewModel.setVideoSurface(it) },
                    onDetachSurface = { viewModel.setVideoSurface(null) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeek = { ms -> viewModel.seekTo(ms) },
                    onRestart = { viewModel.seekTo(0L) },
                    isMuted = uiState.isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    textOverlays = project.textOverlays
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Interactive Multi-Layer Timeline View
                TimelineView(
                    project = project,
                    currentPlayheadMs = playbackState.currentPositionMs,
                    selectedClipId = uiState.selectedClipId,
                    selectedSplitId = uiState.selectedSplitId,
                    onSeek = { ms -> viewModel.seekTo(ms) },
                    onSelectClip = { clipId -> viewModel.selectClip(clipId) },
                    onSelectSplit = { splitId -> viewModel.selectSplit(splitId) },
                    onAddSplitAtPlayhead = { viewModel.addSplitAtPlayhead() },
                    onDeleteSplit = { splitId -> viewModel.deleteSplit(splitId) },
                    onMoveSplit = { splitId, newMs -> viewModel.moveSplit(splitId, newMs) },
                    onDeleteClip = { clipId -> viewModel.deleteClip(clipId) },
                    onDuplicateClip = { clipId -> viewModel.duplicateClip(clipId) },
                    onToggleClipMute = { clipId -> viewModel.toggleClipMute(clipId) },
                    onOpenTextOverlayDialog = { viewModel.openTextOverlayDialog() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Modern Bottom Editor Workspace Tabs (Edit, Keyframes, Reference, Audio, Text, Captions, Export)
                TabRow(
                    selectedTabIndex = uiState.currentTab.ordinal,
                    containerColor = SurfaceDark,
                    contentColor = OmkarGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                            color = OmkarGold,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    EditorTab.values().forEach { tab ->
                        Tab(
                            selected = uiState.currentTab == tab,
                            onClick = { viewModel.setEditorTab(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workspace Tab Content
                when (uiState.currentTab) {
                    EditorTab.EDIT -> {
                        EditToolsPanel(
                            project = project,
                            selectedClipId = uiState.selectedClipId,
                            currentPlayheadMs = playbackState.currentPositionMs,
                            onSplit = { viewModel.addSplitAtPlayhead() },
                            onTrimStart = { clipId, ms -> viewModel.trimClipStart(clipId, ms) },
                            onTrimEnd = { clipId, ms -> viewModel.trimClipEnd(clipId, ms) },
                            onDelete = { clipId -> viewModel.deleteClip(clipId) },
                            onDuplicate = { clipId -> viewModel.duplicateClip(clipId) }
                        )
                    }

                    EditorTab.KEYFRAMES -> {
                        val activeClip = project.clips.firstOrNull { it.id == uiState.selectedClipId }
                            ?: project.clips.firstOrNull()

                        if (activeClip != null) {
                            ClipInspector(
                                clip = activeClip,
                                onDismiss = { viewModel.selectClip(null) },
                                onApplyPreset = { preset -> viewModel.setClipPreset(activeClip.id, preset) },
                                onUpdateKeyframe = { kfId, scale, posX, posY, rot, curve ->
                                    viewModel.updateKeyframe(activeClip.id, kfId, scale, posX, posY, rot, curve)
                                },
                                onAddKeyframe = { scale, posX, posY, curve ->
                                    viewModel.addKeyframeAtPlayhead(activeClip.id, scale, posX, posY, curve)
                                },
                                onDeleteKeyframe = { kfId ->
                                    viewModel.deleteKeyframe(activeClip.id, kfId)
                                }
                            )
                        } else {
                            Text(
                                text = "Select a clip on the timeline to edit keyframes",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    EditorTab.REFERENCE -> {
                        ReferenceMotionPanel(
                            summary = uiState.motionBlueprintSummary,
                            referenceMetadata = uiState.referenceVideoMetadata,
                            currentMotionMode = uiState.currentMotionMode,
                            isAnalyzingReference = uiState.isAnalyzingReference,
                            onSelectReferenceVideo = {
                                referencePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onResetToDefaultReference = { viewModel.useDefaultAuthoritativeReference() },
                            onSetMotionMode = { mode -> viewModel.setMotionMode(mode) },
                            onReapplyMotion = { viewModel.regenerateMotion() }
                        )
                    }

                    EditorTab.AUDIO -> {
                        AudioToolsPanel(
                            project = project,
                            selectedClipId = uiState.selectedClipId,
                            isMasterMuted = uiState.isMuted,
                            onToggleMasterMute = { viewModel.toggleMute() },
                            onToggleClipMute = { clipId -> viewModel.toggleClipMute(clipId) },
                            onSetClipVolume = { clipId, vol -> viewModel.setClipVolume(clipId, vol) }
                        )
                    }

                    EditorTab.TEXT -> {
                        TextToolsPanel(
                            project = project,
                            onAddText = { viewModel.openTextOverlayDialog() },
                            onEditText = { overlay -> viewModel.openTextOverlayDialog(overlay) },
                            onDeleteText = { overlayId -> viewModel.deleteTextOverlay(overlayId) }
                        )
                    }

                    EditorTab.CAPTIONS -> {
                        CaptionsPanel(
                            project = project,
                            onSeekTo = { viewModel.seekTo(it) }
                        )
                    }

                    EditorTab.EXPORT -> {
                        ExportSettingsPanel(
                            project = project,
                            onOpenExport = { viewModel.openExportDialog() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SMART MODE Diagnostics Panel
                if (uiState.currentMode == EditorMode.SMART) {
                    SmartModePanel(
                        project = project,
                        onSeek = { ms -> viewModel.seekTo(ms) },
                        onDeleteSplit = { splitId -> viewModel.deleteSplit(splitId) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }

    // Multi-Stage Real Analysis Progress Modal Dialog
    if (uiState.isAnalyzing) {
        AnalysisProgressDialog(
            progress = uiState.analysisProgress,
            statusText = uiState.analysisStatus,
            currentStep = uiState.analysisStep,
            totalSteps = uiState.totalAnalysisSteps,
            onCancel = { viewModel.cancelAnalysis() }
        )
    }

    // Export Settings Configuration Modal Dialog
    if (uiState.showExportDialog && uiState.project != null) {
        ExportDialog(
            initialSettings = uiState.project!!.exportSettings,
            onDismiss = { viewModel.dismissExportDialog() },
            onConfirmExport = { newSettings ->
                viewModel.updateExportSettings(newSettings)
                viewModel.startExport()
            }
        )
    }

    // Premium Real-time Hardware Export Progress Screen
    ExportProgressScreen(
        isExporting = uiState.isExporting,
        progressUpdate = uiState.exportProgressUpdate,
        exportedFile = uiState.exportedFile,
        exportedUri = uiState.exportedUri,
        isComplete = uiState.isExportComplete,
        onCancel = { viewModel.cancelExport() },
        onPlay = {
            val file = uiState.exportedFile
            val safeUri = if (file != null && file.exists()) {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                } catch (_: Exception) {
                    uiState.exportedUri
                }
            } else {
                uiState.exportedUri
            }

            safeUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Play Exported Video"))
            }
        },
        onShare = {
            val file = uiState.exportedFile
            val safeUri = if (file != null && file.exists()) {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                } catch (_: Exception) {
                    uiState.exportedUri
                }
            } else {
                uiState.exportedUri
            }

            safeUri?.let { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Exported MP4"))
            }
        },
        onSave = {
            viewModel.dismissExportComplete()
        },
        onEditAgain = {
            viewModel.dismissExportComplete()
        }
    )

    // Non-destructive Text Overlay Editor Modal
    if (uiState.showTextOverlayDialog) {
        TextOverlayDialog(
            initialOverlay = uiState.editingTextOverlay,
            defaultStartMs = playbackState.currentPositionMs,
            defaultEndMs = (playbackState.currentPositionMs + 3000L).coerceAtMost(uiState.project?.effectiveDurationMs ?: 10000L),
            onDismiss = { viewModel.dismissTextOverlayDialog() },
            onSave = { overlay -> viewModel.saveTextOverlay(overlay) },
            onDelete = { overlayId -> viewModel.deleteTextOverlay(overlayId) }
        )
    }
}

@Composable
private fun ImportLoadingBanner(
    statusText: String,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Importing Video...",
                    color = OmkarGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cancel",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onCancel() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = OmkarGold,
                trackColor = SurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText.ifEmpty { "Reading video metadata..." },
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EditToolsPanel(
    project: com.example.model.VideoProject,
    selectedClipId: String?,
    currentPlayheadMs: Long,
    onSplit: () -> Unit,
    onTrimStart: (String, Long) -> Unit,
    onTrimEnd: (String, Long) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit
) {
    val activeClip = project.clips.firstOrNull { it.id == selectedClipId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Clip Editing Tools",
                color = OmkarGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = onSplit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = OmkarGold
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (activeClip != null) {
                    ElevatedButton(
                        onClick = { onTrimStart(activeClip.id, currentPlayheadMs) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarCyan
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trim Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    ElevatedButton(
                        onClick = { onTrimEnd(activeClip.id, currentPlayheadMs) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarCyan
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trim End", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (activeClip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDuplicate(activeClip.id) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicate", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onDelete(activeClip.id) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OmkarCutRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioToolsPanel(
    project: com.example.model.VideoProject,
    selectedClipId: String?,
    isMasterMuted: Boolean,
    onToggleMasterMute: () -> Unit,
    onToggleClipMute: (String) -> Unit,
    onSetClipVolume: (String, Float) -> Unit
) {
    val activeClip = project.clips.firstOrNull { it.id == selectedClipId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Audio & Acoustic Controls",
                color = OmkarCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Master Audio Mute Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Master Video Sound", color = Color.White, fontSize = 12.sp)
                ElevatedButton(
                    onClick = onToggleMasterMute,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isMasterMuted) OmkarCutRed else SurfaceVariantDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isMasterMuted) "Unmute Master" else "Mute Master", fontSize = 11.sp)
                }
            }

            if (activeClip != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clip #${activeClip.index} Volume", color = OmkarGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(activeClip.volume * 100).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                }
                Slider(
                    value = activeClip.volume,
                    onValueChange = { onSetClipVolume(activeClip.id, it) },
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = OmkarGold,
                        activeTrackColor = OmkarGold
                    )
                )
            }
        }
    }
}

@Composable
private fun TextToolsPanel(
    project: com.example.model.VideoProject,
    onAddText: () -> Unit,
    onEditText: (com.example.model.TextOverlay) -> Unit,
    onDeleteText: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Text Overlays & Titles (${project.textOverlays.size})",
                    color = OmkarGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                ElevatedButton(
                    onClick = onAddText,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = OmkarGold,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (project.textOverlays.isEmpty()) {
                Text(
                    text = "No text overlays added yet. Tap 'Add Text' to overlay title or callout.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                project.textOverlays.forEach { overlay ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .clickable { onEditText(overlay) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = overlay.text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = overlay.formattedTimeRange, color = OmkarCyan, fontSize = 10.sp)
                        }
                        IconButton(onClick = { onDeleteText(overlay.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OmkarCutRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionsPanel(
    project: com.example.model.VideoProject,
    onSeekTo: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Detected Spoken Thought Segments (${project.speechSegments.size})",
                color = OmkarPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            project.speechSegments.forEach { seg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .clickable { onSeekTo(seg.startMs) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = seg.text, color = Color.White, fontSize = 12.sp)
                        Text(text = String.format("%.2fs - %.2fs", seg.startMs / 1000f, seg.endMs / 1000f), color = OmkarGold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportSettingsPanel(
    project: com.example.model.VideoProject,
    onOpenExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Hardware-Accelerated Video Export",
                color = OmkarGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "GPU MediaCodec encoding with real-time keyframe interpolation and lossless audio muxing.",
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmkarGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("start_hardware_export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "START HARDWARE EXPORT", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SmartModePanel(
    project: com.example.model.VideoProject,
    onSeek: (Long) -> Unit,
    onDeleteSplit: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Smart Mode Intelligence",
                color = OmkarGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Detected ${project.splitPoints.size} frame-accurate split points and ${project.clips.size} clips.",
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.splitPoints) { split ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.clickable { onSeek(split.timestampMs) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cut ${(split.timestampMs / 1000f)}s",
                                color = OmkarCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoImportCard(
    onImportClick: () -> Unit,
    onTrySampleVideo: () -> Unit,
    onSelectReferenceVideo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = OmkarGold.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = OmkarGold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Import Video to Begin",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Automatically detect speech boundaries, create intelligent cuts, and apply cinematic keyframes.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmkarGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("import_video_button")
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Select Video from Device", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onTrySampleVideo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OmkarCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("sample_video_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Try Sample Speech Video", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSelectReferenceVideo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("import_reference_button")
            ) {
                Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Select Reference Video from Device", fontSize = 11.sp)
            }
        }
    }
}
