package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarGreen
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

private data class AnalysisStepInfo(
    val stepIndex: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun AnalysisProgressDialog(
    progress: Float,
    statusText: String,
    currentStep: Int,
    totalSteps: Int = 6,
    onCancel: () -> Unit
) {
    val steps = listOf(
        AnalysisStepInfo(
            stepIndex = 1,
            title = "Analyzing Video",
            description = "Extracting audio channel data & acoustic waveform amplitudes",
            icon = Icons.Default.GraphicEq,
            accentColor = OmkarCyan
        ),
        AnalysisStepInfo(
            stepIndex = 2,
            title = "Detecting Speech",
            description = "Identifying natural pauses, utterances, and completed sentences",
            icon = Icons.Default.RecordVoiceOver,
            accentColor = OmkarPurple
        ),
        AnalysisStepInfo(
            stepIndex = 3,
            title = "Detecting Important Moments",
            description = "Pinpointing acoustic energy peaks, speech cadence & climaxes",
            icon = Icons.Default.Tune,
            accentColor = OmkarGold
        ),
        AnalysisStepInfo(
            stepIndex = 4,
            title = "Analyzing Reference Motion",
            description = "Applying continuous dynamic punch zooms and X/Y camera reframing",
            icon = Icons.Default.Transform,
            accentColor = OmkarCyan
        ),
        AnalysisStepInfo(
            stepIndex = 5,
            title = "Generating Keyframes",
            description = "Snapping cuts to discrete video frames & calculating easing curves",
            icon = Icons.Default.AutoAwesome,
            accentColor = OmkarGreen
        ),
        AnalysisStepInfo(
            stepIndex = 6,
            title = "Preparing Preview",
            description = "Synchronizing real-time multi-keyframe playback pipeline",
            icon = Icons.Default.Movie,
            accentColor = OmkarGold
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                .testTag("analysis_progress_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OmkarGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = OmkarGold,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .rotate(rotation)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTOMATIC ANALYSIS",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "AI Speech Boundary & Motion Engine",
                                fontSize = 10.sp,
                                color = OmkarGold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                            color = OmkarGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = OmkarGold,
                    trackColor = SurfaceVariantDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Current Active Status text
                Text(
                    text = statusText.ifEmpty { "Analyzing video stream..." },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Multi-step Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark.copy(alpha = 0.5f))
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEach { step ->
                        val isDone = currentStep > step.stepIndex || (currentStep == step.stepIndex && progress >= 1.0f)
                        val isCurrent = currentStep == step.stepIndex && progress < 1.0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) step.accentColor.copy(alpha = 0.12f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step Icon
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isDone -> OmkarGreen.copy(alpha = 0.2f)
                                    isCurrent -> step.accentColor.copy(alpha = 0.25f)
                                    else -> Color.DarkGray.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = OmkarGreen,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = if (isCurrent) step.accentColor else Color.Gray,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = step.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isDone -> Color.White
                                            isCurrent -> step.accentColor
                                            else -> Color.Gray
                                        }
                                    )
                                    val stepProgress = when {
                                        isDone -> 1.0f
                                        isCurrent -> ((progress * totalSteps) - (step.stepIndex - 1)).coerceIn(0.15f, 0.95f)
                                        else -> 0.0f
                                    }
                                    Text(
                                        text = "${(stepProgress * 100).toInt()}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) OmkarGreen else if (isCurrent) step.accentColor else Color.DarkGray
                                    )
                                }
                                Text(
                                    text = step.description,
                                    fontSize = 9.sp,
                                    color = if (isCurrent) Color.LightGray else Color(0xFF64748B),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val stepBarProgress = when {
                                    isDone -> 1.0f
                                    isCurrent -> ((progress * totalSteps) - (step.stepIndex - 1)).coerceIn(0.15f, 0.95f)
                                    else -> 0.0f
                                }
                                LinearProgressIndicator(
                                    progress = { stepBarProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isDone) OmkarGreen else step.accentColor,
                                    trackColor = SurfaceDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("cancel_analysis_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Cancel Analysis", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
