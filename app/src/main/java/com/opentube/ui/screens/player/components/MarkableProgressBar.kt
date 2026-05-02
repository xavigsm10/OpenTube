package com.opentube.ui.screens.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TimeBarSegment(
    val startMs: Long,
    val endMs: Long,
    val color: Color
)

@Composable
fun MarkableProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    isLive: Boolean = false,
    onSeekStart: () -> Unit = {},
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    thumbRadius: Dp = 12.dp,
    activeColor: Color = Color(0xFFFF0000),
    inactiveColor: Color = Color.White.copy(alpha = 0.3f),
    bufferedColor: Color = Color.White.copy(alpha = 0.5f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0L) }
    
    // Position to display (dragging overrides actual playback)
    val displayPosition = if (isDragging) dragPosition else currentPosition
    val safeDuration = duration.coerceAtLeast(1)

    val safeOnSeek = if (isLive) { _ -> } else onSeek
    val safeOnSeekStart = if (isLive) { {} } else onSeekStart

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Touch target size
            .systemGestureExclusion()
            .pointerInput(isLive) {
                if (!isLive) {
                    detectTapGestures { offset ->
                        val progress = (offset.x / size.width).coerceIn(0f, 1f)
                        val seekTo = (progress * safeDuration.toFloat()).toLong()
                        safeOnSeek(seekTo)
                    }
                }
            }
            .pointerInput(isLive) {
                if (!isLive) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            safeOnSeekStart?.invoke()
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            dragPosition = (progress * safeDuration.toFloat()).toLong()
                        },
                        onDragEnd = {
                            isDragging = false
                            safeOnSeek(dragPosition)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragPosition = (progress * safeDuration.toFloat()).toLong()
                        }
                    )
                }
            }
    ) {
        val width = size.width
        val centerY = size.height / 2
        val barStrokeWidth = barHeight.toPx()
        
        // 1. Draw Background Track
        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = barStrokeWidth,
            cap = StrokeCap.Round
        )
        
        // 2. Draw Buffered Progress
        val bufferedProgress = (bufferedPosition.toFloat() / safeDuration).coerceIn(0f, 1f)
        if (bufferedProgress > 0) {
            drawLine(
                color = bufferedColor,
                start = Offset(0f, centerY),
                end = Offset(width * bufferedProgress, centerY),
                strokeWidth = barStrokeWidth,
                cap = StrokeCap.Round
            )
        }
        
        // 3. Draw Segments (SponsorBlock/Chapters)
        // Draw them on top of background but below active progress? 
        // Usually segments are colored parts of the track.
        // Segments functionality removed for now as it's not being passed.

        // 4. Draw Active Progress (Played)
        val activeValue = if (isLive) 1f else (displayPosition.toFloat() / safeDuration).coerceIn(0f, 1f)
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(width * activeValue, centerY),
            strokeWidth = barStrokeWidth,
            cap = StrokeCap.Round
        )
        
        // 5. Draw Thumb (Hide for live streams)
        if (!isLive) {
            drawCircle(
                color = activeColor,
                radius = if (isDragging) thumbRadius.toPx() * 1.5f else thumbRadius.toPx(),
                center = Offset(width * activeValue, centerY)
            )
        }
    }
}
