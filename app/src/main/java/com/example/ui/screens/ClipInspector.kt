package com.example.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipSegment
import com.example.model.Keyframe
import com.example.model.MotionCurve
import com.example.model.MotionMode
import com.example.model.MotionPreset
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipInspector(
    clip: ClipSegment,
    onDismiss: () -> Unit,
    onApplyPreset: (MotionPreset) -> Unit,
    onUpdateKeyframe: (keyframeId: String, scale: Float?, posX: Float?, posY: Float?, rot: Float?, curve: MotionCurve?) -> Unit,
    modifier: Modifier = Modifier,
    currentMotionMode: MotionMode = MotionMode.AUTO_MOTION,
    onSelectMotionMode: ((MotionMode) -> Unit)? = null,
    onRegenerateMotion: (() -> Unit)? = null,
    onAddKeyframeAtPlayhead: (() -> Unit)? = null,
    onAddKeyframe: ((scale: Float, posX: Float, posY: Float, curve: MotionCurve) -> Unit)? = null,
    onDeleteKeyframe: ((String) -> Unit)? = null,
    onMoveKeyframe: ((keyframeId: String, newTimestampMs: Long) -> Unit)? = null,
    onSeekToKeyframe: ((Long) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, OmkarGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("clip_inspector_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = OmkarGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clip #${clip.index} Motion Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Motion Mode Selector (REFERENCE MOTION, AUTO MOTION, CUSTOM MOTION)
            if (onSelectMotionMode != null) {
                Text(
                    text = "Motion Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmkarGold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MotionMode.values().forEach { mode ->
                        val isSelected = currentMotionMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectMotionMode(mode) },
                            label = { Text(text = mode.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OmkarGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Regenerate Motion & Add Keyframe Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRegenerateMotion != null) {
                    ElevatedButton(
                        onClick = onRegenerateMotion,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarGold
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Regenerate Motion", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (onAddKeyframeAtPlayhead != null) {
                    ElevatedButton(
                        onClick = onAddKeyframeAtPlayhead,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = OmkarCyan
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Keyframe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spoken thought display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariantDark
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = OmkarPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Detected Speech (${(clip.durationMs / 1000f)}s) • ${clip.allKeyframes().size} Keyframes",
                            fontSize = 10.sp,
                            color = OmkarPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = clip.speechText.ifBlank { "Action sequence detected." },
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Motion Preset selector
            Text(
                text = "Cinematic Motion Presets",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OmkarGold
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MotionPreset.values().forEach { preset ->
                    val isSelected = clip.motionPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyPreset(preset) },
                        label = { Text(text = preset.displayName, fontSize = 10.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmkarGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Multi-Keyframe Inspector Controls ---

            // 1. Start Keyframe Control
            KeyframeControlItem(
                title = "Start Keyframe (${String.format("%.2fs", clip.startMs / 1000f)})",
                keyframe = clip.startKeyframe,
                color = OmkarGold,
                onUpdate = onUpdateKeyframe,
                onSeekToKeyframe = onSeekToKeyframe
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Intermediate Keyframes (if any exist)
            clip.intermediateKeyframes.forEachIndexed { idx, intermediate ->
                KeyframeControlItem(
                    title = "Keyframe #${idx + 2} (${String.format("%.2fs", intermediate.timestampMs / 1000f)})",
                    keyframe = intermediate,
                    color = OmkarPurple,
                    onUpdate = onUpdateKeyframe,
                    onDelete = onDeleteKeyframe,
                    minTimestampMs = (clip.startMs + 40L).coerceAtMost(clip.endMs),
                    maxTimestampMs = (clip.endMs - 40L).coerceAtLeast(clip.startMs),
                    onMoveKeyframe = onMoveKeyframe,
                    onSeekToKeyframe = onSeekToKeyframe
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. End Keyframe Control
            KeyframeControlItem(
                title = "End Keyframe (${String.format("%.2fs", clip.endMs / 1000f)})",
                keyframe = clip.endKeyframe,
                color = OmkarCyan,
                onUpdate = onUpdateKeyframe,
                onSeekToKeyframe = onSeekToKeyframe
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add Keyframe Action
            ElevatedButton(
                onClick = {
                    if (onAddKeyframe != null) {
                        onAddKeyframe(1.15f, 0f, 0f, MotionCurve.DYNAMIC_PUNCH)
                    } else {
                        onAddKeyframeAtPlayhead?.invoke()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = OmkarPurple,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("add_keyframe_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Keyframe at Playhead", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyframeControlItem(
    title: String,
    keyframe: Keyframe,
    color: Color,
    onUpdate: (keyframeId: String, scale: Float?, posX: Float?, posY: Float?, rot: Float?, curve: MotionCurve?) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    minTimestampMs: Long? = null,
    maxTimestampMs: Long? = null,
    onMoveKeyframe: ((String, Long) -> Unit)? = null,
    onSeekToKeyframe: ((Long) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceVariantDark
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onSeekToKeyframe != null) {
                        onSeekToKeyframe?.invoke(keyframe.timestampMs)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(keyframe.scale * 100).toInt()}% • X:${keyframe.positionX.toInt()}% Y:${keyframe.positionY.toInt()}%",
                        fontSize = 10.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { onDelete(keyframe.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Keyframe", tint = OmkarCutRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Move Keyframe Time Slider (for intermediate keyframes)
            if (onMoveKeyframe != null && minTimestampMs != null && maxTimestampMs != null && maxTimestampMs > minTimestampMs) {
                Text(
                    text = "Move Keyframe Time: ${String.format("%.2fs", keyframe.timestampMs / 1000f)}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Slider(
                    value = keyframe.timestampMs.toFloat().coerceIn(minTimestampMs.toFloat(), maxTimestampMs.toFloat()),
                    onValueChange = { onMoveKeyframe(keyframe.id, it.toLong()) },
                    valueRange = minTimestampMs.toFloat()..maxTimestampMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Scale Slider
            Text(text = "Zoom Scale: ${(keyframe.scale * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray)
            Slider(
                value = keyframe.scale,
                onValueChange = { onUpdate(keyframe.id, it, null, null, null, null) },
                valueRange = 1.0f..1.50f,
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
                modifier = Modifier.height(26.dp)
            )

            // Position X & Y Sliders
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Position X: ${keyframe.positionX.toInt()}%", fontSize = 9.sp, color = Color.Gray)
                    Slider(
                        value = keyframe.positionX,
                        onValueChange = { onUpdate(keyframe.id, null, it, null, null, null) },
                        valueRange = -30f..30f,
                        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
                        modifier = Modifier.height(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Position Y: ${keyframe.positionY.toInt()}%", fontSize = 9.sp, color = Color.Gray)
                    Slider(
                        value = keyframe.positionY,
                        onValueChange = { onUpdate(keyframe.id, null, null, it, null, null) },
                        valueRange = -30f..30f,
                        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Easing Curve Selector
            Text(
                text = "Curve: ${keyframe.easing.displayName}",
                fontSize = 10.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                MotionCurve.values().forEach { curve ->
                    val isSelected = keyframe.easing == curve
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdate(keyframe.id, null, null, null, null, curve) },
                        label = { Text(text = curve.displayName, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.8f),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}
