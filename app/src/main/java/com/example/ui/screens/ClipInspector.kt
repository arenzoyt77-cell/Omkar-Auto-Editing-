package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.MotionCurve
import com.example.model.MotionPreset
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                        text = "Clip #${clip.index} Keyframe Inspector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
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
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = OmkarPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Detected Spoken Thought (${(clip.durationMs / 1000f)}s)",
                            fontSize = 11.sp,
                            color = OmkarPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = clip.speechText.ifBlank { "Thought boundary detected naturally." },
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Motion Preset selector
            Text(
                text = "Cinematic Motion Presets",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OmkarGold
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MotionPreset.values().forEach { preset ->
                    val isSelected = clip.motionPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyPreset(preset) },
                        label = { Text(text = preset.displayName, fontSize = 11.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmkarGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start Keyframe Control
            Text(
                text = "Start Keyframe Zoom: ${(clip.startKeyframe.scale * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Slider(
                value = clip.startKeyframe.scale,
                onValueChange = { newScale ->
                    onUpdateKeyframe(clip.startKeyframe.id, newScale, null, null, null, null)
                },
                valueRange = 1.0f..1.50f,
                colors = SliderDefaults.colors(
                    thumbColor = OmkarGold,
                    activeTrackColor = OmkarGold
                )
            )

            // End Keyframe Control
            Text(
                text = "End Keyframe Zoom: ${(clip.endKeyframe.scale * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Slider(
                value = clip.endKeyframe.scale,
                onValueChange = { newScale ->
                    onUpdateKeyframe(clip.endKeyframe.id, newScale, null, null, null, null)
                },
                valueRange = 1.0f..1.50f,
                colors = SliderDefaults.colors(
                    thumbColor = OmkarCyan,
                    activeTrackColor = OmkarCyan
                )
            )

            // Motion Easing Curve Selector
            Text(
                text = "Interpolation Curve: ${clip.startKeyframe.easing.displayName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MotionCurve.values().forEach { curve ->
                    val isSelected = clip.startKeyframe.easing == curve
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onUpdateKeyframe(clip.startKeyframe.id, null, null, null, null, curve)
                            onUpdateKeyframe(clip.endKeyframe.id, null, null, null, null, curve)
                        },
                        label = { Text(text = curve.displayName, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmkarPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}
