package com.opentube.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animatable para controlar el progreso de 0f a 1f
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animar el progreso durante 2.5 segundos
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2500,
                easing = LinearEasing
            )
        )
        // Al terminar la animación, navegar a la siguiente pantalla
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Fondo negro como en YouTube
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de Play (similar al logo de YouTube/OpenTube)
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "OpenTube Logo",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Barra de progreso roja
            LinearProgressIndicator(
                progress = progress.value,
                modifier = Modifier
                    .width(150.dp) // Ancho fijo para que no ocupe toda la pantalla
                    .height(4.dp), // Altura delgada
                color = Color.Red, // Color de YouTube
                trackColor = Color.DarkGray, // Color de fondo de la pista
                strokeCap = StrokeCap.Round
            )
        }
    }
}
