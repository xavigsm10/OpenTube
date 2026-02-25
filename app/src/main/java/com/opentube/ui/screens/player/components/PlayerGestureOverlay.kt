package com.opentube.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
    
    var showSeekAnim by remember { mutableStateOf(false) }
    var seekForward by remember { mutableStateOf(true) }
    var seekCount by remember { mutableIntStateOf(0) } // To trigger animation restart

    val context = LocalContext.current
    
    // Animation states
    var animTrigger by remember { mutableStateOf(false) }
    val slideOffset by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "slideAnimation"
    )

    LaunchedEffect(seekCount) {
        if (showSeekAnim) {
            animTrigger = false // Reset
            kotlinx.coroutines.delay(50) // Small delay to allow reset to register
            animTrigger = true // Start animation
            
            kotlinx.coroutines.delay(650) // Wait for animation to finish
            showSeekAnim = false
            animTrigger = false
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
                        seekCount++
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
                    .align(if (seekForward) Alignment.CenterEnd else Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                // Background gradient removed as per image 2, just showing the +10 and arrow
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!seekForward) {
                            // Left Arrow sliding left
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .offset { IntOffset(x = -(slideOffset * 30f).roundToInt(), y = 0) }
                                    .alpha(1f - slideOffset)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Text(
                            text = if (seekForward) "+10" else "-10",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        if (seekForward) {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Right Arrow sliding right
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .offset { IntOffset(x = (slideOffset * 30f).roundToInt(), y = 0) }
                                    .alpha(1f - slideOffset)
                            )
                        }
                    }
                }
            }
        }
    }
}
