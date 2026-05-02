package com.opentube.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onPreviousVideo)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Video Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Play/Pause
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onPlayPauseClick)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        MorphingPlayPauseIcon(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Next Video
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onNextVideo)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Siguiente Video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
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
            ) {
                // Time and Fullscreen (Above Seekbar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isFullscreen) 16.dp else 2.dp,
                            vertical = if (isFullscreen) 4.dp else 0.dp
                        )
                        .offset(y = if (isFullscreen) 0.dp else 8.dp),
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
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { onFullscreenClick() }
                            .padding(4.dp)
                    ) {
                        val iconRes = if (isFullscreen) R.drawable.boton_achicar else R.drawable.boton_zoom
                        
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                
                // Edge-to-Edge Seek Bar (unless fullscreen)
                VideoProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    isLive = isLive,
                    onSeek = onSeek,
                    isFullscreen = isFullscreen,
                    modifier = if (isFullscreen) Modifier.padding(horizontal = 16.dp) else Modifier.offset(y = 10.dp)
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
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onSeek: (Long) -> Unit
) {
    MarkableProgressBar(
        currentPosition = currentPosition,
        duration = duration,
        bufferedPosition = bufferedPosition,
        onSeek = onSeek,
        isLive = isLive,
        barHeight = if (isFullscreen) 4.dp else 2.dp,
        thumbRadius = if (isFullscreen) 12.dp else 6.dp,
        modifier = modifier
            .fillMaxWidth()
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

@Composable
fun MorphingPlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier, color: Color = Color.White) {
    val transition = updateTransition(targetState = isPlaying, label = "PlayPause")
    
    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "progress"
    ) { state -> if (state) 1f else 0f }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun lerp(start: Float, stop: Float, fraction: Float): Float {
            return start + (stop - start) * fraction
        }
        
        fun lerp(start: androidx.compose.ui.geometry.Offset, stop: androidx.compose.ui.geometry.Offset, fraction: Float): androidx.compose.ui.geometry.Offset {
            return androidx.compose.ui.geometry.Offset(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))
        }

        // Play Triangle Points
        val playLTL = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f)
        val playLBL = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.8f)
        val playLTR = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.35f)
        val playLBR = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.65f)
        
        val playRTL = playLTR
        val playRBL = playLBR
        val playRTR = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.5f)
        val playRBR = playRTR

        // Pause Bars Points
        val pauseLTL = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.2f)
        val pauseLBL = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.8f)
        val pauseLTR = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.2f)
        val pauseLBR = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.8f)
        
        val pauseRTL = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.2f)
        val pauseRBL = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.8f)
        val pauseRTR = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.2f)
        val pauseRBR = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.8f)

        val currentLTL = lerp(playLTL, pauseLTL, progress)
        val currentLBL = lerp(playLBL, pauseLBL, progress)
        val currentLTR = lerp(playLTR, pauseLTR, progress)
        val currentLBR = lerp(playLBR, pauseLBR, progress)

        val currentRTL = lerp(playRTL, pauseRTL, progress)
        val currentRBL = lerp(playRBL, pauseRBL, progress)
        val currentRTR = lerp(playRTR, pauseRTR, progress)
        val currentRBR = lerp(playRBR, pauseRBR, progress)

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(currentLTL.x, currentLTL.y)
            lineTo(currentLTR.x, currentLTR.y)
            lineTo(currentLBR.x, currentLBR.y)
            lineTo(currentLBL.x, currentLBL.y)
            close()

            moveTo(currentRTL.x, currentRTL.y)
            lineTo(currentRTR.x, currentRTR.y)
            lineTo(currentRBR.x, currentRBR.y)
            lineTo(currentRBL.x, currentRBL.y)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}






