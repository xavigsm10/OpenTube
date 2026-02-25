package com.opentube.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opentube.R

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isFullscreen: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
    videoTitle: String = "",
    uploader: String = "",
    resizeMode: Int = 0,
    onResizeModeClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onNextVideo: () -> Unit = {},
    onPreviousVideo: () -> Unit = {},
    onMoreVideosClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimizar",
                            tint = Color.White
                        )
                    }
                    
                    if (isFullscreen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = videoTitle,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uploader,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* CC Action */ }) {
                        Icon(
                            imageVector = Icons.Rounded.ClosedCaption,
                            contentDescription = "Subtítulos",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configuración",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Video
                IconButton(
                    onClick = onPreviousVideo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Video Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Play/Pause
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Next Video
                IconButton(
                    onClick = onNextVideo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Siguiente Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(bottom = if (isFullscreen) 24.dp else 0.dp) // Raise controls in fullscreen
            ) {
                // Time and Fullscreen (Above Seekbar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(onClick = onFullscreenClick) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                            tint = Color.White
                        )
                    }
                }
                
                // Edge-to-Edge Seek Bar
                VideoProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    onSeek = onSeek
                )

                if (isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Comments Button
                            IconButton(onClick = onCommentsClick) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Comentarios",
                                    tint = Color.White
                                )
                            }
                            // Share Button
                            IconButton(onClick = onShareClick) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Más Videos Button
                        TextButton(
                            onClick = onMoreVideosClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = "Más videos",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    MarkableProgressBar(
        currentPosition = currentPosition,
        duration = duration,
        bufferedPosition = bufferedPosition,
        onSeek = onSeek,
        segments = emptyList(), // Placeholder for future Chapter/SponsorBlock data
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .height(20.dp)
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%d:%02d", minutes, remainingSeconds)
    }
}
