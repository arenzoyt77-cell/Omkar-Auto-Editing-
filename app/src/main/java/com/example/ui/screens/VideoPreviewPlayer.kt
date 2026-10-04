package com.example.ui.screens

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.TextOverlay
import com.example.service.PlaybackState
import com.example.service.VideoPreviewEngine
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.OmkarPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun VideoPreviewPlayer(
    previewEngine: VideoPreviewEngine,
    playbackState: PlaybackState,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
    textOverlays: List<TextOverlay> = emptyList()
) {
    VideoPreviewPlayer(
        playbackState = playbackState,
        totalDurationMs = totalDurationMs,
        onAttachSurface = { previewEngine.attachSurface(it) },
        onDetachSurface = { previewEngine.detachSurface() },
        onTogglePlayPause = { previewEngine.togglePlayPause() },
        onSeek = { previewEngine.seekTo(it) },
        onRestart = { previewEngine.seekTo(0L) },
        modifier = modifier,
        textOverlays = textOverlays
    )
}

@Composable
fun VideoPreviewPlayer(
    playbackState: PlaybackState,
    totalDurationMs: Long,
    onAttachSurface: (Surface) -> Unit,
    onDetachSurface: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null,
    isMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    textOverlays: List<TextOverlay> = emptyList()
) {
    val transform = playbackState.transform
    var isFillMode by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundDark)
            .testTag("video_preview_container")
    ) {
        // Video Viewport with dynamic hardware-accelerated Keyframe Graphics Layer
        val ratio = if (isFillMode) 9f / 16f else 16f / 9f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isFullscreen) Modifier.aspectRatio(16f / 9f) else Modifier.height(340.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = transform.scale * (if (isFillMode) 1.25f else 1.0f),
                        scaleY = transform.scale * (if (isFillMode) 1.25f else 1.0f),
                        translationX = (transform.positionX / 100f) * 400f,
                        translationY = (transform.positionY / 100f) * 250f,
                        rotationZ = transform.rotation
                    ),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                onAttachSurface(Surface(st))
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                onDetachSurface()
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                }
            )

            // Dynamic Subtitle Banner
            if (playbackState.currentSubtitle.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC080A10))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = playbackState.currentSubtitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time Text Overlays Active at Current Timestamp
            val activeOverlays = textOverlays.filter { it.contains(playbackState.currentPositionMs) }
            activeOverlays.forEach { overlay ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = ((overlay.positionY / 100f) * 180f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(overlay.backgroundColor))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = overlay.text,
                        color = Color(overlay.textColor),
                        fontSize = overlay.fontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time Keyframe HUD Badge (Shows live interpolated scale & easing)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark.copy(alpha = 0.85f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = "Keyframe HUD",
                        tint = OmkarGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Zoom: ${(transform.scale * 100).toInt()}% • X:${String.format("%+.1f%%", transform.positionX)} Y:${String.format("%+.1f%%", transform.positionY)}",
                        color = OmkarGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Top Right Controls: Fit/Fill, Mute, Fullscreen
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onToggleMute != null) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceDark.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp).clickable { onToggleMute() }
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = if (isMuted) Color.Gray else Color.White,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = SurfaceDark.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp).clickable { isFillMode = !isFillMode }
                ) {
                    Icon(
                        imageVector = if (isFillMode) Icons.Default.FitScreen else Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = if (isFillMode) OmkarGold else Color.White,
                        modifier = Modifier.padding(7.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SurfaceDark.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp).clickable { isFullscreen = !isFullscreen }
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }
        }

        // Playback Bottom Control Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = SurfaceDark.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Seek Bar Slider
                if (onSeek != null && totalDurationMs > 0) {
                    Slider(
                        value = playbackState.currentPositionMs.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..totalDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = OmkarGold,
                            activeTrackColor = OmkarGold,
                            inactiveTrackColor = SurfaceVariantDark
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = OmkarGold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        val curSec = playbackState.currentPositionMs / 1000
                        val curMs = (playbackState.currentPositionMs % 1000) / 10
                        val totSec = totalDurationMs / 1000
                        val totMs = (totalDurationMs % 1000) / 10

                        Text(
                            text = String.format("%02d:%02d.%02d / %02d:%02d.%02d", curSec / 60, curSec % 60, curMs, totSec / 60, totSec % 60, totMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        playbackState.activeClip?.let { clip ->
                            Text(
                                text = "Clip #${clip.index} • ${clip.motionPreset.displayName}",
                                color = OmkarPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = onRestart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Restart",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
