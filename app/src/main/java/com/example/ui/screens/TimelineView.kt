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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
    modifier: Modifier = Modifier,
    onDeleteClip: ((String) -> Unit)? = null,
    onDuplicateClip: ((String) -> Unit)? = null,
    onToggleClipMute: ((String) -> Unit)? = null,
    onOpenTextOverlayDialog: (() -> Unit)? = null
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    val totalDurationMs = project.effectiveDurationMs.coerceAtLeast(1000L)
    val baseTimelineWidthDp = 380f
    val timelineWidthDp = (baseTimelineWidthDp * zoomScale).coerceAtLeast(baseTimelineWidthDp)
    val msToDpRatio = timelineWidthDp / totalDurationMs.toFloat()

    val scrollState = rememberScrollState()
    val activeClip = project.clips.firstOrNull { it.id == selectedClipId }

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
                    text = "Interactive Timeline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${project.clips.size} Clips)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Quick actions & Timeline Zoom
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.LightGray,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.LightGray,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Split & Clip Management Sub-Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Split at playhead
                ElevatedButton(
                    onClick = onAddSplitAtPlayhead,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = OmkarGold
                    ),
                    modifier = Modifier.testTag("timeline_split_button")
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Split", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Add Text
                if (onOpenTextOverlayDialog != null) {
                    FilledTonalButton(
                        onClick = onOpenTextOverlayDialog,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarCyan
                        )
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Clip Quick Actions (Delete, Duplicate, Mute)
            if (activeClip != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onToggleClipMute != null) {
                        IconButton(
                            onClick = { onToggleClipMute(activeClip.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (activeClip.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute Clip",
                                tint = if (activeClip.isMuted) OmkarCutRed else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onDuplicateClip != null) {
                        IconButton(
                            onClick = { onDuplicateClip(activeClip.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate Clip",
                                tint = OmkarCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onDeleteClip != null && project.clips.size > 1) {
                        IconButton(
                            onClick = { onDeleteClip(activeClip.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Clip",
                                tint = OmkarCutRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                    .pointerInput(totalDurationMs, timelineWidthDp) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (dragRatio * totalDurationMs).toLong()
                            onSeek(targetMs)
                        }
                    }
                    .padding(vertical = 6.dp)
            ) {
                // Layer 1: Time Ruler
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    val seconds = (totalDurationMs / 1000).toInt() + 1
                    for (s in 0..seconds) {
                        val x = (s * 1000L) * msToDpRatio.dp.toPx()
                        drawLine(
                            color = Color(0xFF64748B),
                            start = Offset(x, 0f),
                            end = Offset(x, 12f),
                            strokeWidth = 2f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Layer 2: Audio Waveform & Speech Utterances
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
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

                // Layer 3: Text Overlays Track (if any text overlays exist)
                if (project.textOverlays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(Color(0xFF131722))
                    ) {
                        for (overlay in project.textOverlays) {
                            val startDp = (overlay.startMs * msToDpRatio).dp
                            val widthDp = ((overlay.endMs - overlay.startMs) * msToDpRatio).dp.coerceAtLeast(24.dp)
                            Box(
                                modifier = Modifier
                                    .offset(x = startDp)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(OmkarGold.copy(alpha = 0.35f))
                                    .border(1.dp, OmkarGold, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = overlay.text,
                                    color = OmkarGold,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Layer 4: Clips Strip with Keyframe Markers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Clip 0${clip.index}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) OmkarGold else Color.White
                                        )
                                        if (clip.isMuted) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.VolumeMute,
                                                contentDescription = "Muted",
                                                tint = OmkarCutRed,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
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
                                            modifier = Modifier.size(11.dp)
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
                                            modifier = Modifier.size(11.dp)
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

                    // Layer 5: Playhead Scrubber Line
                    val playheadX = (currentPlayheadMs * msToDpRatio).dp
                    Box(
                        modifier = Modifier
                            .offset(x = playheadX - 1.dp)
                            .width(2.5.dp)
                            .fillMaxHeight()
                            .background(OmkarGold)
                    )
                }
            }
        }
    }
}
