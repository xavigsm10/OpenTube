package com.opentube.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PlayerGestureOverlay(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Int) -> Unit, // +10 or -10
    onSwipeDown: () -> Unit,
    content: @Composable () -> Unit
) {
    // We wrap content to capture touches over it
    Box(modifier = modifier.fillMaxSize()) {
        content() // The video surface
        
        // Touch Handler Layer
        GestureHandler(
            onSingleTap = onSingleTap,
            onDoubleTapSeek = onDoubleTapSeek,
            onSwipeDown = onSwipeDown
        )
    }
}

@Composable
private fun GestureHandler(
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Int) -> Unit,
    onSwipeDown: () -> Unit
) {
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    
    // For Seek Animation
    var showSeekAnim by remember { mutableStateOf(false) }
    var seekForward by remember { mutableStateOf(true) }

    val context = LocalContext.current
    
    LaunchedEffect(showSeekAnim) {
        if (showSeekAnim) {
            kotlinx.coroutines.delay(600)
            showSeekAnim = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val isRight = offset.x > size.width / 2
                        if (isRight) {
                            seekForward = true
                            onDoubleTapSeek(10)
                        } else {
                            seekForward = false
                            onDoubleTapSeek(-10)
                        }
                        showSeekAnim = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { 
                        accumulatedDragY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // dragAmount > 0 is down
                        accumulatedDragY += dragAmount
                        
                        // Threshold for swipe down (e.g., 200px)
                        if (accumulatedDragY > 200f) {
                            onSwipeDown()
                            accumulatedDragY = 0f // Reset to prevent multiple calls
                        }
                    },
                    onDragEnd = {
                        accumulatedDragY = 0f
                    }
                )
            }
    ) {
        // Overlays
        
        // 1. Double Tap Seek Animation (Left or Right)
        if (showSeekAnim) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .align(if (seekForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(
                        Color.White.copy(alpha = 0.1f), 
                        RoundedCornerShape(
                            topStart = if (seekForward) 100.dp else 0.dp, 
                            bottomStart = if (seekForward) 100.dp else 0.dp, 
                            topEnd = if (!seekForward) 100.dp else 0.dp, 
                            bottomEnd = if (!seekForward) 100.dp else 0.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (seekForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (seekForward) "+10s" else "-10s",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
