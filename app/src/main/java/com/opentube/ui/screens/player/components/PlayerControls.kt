package com.opentube.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.path
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
    bufferedPosition: Long, isBuffering: Boolean = false,
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
    nextVideoThumbnailUrl: String? = null,
    onNextVideo: () -> Unit = {},
    onPreviousVideo: () -> Unit = {},
    onMoreVideosClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    isLive: Boolean = false
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
                    .then(
                        if (isFullscreen) Modifier.fillMaxWidth(0.7f)
                        else Modifier.fillMaxWidth()
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Video
                IconButton(
                    onClick = onPreviousVideo,
                    modifier = Modifier
                        .size(if (isFullscreen) 64.dp else 56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Video Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 36.dp else 32.dp)
                    )
                }
                  Spacer(modifier = Modifier.width(32.dp))
                  // Play/Pause

                var playPauseScale by remember { mutableStateOf(1f) }
                LaunchedEffect(isPlaying) {
                    playPauseScale = 1.3f
                    kotlinx.coroutines.delay(100)
                    playPauseScale = 1f
                }
                
                val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = playPauseScale,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ), label = "playPauseScale"
                )

                IconButton(
                    onClick = { 
                        onPlayPauseClick()
                    },
                    modifier = Modifier
                        .size(if (isFullscreen) 86.dp else 72.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 1.dp,
                            modifier = Modifier.size(if (isFullscreen) 100.dp else 84.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier
                                .size(if (isFullscreen) 56.dp else 48.dp)
                                .scale(animatedScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Next Video
                IconButton(
                    onClick = onNextVideo,
                    modifier = Modifier
                        .size(if (isFullscreen) 64.dp else 56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Siguiente Video",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 36.dp else 32.dp)
                    )
                }
            }
            
            // Bottom Bar
            Column(
                modifier = Modifier
                    .then(
                        if (isFullscreen) Modifier.fillMaxWidth(0.85f)
                        else Modifier.fillMaxWidth()
                    )
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
                    if (isLive) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Text(
                                    text = "En vivo",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    IconButton(onClick = onFullscreenClick) {
                        // Use provided zoom/shrink PNG icons
                        val iconRes = if (isFullscreen) R.drawable.boton_achicar else R.drawable.boton_zoom
                        
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                // Edge-to-Edge Seek Bar
                VideoProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    isLive = isLive,
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
                        }
                        
                        // Más Videos Button featuring next video thumbnail
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable(onClick = onMoreVideosClick)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, end = 6.dp)
                            ) {
                                Text(
                                    text = "Más videos",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Thumbnail Circle/Rectangle
                                if (!nextVideoThumbnailUrl.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = nextVideoThumbnailUrl,
                                        contentDescription = "Siguiente Video",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    // Placeholder if no thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                            .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
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

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long, isBuffering: Boolean = false,
    isLive: Boolean = false,
    onSeek: (Long) -> Unit
) {
    MarkableProgressBar(
        currentPosition = currentPosition,
        duration = duration,
        bufferedPosition = bufferedPosition,
        onSeek = onSeek,
        isLive = isLive,
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






