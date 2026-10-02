package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.VideoProject
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun SmartModePanel(
    project: VideoProject,
    onSeek: (Long) -> Unit,
    onDeleteSplit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Speech Boundaries", "Split Points", "Keyframe Matrix")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, OmkarCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("smart_mode_panel"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = OmkarCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMART MODE • Advanced AI & Video Diagnostics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceVariantDark,
                contentColor = OmkarGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = OmkarGold
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Speech boundaries
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Detected Spoken Thoughts (${project.speechSegments.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmkarPurple
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        project.speechSegments.forEachIndexed { idx, segment ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSeek(segment.startMs) },
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Thought #${idx + 1} (${segment.words.size} words)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OmkarGold
                                        )
                                        Text(
                                            text = "${segment.startMs}ms - ${segment.endMs}ms (Pause: ${segment.pauseDurationAfterMs}ms)",
                                            fontSize = 10.sp,
                                            color = OmkarCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = segment.text,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Split points
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Frame-Accurate Split Boundaries (${project.splitPoints.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmkarCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (project.splitPoints.isEmpty()) {
                            Text(text = "No split points yet.", color = Color.Gray, fontSize = 12.sp)
                        }

                        project.splitPoints.forEachIndexed { idx, split ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Split #${idx + 1} • Frame ${split.frameIndex}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = OmkarCyan
                                        )
                                        Text(
                                            text = "${split.timestampMs}ms • ${split.reason}",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSplit(split.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OmkarCutRed)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Keyframe Matrix
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Clip Keyframes & Curves Matrix",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmkarGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        project.clips.forEach { clip ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariantDark
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Clip #${clip.index} • ${clip.motionPreset.displayName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OmkarGold
                                        )
                                        Text(
                                            text = "${clip.startMs}ms - ${clip.endMs}ms",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Start: ${(clip.startKeyframe.scale * 100).toInt()}% • Easing: ${clip.startKeyframe.easing.displayName}",
                                            fontSize = 10.sp,
                                            color = OmkarCyan
                                        )
                                        Text(
                                            text = "End: ${(clip.endKeyframe.scale * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            color = OmkarPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
