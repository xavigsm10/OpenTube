package com.opentube.ui.screens.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    segments: List<TimeBarSegment> = emptyList(), // For SponsorBlock, Chapters
    onSeek: (Long) -> Unit,
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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Touch target size
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val progress = (offset.x / size.width).coerceIn(0f, 1f)
                    val seekTo = (progress * safeDuration).toLong()
                    onSeek(seekTo)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onSeekStart()
                        val progress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragPosition = (progress * safeDuration).toLong()
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragPosition)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragPosition = (progress * safeDuration).toLong()
                    }
                )
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
        segments.forEach { segment ->
            val startRatio = (segment.startMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            val endRatio = (segment.endMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            
            if (endRatio > startRatio) {
                drawLine(
                    color = segment.color,
                    start = Offset(width * startRatio, centerY),
                    end = Offset(width * endRatio, centerY),
                    strokeWidth = barStrokeWidth,
                    cap = StrokeCap.Butt // Precise edges for segments
                )
            }
        }

        // 4. Draw Active Progress (Played)
        val activeProgress = (displayPosition.toFloat() / safeDuration).coerceIn(0f, 1f)
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(width * activeProgress, centerY),
            strokeWidth = barStrokeWidth,
            cap = StrokeCap.Round
        )
        
        // 5. Draw Thumb
        drawCircle(
            color = activeColor,
            radius = if (isDragging) thumbRadius.toPx() * 1.5f else thumbRadius.toPx(),
            center = Offset(width * activeProgress, centerY)
        )
    }
}
