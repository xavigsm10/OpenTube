package com.opentube.ui.screens.player.components

import android.app.Activity
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class GestureAction {
    NONE, VOLUME, BRIGHTNESS, SEEK_FORWARD, SEEK_BACKWARD
}

@Composable
fun PlayerGestureOverlay(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Int) -> Unit, // +10 or -10
    onVolumeChange: (Float) -> Unit, // Delta
    onBrightnessChange: (Float) -> Unit, // Delta
    content: @Composable () -> Unit
) {
    // We wrap content to capture touches over it
    Box(modifier = modifier.fillMaxSize()) {
        content() // The video surface
        
        // Touch Handler Layer
        GestureHandler(
            onSingleTap = onSingleTap,
            onDoubleTapSeek = onDoubleTapSeek,
            onVolumeChange = onVolumeChange,
            onBrightnessChange = onBrightnessChange
        )
    }
}

@Composable
private fun GestureHandler(
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    var activeAction by remember { mutableStateOf(GestureAction.NONE) }
    var actionValue by remember { mutableFloatStateOf(0f) } // 0..1 for vol/bright
    var showIndicator by remember { mutableStateOf(false) }
    
    // For Seek Animation
    var showSeekAnim by remember { mutableStateOf(false) }
    var seekForward by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    
    // Initial Values
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    // Brightness/Volume Logic
    // We maintain local state to show the UI, and push changes to system
    
    LaunchedEffect(showIndicator) {
        if (showIndicator) {
            kotlinx.coroutines.delay(1500)
            showIndicator = false
            activeAction = GestureAction.NONE
        }
    }
    
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
                            activeAction = GestureAction.SEEK_FORWARD
                        } else {
                            seekForward = false
                            onDoubleTapSeek(-10)
                            activeAction = GestureAction.SEEK_BACKWARD
                        }
                        showSeekAnim = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Determine zone
                        val isRight = offset.x > size.width / 2
                        activeAction = if (isRight) GestureAction.VOLUME else GestureAction.BRIGHTNESS
                        showIndicator = true
                        
                        // Initialize current values to avoid jumping
                        if (activeAction == GestureAction.BRIGHTNESS) {
                            val activity = context as? Activity
                            val brightness = activity?.window?.attributes?.screenBrightness ?: -1f
                            actionValue = if (brightness < 0) 0.5f else brightness
                        } else {
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            actionValue = currentVol.toFloat() / maxVolume
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // dragAmount is + when down (decreasing value), - when up (increasing value)
                        val delta = -dragAmount / 500f // Sensitivity
                        actionValue = (actionValue + delta).coerceIn(0f, 1f)
                        
                        if (activeAction == GestureAction.BRIGHTNESS) {
                            onBrightnessChange(actionValue)
                        } else {
                            // Map 0..1 to maxVolume
                            val newVol = (actionValue * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            onVolumeChange(actionValue)
                        }
                    }
                )
            }
    ) {
        // Overlays
        
        // 1. Volume/Brightness Indicator (Centered)
        AnimatedVisibility(
            visible = showIndicator && (activeAction == GestureAction.VOLUME || activeAction == GestureAction.BRIGHTNESS),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureIndicator(
                icon = if (activeAction == GestureAction.BRIGHTNESS) Icons.Default.BrightnessMedium else Icons.Default.VolumeUp,
                value = actionValue,
                text = if (activeAction == GestureAction.BRIGHTNESS) "Brillo" else "Volumen" // Should use R.string
            )
        }
        
        // 2. Double Tap Seek Animation (Left or Right)
        if (showSeekAnim) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .align(if (seekForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(
                        Color.White.copy(alpha = 0.1f), 
                        RoundedCornerShape(topStart = if (seekForward) 100.dp else 0.dp, bottomStart = if (seekForward) 100.dp else 0.dp, topEnd = if (!seekForward) 100.dp else 0.dp, bottomEnd = if (!seekForward) 100.dp else 0.dp)
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

@Composable
fun GestureIndicator(
    icon: ImageVector,
    value: Float,
    text: String
) {
    Column(
        modifier = Modifier
            .size(120.dp)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
    }
}
