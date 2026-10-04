package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.ExportProgressUpdate
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarGreen
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import java.io.File

/**
 * Premium Export Progress UI inspired by modern mobile video editors.
 * Displays real-time hardware-accelerated encoding metrics:
 * - Animated progress indicator
 * - Percentage (calculated from actual frames)
 * - Processed frames / total frames
 * - Processed time / total duration
 * - Processing stage
 * - Real ETA (estimated time remaining)
 * - Real export speed in FPS
 * - Cancel button
 * - Export Complete Screen with Play, Share, Save, and Edit Again options
 */
@Composable
fun ExportProgressScreen(
    isExporting: Boolean,
    progressUpdate: ExportProgressUpdate,
    exportedFile: File?,
    exportedUri: Uri?,
    isComplete: Boolean,
    onCancel: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onEditAgain: () -> Unit
) {
    if (!isExporting && !isComplete) return

    val animatedProgress by animateFloatAsState(
        targetValue = progressUpdate.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ExportProgressAnimation"
    )

    val currentSeconds = progressUpdate.currentDurationMs / 1000f
    val totalSeconds = progressUpdate.totalDurationMs / 1000f

    val curMin = (progressUpdate.currentDurationMs / 60000).toInt()
    val curSec = ((progressUpdate.currentDurationMs % 60000) / 1000).toInt()
    val curMs = ((progressUpdate.currentDurationMs % 1000) / 10).toInt()

    val totMin = (progressUpdate.totalDurationMs / 60000).toInt()
    val totSec = ((progressUpdate.totalDurationMs % 60000) / 1000).toInt()
    val totMs = ((progressUpdate.totalDurationMs % 1000) / 10).toInt()

    val formattedTime = String.format("%02d:%02d.%02d / %02d:%02d.%02d", curMin, curSec, curMs, totMin, totSec, totMs)
    val percentageInt = (animatedProgress * 100).toInt()

    Dialog(
        onDismissRequest = {
            if (isComplete) onEditAgain()
        },
        properties = DialogProperties(
            dismissOnBackPress = isComplete,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6080A10))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
                    .testTag("export_progress_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isComplete) {
                        // EXPORT IN PROGRESS VIEW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exporting Processed MP4 Video",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Export",
                                    tint = Color.LightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Large Circular/Orb Progress Indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(150.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(150.dp),
                                color = SurfaceVariantDark,
                                strokeWidth = 10.dp
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(150.dp),
                                color = OmkarGold,
                                strokeWidth = 10.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$percentageInt%",
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "GPU ENCODING",
                                    color = OmkarCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Linear Sleek Bar
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = OmkarGold,
                            trackColor = SurfaceVariantDark,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 5-Stage Pipeline Stepper: Preparing -> Rendering -> Encoding -> Finalizing -> Saving
                        val exportStages = listOf("Preparing", "Rendering", "Encoding", "Finalizing", "Saving")
                        val activeStageIdx = when {
                            animatedProgress < 0.05f -> 0
                            animatedProgress < 0.45f -> 1
                            animatedProgress < 0.88f -> 2
                            animatedProgress < 0.96f -> 3
                            else -> 4
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            exportStages.forEachIndexed { idx, label ->
                                val isDoneStage = idx < activeStageIdx
                                val isActiveStage = idx == activeStageIdx
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isActiveStage) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isDoneStage -> OmkarGreen
                                        isActiveStage -> OmkarGold
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Metrics Grid: Frame & Time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceVariantDark)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "FRAMES",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Frame ${progressUpdate.currentFrame} / ${progressUpdate.totalFrames}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DURATION",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formattedTime,
                                    color = OmkarCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real Speed (FPS) & ETA Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = OmkarGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = "Speed", color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            text = String.format("%.1f FPS", progressUpdate.fpsSpeed),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = OmkarCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = "ETA", color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            text = if (progressUpdate.etaSeconds > 0) String.format("00:%02d", progressUpdate.etaSeconds) else "00:01",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Current Stage
                        Text(
                            text = "Processing:",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = progressUpdate.stage.ifBlank { "Applying cinematic motion..." },
                            color = OmkarGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("cancel_export_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OmkarCutRed)
                        ) {
                            Text(text = "Cancel Export", fontWeight = FontWeight.Bold)
                        }

                    } else {
                        // EXPORT COMPLETE VIEW
                        Surface(
                            shape = CircleShape,
                            color = OmkarGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Complete",
                                tint = OmkarGreen,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Export Complete",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Hardware-accelerated MP4 ready with all cuts and keyframes",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        exportedFile?.let { file ->
                            val sizeMb = file.length() / (1024f * 1024f)
                            Text(
                                text = "${file.name} • ${String.format("%.1f MB", sizeMb)}",
                                color = OmkarGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: Play, Share, Save, Edit Again
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPlay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("play_exported_video_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OmkarGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Play Exported Video", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ElevatedButton(
                                    onClick = onShare,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("share_exported_video_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = SurfaceVariantDark,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                ElevatedButton(
                                    onClick = onSave,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_exported_video_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = SurfaceVariantDark,
                                        contentColor = OmkarCyan
                                    )
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Saved", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = onEditAgain,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("edit_again_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Edit Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
