package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.ExportResolution
import com.example.model.ExportSettings
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun ExportDialog(
    initialSettings: ExportSettings,
    onDismiss: () -> Unit,
    onConfirmExport: (ExportSettings) -> Unit
) {
    var selectedResolution by remember { mutableStateOf(initialSettings.resolution) }
    var bitrateMbps by remember { mutableIntStateOf(initialSettings.bitrateMbps) }
    var includeSubtitles by remember { mutableStateOf(initialSettings.includeSubtitles) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("export_dialog"),
        containerColor = SurfaceDark,
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = OmkarGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Export Processed MP4 Video", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Resolution & Aspect Ratio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmkarGold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExportResolution.values().forEach { res ->
                    val isSelected = selectedResolution == res
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) OmkarGold else BorderDark,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedResolution = res },
                        color = if (isSelected) OmkarGold.copy(alpha = 0.15f) else SurfaceVariantDark
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = res.displayName,
                                fontSize = 12.sp,
                                color = if (isSelected) OmkarGold else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = OmkarGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bitrate control
                Text(
                    text = "Video Encoding Bitrate: ${bitrateMbps} Mbps (H.264)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Slider(
                    value = bitrateMbps.toFloat(),
                    onValueChange = { bitrateMbps = it.toInt() },
                    valueRange = 4f..20f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = OmkarCyan,
                        activeTrackColor = OmkarCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Burn-in Subtitles toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Burn-in Speech Captions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text(text = "Renders spoken thought text into the MP4", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = includeSubtitles,
                        onCheckedChange = { includeSubtitles = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OmkarGold,
                            checkedTrackColor = OmkarGold.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundDark
                ) {
                    Text(
                        text = "Format: MP4 Container • H.264 AVC Video • AAC 44.1kHz Audio • Hardware Accelerated",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmExport(
                        ExportSettings(
                            resolution = selectedResolution,
                            bitrateMbps = bitrateMbps,
                            fps = 30,
                            includeSubtitles = includeSubtitles
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmkarGold, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Export MP4", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.White)
            }
        }
    )
}
