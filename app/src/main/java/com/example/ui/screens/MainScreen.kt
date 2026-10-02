package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarGreen
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.VideoProcessingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: VideoProcessingViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideo(uri, "Selected Video")
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
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Speech-Boundary Detection • Keyframe Motion",
                                fontSize = 10.sp,
                                color = OmkarGold
                            )
                        }
                    }
                },
                actions = {
                    // Mode Switcher: SIMPLE MODE vs SMART MODE
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        FilterChip(
                            selected = uiState.currentMode == EditorMode.SIMPLE,
                            onClick = { viewModel.setMode(EditorMode.SIMPLE) },
                            label = { Text("SIMPLE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OmkarGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = uiState.currentMode == EditorMode.SMART,
                            onClick = { viewModel.setMode(EditorMode.SMART) },
                            label = { Text("SMART", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OmkarCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            val project = uiState.project

            if (project == null) {
                // Initial State: Pick Video or Try Sample Video
                InitialVideoSelectionView(
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onTrySampleVideo = { viewModel.loadSampleSpeechVideo() }
                )
            } else {
                // Video is loaded: Editor Pipeline View
                // 1. Video Preview Player with dynamic Keyframe Graphics Layer
                VideoPreviewPlayer(
                    previewEngine = viewModel.previewEngine,
                    playbackState = playbackState,
                    totalDurationMs = project.durationMs
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar (Analyze Video, Preview Result, Edit Timeline, Export Video)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { viewModel.analyzeCurrentVideo() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarGold
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("analyze_video_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Analyze", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    ElevatedButton(
                        onClick = { viewModel.previewEngine.togglePlayPause() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    ElevatedButton(
                        onClick = { viewModel.openExportDialog() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = OmkarGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_video_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Detected Clips Strip
                if (project.clips.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Clips (${project.clips.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Auto Keyframed (100% -> 112%)",
                            fontSize = 11.sp,
                            color = OmkarGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(project.clips) { clip ->
                            val isSelected = clip.id == uiState.selectedClipId
                            Surface(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) OmkarGold else BorderDark,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectClip(clip.id) },
                                color = if (isSelected) OmkarGold.copy(alpha = 0.15f) else SurfaceDark
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Clip 0${clip.index}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) OmkarGold else Color.White
                                        )
                                        Text(
                                            text = "${(clip.durationMs / 1000f)}s",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = clip.speechText.ifBlank { "Thought boundary" },
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Diamond,
                                            contentDescription = null,
                                            tint = OmkarCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = clip.motionPreset.displayName,
                                            fontSize = 9.sp,
                                            color = OmkarCyan,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeline Section
                TimelineView(
                    project = project,
                    currentPlayheadMs = playbackState.currentPositionMs,
                    selectedClipId = uiState.selectedClipId,
                    selectedSplitId = uiState.selectedSplitId,
                    onSeek = { ms -> viewModel.previewEngine.seekTo(ms) },
                    onSelectClip = { clipId -> viewModel.selectClip(clipId) },
                    onSelectSplit = { splitId -> viewModel.selectSplit(splitId) },
                    onAddSplitAtPlayhead = { viewModel.addSplitAtPlayhead() },
                    onDeleteSplit = { splitId -> viewModel.deleteSplit(splitId) },
                    onMoveSplit = { splitId, newMs -> viewModel.moveSplit(splitId, newMs) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clip Inspector (if clip selected)
                val selectedClip = project.clips.firstOrNull { it.id == uiState.selectedClipId }
                if (selectedClip != null) {
                    ClipInspector(
                        clip = selectedClip,
                        onDismiss = { viewModel.selectClip(null) },
                        onApplyPreset = { preset -> viewModel.setClipPreset(selectedClip.id, preset) },
                        onUpdateKeyframe = { kfId, scale, posX, posY, rot, curve ->
                            viewModel.updateKeyframe(selectedClip.id, kfId, scale, posX, posY, rot, curve)
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // SMART MODE Diagnostics Panel
                if (uiState.currentMode == EditorMode.SMART) {
                    SmartModePanel(
                        project = project,
                        onSeek = { ms -> viewModel.previewEngine.seekTo(ms) },
                        onDeleteSplit = { splitId -> viewModel.deleteSplit(splitId) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Secondary change video button
                OutlinedButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Choose Another Video", color = Color.LightGray)
                }
            }

            // Analysis progress card
            if (uiState.isAnalyzing) {
                Spacer(modifier = Modifier.height(12.dp))
                ProcessingOverlay(
                    title = "Speech & Thought Analysis Pipeline",
                    statusText = uiState.analysisStatus,
                    progress = uiState.analysisProgress,
                    isExport = false
                )
            }

            // Export progress card
            if (uiState.isExporting) {
                Spacer(modifier = Modifier.height(12.dp))
                ProcessingOverlay(
                    title = "Exporting Processed MP4 Video",
                    statusText = uiState.exportStatus,
                    progress = uiState.exportProgress,
                    isExport = true
                )
            }
        }
    }

    // Export Settings Dialog
    if (uiState.showExportDialog && uiState.project != null) {
        ExportDialog(
            initialSettings = uiState.project!!.exportSettings,
            onDismiss = { viewModel.dismissExportDialog() },
            onConfirmExport = { settings ->
                viewModel.updateExportSettings(settings)
                viewModel.startExport()
            }
        )
    }

    // Export Success Dialog
    if (uiState.showExportSuccessDialog && uiState.exportedFile != null) {
        val file = uiState.exportedFile!!
        val fileMb = String.format("%.2f", file.length() / (1024f * 1024f))

        AlertDialog(
            onDismissRequest = { viewModel.dismissExportSuccessDialog() },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OmkarGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Export Complete (100%)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your automatic speech-split video with cinematic keyframes has been saved successfully as MP4 (H.264 / AAC).",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundDark
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "File: ${file.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = "Size: $fileMb MB", fontSize = 11.sp, color = OmkarCyan)
                            Text(text = "Location: ${file.parent}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uiState.exportedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmkarGold, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share Video", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissExportSuccessDialog() }) {
                    Text(text = "Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun InitialVideoSelectionView(
    onPickVideo: () -> Unit,
    onTrySampleVideo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .testTag("initial_selection_card"),
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
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = null,
                    tint = OmkarGold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OMKAR AUTOMATIC VIDEO MAKER",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Automatically detect natural spoken thoughts, split video without cutting words, apply smooth cinematic keyframes, and export real H.264 MP4.",
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Pick Button
            ElevatedButton(
                onClick = onPickVideo,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = OmkarGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("select_video_button")
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Select Video from Device", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Try Sample Video Button
            OutlinedButton(
                onClick = onTrySampleVideo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("try_sample_video_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OmkarCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Try Sample Speech Video", color = OmkarCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundDark)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureBadge("Semantic Speech Boundary", "Splits at completed thoughts, never slicing mid-word")
                FeatureBadge("Subtle Cinematic Keyframes", "100% -> 112% smooth Ease-In-Out motion per clip")
                FeatureBadge("Interactive Timeline", "Fine-tune splits, adjust zoom, tweak keyframe curves")
                FeatureBadge("Hardware MP4 Export", "Real H.264 video with AAC audio synchronization")
            }
        }
    }
}

@Composable
private fun FeatureBadge(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = OmkarGreen,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
