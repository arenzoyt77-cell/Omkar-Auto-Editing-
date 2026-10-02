package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipSegment
import com.example.model.SplitPoint
import com.example.model.VideoProject
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    project: VideoProject,
    currentPlayheadMs: Long,
    selectedClipId: String?,
    selectedSplitId: String?,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectSplit: (String) -> Unit,
    onAddSplitAtPlayhead: () -> Unit,
    onDeleteSplit: (String) -> Unit,
    onMoveSplit: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    val totalDurationMs = project.durationMs.coerceAtLeast(1000L)
    val baseTimelineWidthDp = 360f
    val timelineWidthDp = (baseTimelineWidthDp * zoomScale).coerceAtLeast(baseTimelineWidthDp)
    val msToDpRatio = timelineWidthDp / totalDurationMs.toFloat()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("timeline_container")
    ) {
        // Timeline Header & Tool Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = OmkarGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Professional Timeline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${project.clips.size} Clips • ${project.splitPoints.size} Splits)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Split Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedButton(
                onClick = onAddSplitAtPlayhead,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = OmkarGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_split_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Split at Playhead", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            if (selectedSplitId != null) {
                val activeSplit = project.splitPoints.firstOrNull { it.id == selectedSplitId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (activeSplit != null) {
                                onMoveSplit(activeSplit.id, (activeSplit.timestampMs - 150L).coerceAtLeast(0L))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Nudge -150ms", tint = OmkarCyan)
                    }

                    IconButton(
                        onClick = {
                            if (activeSplit != null) {
                                onMoveSplit(activeSplit.id, (activeSplit.timestampMs + 150L).coerceAtMost(totalDurationMs))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Nudge +150ms", tint = OmkarCyan)
                    }

                    IconButton(
                        onClick = { onDeleteSplit(selectedSplitId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Split", tint = OmkarCutRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Multi-Layer Timeline Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(BackgroundDark)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .width(timelineWidthDp.dp)
                    .pointerInput(totalDurationMs, timelineWidthDp) {
                        detectTapGestures { offset ->
                            val tappedRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (tappedRatio * totalDurationMs).toLong()
                            onSeek(targetMs)
                        }
                    }
                    .padding(vertical = 8.dp)
            ) {
                // Layer 1: Time Ruler
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    val seconds = (totalDurationMs / 1000).toInt() + 1
                    for (s in 0..seconds) {
                        val x = (s * 1000L) * msToDpRatio.dp.toPx()
                        drawLine(
                            color = Color(0xFF64748B),
                            start = Offset(x, 0f),
                            end = Offset(x, 14f),
                            strokeWidth = 2f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Layer 2: Audio Waveform & Speech Utterances
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(SurfaceVariantDark)
                ) {
                    // Audio waveform bars
                    val amplitudes = project.audioAmplitudes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (amplitudes.isNotEmpty()) {
                            val barW = size.width / amplitudes.size.toFloat()
                            for (i in amplitudes.indices) {
                                val amp = amplitudes[i]
                                val barH = size.height * amp
                                val bx = i * barW
                                drawRect(
                                    color = OmkarCyan.copy(alpha = 0.55f),
                                    topLeft = Offset(bx, size.height - barH),
                                    size = Size(barW.coerceAtLeast(1f), barH)
                                )
                            }
                        }
                    }

                    // Speech segments tags
                    for (speech in project.speechSegments) {
                        val startDp = (speech.startMs * msToDpRatio).dp
                        val widthDp = ((speech.endMs - speech.startMs) * msToDpRatio).dp

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(OmkarPurple.copy(alpha = 0.25f))
                                .border(1.dp, OmkarPurple.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = speech.text,
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Layer 3: Clips Strip with Keyframe Markers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    for (clip in project.clips) {
                        val startDp = (clip.startMs * msToDpRatio).dp
                        val widthDp = ((clip.endMs - clip.startMs) * msToDpRatio).dp
                        val isSelected = clip.id == selectedClipId

                        val clipBgColor = if (isSelected) {
                            OmkarGold.copy(alpha = 0.25f)
                        } else {
                            SurfaceVariantDark
                        }

                        val borderColor = if (isSelected) OmkarGold else BorderDark

                        Box(
                            modifier = Modifier
                                .offset(x = startDp)
                                .width(widthDp)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(clipBgColor)
                                .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
                                .clickable { onSelectClip(clip.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Clip 0${clip.index}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) OmkarGold else Color.White
                                    )
                                    Text(
                                        text = "${(clip.durationMs / 1000f)}s",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }

                                // Keyframe Markers (Start diamond, End diamond)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Diamond,
                                            contentDescription = "Start Keyframe",
                                            tint = OmkarGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${(clip.startKeyframe.scale * 100).toInt()}%",
                                            fontSize = 8.sp,
                                            color = OmkarGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${(clip.endKeyframe.scale * 100).toInt()}%",
                                            fontSize = 8.sp,
                                            color = OmkarCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Diamond,
                                            contentDescription = "End Keyframe",
                                            tint = OmkarCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Split boundary markers (Scissors indicators)
                    for (split in project.splitPoints) {
                        val splitX = (split.timestampMs * msToDpRatio).dp
                        val isSelectedSplit = split.id == selectedSplitId

                        Box(
                            modifier = Modifier
                                .offset(x = splitX - 10.dp)
                                .width(20.dp)
                                .fillMaxHeight()
                                .clickable { onSelectSplit(split.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelectedSplit) OmkarCutRed else OmkarCyan,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = "Split Point",
                                    tint = Color.Black,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    // Layer 4: Playhead Scrubber Needle
                    val playheadX = (currentPlayheadMs * msToDpRatio).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadX - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(OmkarGold)
                    )
                }
            }
        }
    }
}
