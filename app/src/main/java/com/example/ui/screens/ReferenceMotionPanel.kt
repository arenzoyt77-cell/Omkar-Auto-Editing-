package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlueprintDebugSummary
import com.example.model.MotionMode
import com.example.model.VideoMetadata
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarGreen
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceMotionPanel(
    summary: BlueprintDebugSummary,
    referenceMetadata: VideoMetadata?,
    currentMotionMode: MotionMode,
    isAnalyzingReference: Boolean,
    onSelectReferenceVideo: () -> Unit,
    onResetToDefaultReference: () -> Unit,
    onSetMotionMode: (MotionMode) -> Unit,
    onReapplyMotion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp)),
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
                    Surface(
                        shape = CircleShape,
                        color = OmkarCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = null,
                            tint = OmkarCyan,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Reference Motion Blueprint",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (referenceMetadata != null) "Custom Target • Cached Blueprint" else "Authoritative Target (YouCut)",
                            color = OmkarCyan,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OmkarGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmkarGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cached, contentDescription = null, tint = OmkarGreen, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Cached", color = OmkarGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reference Source Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or Movie Icon
                val thumbFile = referenceMetadata?.thumbnailPath?.let { File(it) }
                val bitmap = remember(thumbFile?.absolutePath) {
                    if (thumbFile != null && thumbFile.exists()) {
                        BitmapFactory.decodeFile(thumbFile.absolutePath)
                    } else null
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Reference Thumbnail",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OmkarCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = OmkarCyan,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.referenceName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Duration: ${(summary.referenceDurationMs / 1000f)}s • ${summary.totalEventsCount} Events",
                        color = OmkarGold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Zoom Range: ${(summary.minScale * 100).toInt()}% → ${(summary.maxScale * 100).toInt()}%",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector: AUTO MOTION vs REFERENCE MOTION vs CUSTOM MOTION
            Text(
                text = "Active Motion Engine Mode",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MotionMode.values().forEach { mode ->
                    val isSelected = currentMotionMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetMotionMode(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    MotionMode.AUTO_MOTION -> "AUTO ACTION"
                                    MotionMode.REFERENCE_MOTION -> "REFERENCE"
                                    MotionMode.CUSTOM_MOTION -> "CUSTOM"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmkarGold,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Import Custom Reference / Revert to Default / Re-Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectReferenceVideo,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmkarCyan),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAnalyzingReference) "Analyzing..." else "Import Reference",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (referenceMetadata != null) {
                    OutlinedButton(
                        onClick = onResetToDefaultReference,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        modifier = Modifier.weight(0.8f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Default", fontSize = 10.sp)
                    }
                }

                Button(
                    onClick = onReapplyMotion,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmkarGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Apply Motion", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
