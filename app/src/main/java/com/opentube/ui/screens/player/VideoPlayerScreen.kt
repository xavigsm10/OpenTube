package com.opentube.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.opentube.ui.screens.player.components.MetrolistMusicPlayer
import androidx.media3.ui.PlayerView
import com.opentube.ui.screens.player.components.PlayerControls
import com.opentube.ui.screens.player.components.PlayerSettingsSheet
import com.opentube.ui.screens.player.components.PlayerGestureOverlay
import com.opentube.ui.components.VideoCard
import com.opentube.helpers.DashHelper
import com.opentube.util.PictureInPictureUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    onChannelClick: ((String) -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null,
    onMinimize: ((title: String, channel: String, thumbnailUrl: String, isPlaying: Boolean, player: androidx.media3.exoplayer.ExoPlayer?) -> Unit)? = null,
    existingPlayer: androidx.media3.exoplayer.ExoPlayer? = null,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    android.util.Log.d("VideoPlayerScreen", "=== VideoPlayerScreen COMPOSABLE STARTED for videoId: $videoId ===")
    
    val uiState by viewModel.uiState.collectAsState()
    val showSettingsSheet by viewModel.showSettingsSheet.collectAsState()
    val context = LocalContext.current
    
    // Use shared player from ViewModel
    val exoPlayer = viewModel.player
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isMinimizing by remember { mutableStateOf(false) }
    var showMoreVideos by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    
    // Manage fullscreen
    val activity = context as? Activity
    val isFullscreen = (uiState as? VideoPlayerUiState.Success)?.playerSettings?.isFullscreen ?: false
    
    // Gestionar orientación y barras del sistema según modo pantalla completa
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        // Ocultar/mostrar barras del sistema en pantalla completa
        activity?.window?.let { window ->
            // SIEMPRE usar false para que el contenido se dibuje detrás de las barras
            // El padding del Scaffold en OpenTubeNavHost se encarga de dejar el espacio en portrait
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController?.apply {
                if (isFullscreen) {
                    hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                }
            }
        }
    }
    
    // Ocultar controles automáticamente después de 3 segundos
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // Actualizar posición de reproducción cada segundo
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            exoPlayer?.let { player ->
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0)
                bufferedPosition = player.bufferedPosition
                isPlaying = player.isPlaying
                viewModel.updatePlaybackPosition(currentPosition)
            }
            delay(100)
        }
    }
    
    // Handle seamless transition from mini player - ensure playback continues
    LaunchedEffect(existingPlayer) {
        if (existingPlayer != null) {
            android.util.Log.d("VideoPlayerScreen", "Transition from mini player detected - ensuring playback continues")
            // Small delay to let the UI settle
            delay(100)
            exoPlayer?.let { player ->
                // If player was playing before, make sure it continues
                if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                    if (!player.isPlaying) {
                        android.util.Log.d("VideoPlayerScreen", "Resuming playback after mini player transition")
                        player.play()
                    }
                }
            }
        }
    }
    
    // Handle back button - Minimize video player
    BackHandler(enabled = !isFullscreen) {
        val state = uiState
        if (state is VideoPlayerUiState.Success && onMinimize != null) {
            // Log para debug
            android.util.Log.d("VideoPlayerScreen", "Minimizando - Player: ${exoPlayer != null}, isPlaying: $isPlaying")
            // Marcar que estamos minimizando para NO liberar el player
            isMinimizing = true
            // Minimizar el reproductor
            onMinimize(
                state.videoDetails.title,
                state.videoDetails.uploader,
                state.videoDetails.thumbnailUrl,
                isPlaying,
                exoPlayer
            )
            // Navegar hacia atrás DESPUÉS de minimizar
            onNavigateBack()
        } else {
            onNavigateBack()
        }
    }
    
    // Handle fullscreen back button
    BackHandler(enabled = isFullscreen) {
        viewModel.toggleFullscreen()
    }
    
    when (val state = uiState) {
        is VideoPlayerUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        is VideoPlayerUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                }
            }
        }
        
        is VideoPlayerUiState.Success -> {
            val videoDetails = state.videoDetails
            val playerSettings = state.playerSettings
            
            // Inicializar ExoPlayer
            DisposableEffect(videoId) {
                android.util.Log.d("VideoPlayerScreen", "DisposableEffect started for videoId: $videoId")
                
                // Use shared player
                val player = exoPlayer
                
                player.apply {
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
                
                // Solo continuar si el player se creó correctamente
                if (player != null) {
                    android.util.Log.d("VideoPlayerScreen", "Player inicializado correctamente")
                    
                    // Verificar si ya estamos reproduciendo este video (Continuidad)
                    // Comprobamos si hay un MediaItem y si el ID coincide (o si simplemente ya está reproduciendo algo y asumimos que es correcto si venimos del miniplayer)
                    val isSameVideo = viewModel.isCurrentVideo(videoId)
                    val isFromMiniPlayer = existingPlayer != null || isSameVideo
                    
                    if (!isFromMiniPlayer) {
                        android.util.Log.d("VideoPlayerScreen", "Preparando nuevo stream")
                        
                        try {
                            // Detener el player actual antes de cargar nuevo contenido
                            player.stop()
                            player.clearMediaItems()
                            
                            var streamLoaded = false
                            
                            // PARA EN VIVOS: Intentar HLS primero, si no hay HLS intentar Progressive
                            if (videoDetails.liveNow) {
                                android.util.Log.d("VideoPlayerScreen", "🔴 LIVE STREAM detected")
                                
                                // Intentar HLS primero
                                if (!videoDetails.hlsUrl.isNullOrEmpty()) {
                                    android.util.Log.d("VideoPlayerScreen", "Trying HLS for LIVE stream: ${videoDetails.hlsUrl}")
                                    try {
                                        val mediaItem = androidx.media3.common.MediaItem.fromUri(videoDetails.hlsUrl)
                                        player.setMediaItem(mediaItem)
                                        player.prepare()
                                        player.playWhenReady = true
                                        streamLoaded = true
                                        android.util.Log.d("VideoPlayerScreen", "✅ HLS LIVE loaded")
                                    } catch (e: Exception) {
                                        android.util.Log.e("VideoPlayerScreen", "❌ HLS LIVE failed", e)
                                    }
                                } else {
                                    android.util.Log.d("VideoPlayerScreen", "⚠️ HLS URL is empty for LIVE stream")
                                }
                                
                                // Si HLS falla, intentar Progressive para en vivos
                                if (!streamLoaded && videoDetails.videoStreams.isNotEmpty()) {
                                    android.util.Log.d("VideoPlayerScreen", "Fallback: Trying Progressive for LIVE")
                                    try {
                                        val bestProgressive = videoDetails.videoStreams
                                            .filter { !it.videoOnly }
                                            .maxByOrNull { it.height ?: 0 }
                                        
                                        if (bestProgressive != null) {
                                            android.util.Log.d("VideoPlayerScreen", "Loading LIVE Progressive ${bestProgressive.height}p")
                                            val mediaItem = androidx.media3.common.MediaItem.fromUri(bestProgressive.url)
                                            player.setMediaItem(mediaItem)
                                            player.prepare()
                                            player.playWhenReady = true
                                            streamLoaded = true
                                            android.util.Log.d("VideoPlayerScreen", "✅ LIVE Progressive loaded")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("VideoPlayerScreen", "❌ LIVE Progressive failed", e)
                                    }
                                }
                            }
                            
                            // OPCIÓN 1: Progressive stream (video+audio juntos) - MÁS CONFIABLE (para videos normales)
                            if (!streamLoaded && videoDetails.videoStreams.isNotEmpty()) {
                                android.util.Log.d("VideoPlayerScreen", "Trying Progressive stream first")
                                try {
                                    // Buscar el mejor stream Progressive (con audio incluido)
                                    val bestProgressive = videoDetails.videoStreams
                                        .filter { !it.videoOnly && (it.height ?: 0) >= 360 } // Con audio y mínimo 360p
                                        .maxByOrNull { it.height ?: 0 }
                                    
                                    if (bestProgressive != null) {
                                        android.util.Log.d("VideoPlayerScreen", "Loading progressive ${bestProgressive.height}p")
                                        val mediaItem = androidx.media3.common.MediaItem.fromUri(bestProgressive.url)
                                        player.setMediaItem(mediaItem)
                                        player.prepare()
                                        player.playWhenReady = true
                                        streamLoaded = true
                                        android.util.Log.d("VideoPlayerScreen", "✅ Progressive loaded successfully")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoPlayerScreen", "❌ Progressive failed", e)
                                }
                            }
                            
                            // OPCIÓN 2: HLS (calidad adaptativa) - Si Progressive falla
                            if (!videoDetails.hlsUrl.isNullOrEmpty() && !streamLoaded) {
                                android.util.Log.d("VideoPlayerScreen", "Trying HLS stream")
                                try {
                                    val mediaItem = androidx.media3.common.MediaItem.fromUri(videoDetails.hlsUrl)
                                    player.setMediaItem(mediaItem)
                                    player.prepare()
                                    player.playWhenReady = true
                                    streamLoaded = true
                                    android.util.Log.d("VideoPlayerScreen", "✅ HLS loaded")
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoPlayerScreen", "❌ HLS failed", e)
                                }
                            }
                            
                            // OPCIÓN 3: DASH (video+audio separado, mejor calidad fija)
                            if (!streamLoaded && playerSettings.selectedQuality != null) {
                                android.util.Log.d("VideoPlayerScreen", "Trying DASH streams")
                                try {
                                    val videoStream = playerSettings.selectedQuality
                                    val audioStream = playerSettings.selectedAudioTrack
                                    
                                    if (videoStream != null && videoStream.url.isNotEmpty()) {
                                        if (videoStream.videoOnly && audioStream != null && audioStream.url.isNotEmpty()) {
                                            // Video + Audio separado (DASH)
                                            android.util.Log.d("VideoPlayerScreen", "Merging video ${videoStream.height}p + audio ${audioStream.bitrate}")
                                            
                                            val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                            
                                            val videoSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                                                .createMediaSource(androidx.media3.common.MediaItem.fromUri(videoStream.url))
                                            
                                            val audioSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                                                .createMediaSource(androidx.media3.common.MediaItem.fromUri(audioStream.url))
                                            
                                            val mergedSource = androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
                                            
                                            player.setMediaSource(mergedSource)
                                            player.prepare()
                                            player.playWhenReady = true
                                            streamLoaded = true
                                            android.util.Log.d("VideoPlayerScreen", "✅ DASH loaded")
                                        } else if (!videoStream.videoOnly) {
                                            // Stream con audio integrado
                                            android.util.Log.d("VideoPlayerScreen", "Using progressive stream ${videoStream.height}p")
                                            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(videoStream.url))
                                            player.prepare()
                                            player.playWhenReady = true
                                            streamLoaded = true
                                            android.util.Log.d("VideoPlayerScreen", "✅ Progressive loaded")
                                        } else {
                                            // Video sin audio y no hay audio stream
                                            android.util.Log.w("VideoPlayerScreen", "Video is videoOnly but no audio stream available")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoPlayerScreen", "❌ DASH failed", e)
                                    e.printStackTrace()
                                }
                            }
                            
                            // OPCIÓN 4: Cualquier stream como último recurso
                            if (!streamLoaded && videoDetails.videoStreams.isNotEmpty()) {
                                android.util.Log.w("VideoPlayerScreen", "Trying any available stream as fallback")
                                try {
                                    val anyStream = videoDetails.videoStreams.firstOrNull { it.url.isNotEmpty() }
                                    if (anyStream != null) {
                                        android.util.Log.d("VideoPlayerScreen", "Loading fallback stream ${anyStream.quality}")
                                        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(anyStream.url))
                                        player.prepare()
                                        player.playWhenReady = true
                                        streamLoaded = true
                                        android.util.Log.d("VideoPlayerScreen", "✅ Fallback loaded")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoPlayerScreen", "❌ Fallback failed", e)
                                    e.printStackTrace()
                                }
                            }
                            
                            if (!streamLoaded) {
                                android.util.Log.e("VideoPlayerScreen", "❌ No stream loaded! HLS: ${videoDetails.hlsUrl != null}, Streams: ${videoDetails.videoStreams.size}")
                            }
                            
                            // Set playback speed
                            try {
                                player.setPlaybackSpeed(playerSettings.playbackSpeed)
                            } catch (e: Exception) {
                                android.util.Log.e("VideoPlayerScreen", "Error setting playback speed", e)
                            }
                            
                            // Notificar al ViewModel que la reproducción ha comenzado para este video
                            if (streamLoaded) {
                                viewModel.onPlaybackStarted()
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("VideoPlayerScreen", "❌ Fatal error loading stream", e)
                            e.printStackTrace()
                        }
                    } else {
                        android.util.Log.d("VideoPlayerScreen", "Using existing mini player - keeping content")
                        // Asegurar que el video siga reproduciéndose cuando viene del miniplayer
                        if (player.playbackState == androidx.media3.common.Player.STATE_READY || 
                            player.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                            // Mantener el estado de reproducción (si estaba reproduciendo, seguir reproduciendo)
                            if (!player.isPlaying && player.playWhenReady) {
                                player.play()
                            }
                        }
                    }
                    // Asignar player DESPUÉS de prepararlo
                // Asignar player DESPUÉS de prepararlo
                // exoPlayer = player // Removed: exoPlayer is now a val from ViewModel
                
                // TODO: Servicio de media desactivado temporalmente por crashes
                // El servicio intenta crear MediaItems sin URI causando NullPointerException
                /*
                try {
                    com.opentube.util.MediaServiceManager.startService(
                        context = context,
                        videoTitle = videoDetails.title,
                        videoUploader = videoDetails.uploader,
                        videoThumbnail = videoDetails.thumbnailUrl,
                        videoId = videoDetails.videoId,
                        player = player
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerScreen", "Error iniciando servicio de media", e)
                }
                */
                }
                
                onDispose {
                    // No liberar el player aquí, es gestionado por PlayerManager
                    if (!isMinimizing) {
                        player.pause()
                    }
                }
            }
            
            // Actualizar velocidad de reproducción cuando cambia
            LaunchedEffect(playerSettings.playbackSpeed) {
                exoPlayer?.setPlaybackSpeed(playerSettings.playbackSpeed)
            }
            
            // Change quality/audio when changed - exactamente como LibreTube
            LaunchedEffect(playerSettings.selectedQuality, playerSettings.selectedAudioTrack) {
                exoPlayer?.let { player ->
                    if (videoDetails.videoStreams.isEmpty()) return@let
                    
                    val savedPosition = player.currentPosition
                    val wasPlaying = player.isPlaying
                    
                    try {
                        // Usar la calidad seleccionada por el usuario
                        val selectedStream = playerSettings.selectedQuality
                        
                        if (selectedStream != null && selectedStream.url.isNotEmpty()) {
                            android.util.Log.d("VideoPlayerScreen", "Changing to quality: ${selectedStream.quality} (${selectedStream.height}p)")
                            
                            if (selectedStream.videoOnly) {
                                // Video DASH - necesita audio separado
                                val selectedAudio = playerSettings.selectedAudioTrack 
                                    ?: videoDetails.audioStreams.maxByOrNull { it.bitrate ?: 0 }
                                
                                if (selectedAudio != null) {
                                    val dashUri = DashHelper.createDashSource(
                                        videoStreams = listOf(selectedStream),
                                        audioStreams = listOf(selectedAudio),
                                        duration = videoDetails.duration
                                    )
                                    
                                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                                        .setUri(dashUri)
                                        .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                                        .build()
                                    
                                    player.setMediaItem(mediaItem)
                                    player.prepare()
                                    player.seekTo(savedPosition)
                                    player.playWhenReady = wasPlaying
                                }
                            } else {
                                // Progressive stream (video + audio juntos)
                                val mediaItem = androidx.media3.common.MediaItem.Builder()
                                    .setUri(selectedStream.url)
                                    .build()
                                
                                player.setMediaItem(mediaItem)
                                player.prepare()
                                player.seekTo(savedPosition)
                                player.playWhenReady = wasPlaying
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VideoPlayerScreen", "Error changing quality", e)
                        e.printStackTrace()
                    }
                }
            }
            
            // Get status bar height for proper padding in portrait mode
            val statusBarInsets = androidx.compose.foundation.layout.WindowInsets.statusBars
            val statusBarHeight = statusBarInsets.asPaddingValues().calculateTopPadding()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .then(
                        if (!isFullscreen) {
                            // En portrait, agregar padding superior para la status bar
                            Modifier.padding(top = statusBarHeight)
                        } else {
                            Modifier
                        }
                    )
            ) {
                // Video player
                Box(
                    modifier = Modifier
                        .then(
                            if (isFullscreen) {
                                // En fullscreen, usar toda la pantalla disponible
                                Modifier.fillMaxSize()
                            } else {
                                // En portrait, mantener aspect ratio 16:9
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            }
                        )
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    
                    // Ambient Mode (Ambilight effect)
                    // We show this behind the player, blurred
                    if (state is VideoPlayerUiState.Success) {
                        AsyncImage(
                            model = state.videoDetails.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.6f) // Adjust alpha for subtlety
                                .blur(
                                    radiusX = 100.dp, 
                                    radiusY = 100.dp, 
                                    edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded
                                )
                        )
                    }

                    // Obtener el resize mode del estado
                    val resizeMode = (uiState as? VideoPlayerUiState.Success)?.playerSettings?.resizeMode ?: 0

                    PlayerGestureOverlay(
                        onSingleTap = { showControls = !showControls },
                        onDoubleTapSeek = { seconds ->
                            exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) + seconds * 1000)
                        },
                        onSwipeDown = {
                            if (isFullscreen) {
                                viewModel.toggleFullscreen()
                            } else {
                                // Smart cast for accessing videoDetails
                                val currentState = uiState
                                if (currentState is VideoPlayerUiState.Success) {
                                    isMinimizing = true
                                    onMinimize?.invoke(
                                        currentState.videoDetails.title,
                                        currentState.videoDetails.uploader,
                                        currentState.videoDetails.thumbnailUrl,
                                        isPlaying,
                                        exoPlayer
                                    )
                                    onNavigateBack()
                                }
                            }
                        }
                    ) {
                    // ExoPlayer view
                    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
                    
                    // Forzar resize mode continuamente para prevenir cambios automáticos
                    LaunchedEffect(exoPlayer, isFullscreen, resizeMode) {
                        val actualResizeMode = when(resizeMode) {
                            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            4 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        
                        while (isActive) {
                            playerViewRef?.let { pv ->
                                // Forzar el resize mode seleccionado
                                if (pv.resizeMode != actualResizeMode) {
                                    pv.resizeMode = actualResizeMode
                                    android.util.Log.d("VideoPlayerScreen", "Aplicando resizeMode: $resizeMode ($actualResizeMode)")
                                }
                                pv.scaleX = 1f
                                pv.scaleY = 1f
                            }
                            delay(100) // Revisar cada 100ms
                        }
                    }
                    
                    AndroidView(
                        factory = { context ->
                            val mode = when(resizeMode) {
                                0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                                4 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = false
                                
                                // FORZAR uso de SurfaceView (mejor manejo de aspect ratio)
                                // 0 = SurfaceView, 1 = TextureView, 2 = SphericalGLSurfaceView, 3 = VideoDecoderGLSurfaceView
                                // Usamos reflection porque el método no es público
                                try {
                                    val method = PlayerView::class.java.getDeclaredMethod("setShutterBackgroundColor", Int::class.javaPrimitiveType)
                                    method.isAccessible = true
                                } catch (e: Exception) {
                                    android.util.Log.w("VideoPlayerScreen", "No se pudo configurar shutter color", e)
                                }
                                
                                // CRITICAL: Configuración para prevenir crop
                                this.resizeMode = mode
                                
                                // Configuración adicional
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                controllerShowTimeoutMs = 3000
                                controllerHideOnTouch = false
                                keepScreenOn = true
                                
                                // Forzar que use el tamaño exacto sin transformaciones
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                
                                playerViewRef = this
                            }
                        },
                        update = { playerView ->
                            val mode = when(resizeMode) {
                                0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                                4 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            
                            playerView.player = exoPlayer
                            // CRÍTICO: Aplicar resize mode desde el estado
                            playerView.resizeMode = mode
                            // Asegurar que no haya escala aplicada
                            playerView.scaleX = 1f
                            playerView.scaleY = 1f
                            
                            // Forzar invalidación para refrescar el render
                            playerView.invalidate()
                            
                            playerViewRef = playerView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxSize()
                    )
                    }

                    // Controles personalizados
                    PlayerControls(
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            bufferedPosition = bufferedPosition,
                            onPlayPauseClick = {
                                exoPlayer?.let { player ->
                                    if (player.isPlaying) player.pause() else player.play()
                                }
                            },
                            onSeek = { position ->
                                exoPlayer?.seekTo(position)
                            },
                            onRewind = {
                                exoPlayer?.let { player ->
                                    player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                }
                            },
                            onForward = {
                                exoPlayer?.let { player ->
                                    player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                }
                            },
                            onFullscreenClick = {
                                viewModel.toggleFullscreen()
                            },
                            onSettingsClick = {
                                viewModel.showSettingsSheet()
                            },
                            onResizeModeClick = {
                                viewModel.cycleResizeMode()
                            },
                            onBackClick = {
                                if (isFullscreen) {
                                    viewModel.toggleFullscreen()
                                } else {
                                    isMinimizing = true
                                    onMinimize?.invoke(
                                        videoDetails.title,
                                        videoDetails.uploader,
                                        videoDetails.thumbnailUrl,
                                        isPlaying,
                                        exoPlayer
                                    )
                                }
                            },
                            onShareClick = {
                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            isFullscreen = isFullscreen,
                            visible = showControls,
                            videoTitle = videoDetails.title,
                            uploader = videoDetails.uploader,
                            resizeMode = resizeMode,
                            onNextVideo = {
                                if (videoDetails.relatedStreams.isNotEmpty()) {
                                    val nextVideo = videoDetails.relatedStreams.first()
                                    onVideoClick?.invoke(nextVideo.videoId)
                                }
                            },
                            onPreviousVideo = {
                                exoPlayer?.let { player ->
                                    if (player.currentPosition > 5000) {
                                        player.seekTo(0)
                                    } else {
                                        onNavigateBack()
                                    }
                                } ?: onNavigateBack()
                            },
                            onMoreVideosClick = { 
                                showMoreVideos = true 
                                showComments = false
                            },
                            onCommentsClick = {
                                showComments = !showComments
                                if (showComments) showMoreVideos = false
                            }
                        )

                    // Más Videos Overlay (Horizontal)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFullscreen && showMoreVideos,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp) // Adjust height as needed
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(top = 16.dp, bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Más videos",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { showMoreVideos = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            tint = Color.White
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(videoDetails.relatedStreams.take(20)) { relatedVideo ->
                                        Box(modifier = Modifier.width(280.dp)) {
                                            VideoCard(
                                                video = relatedVideo,
                                                onClick = { 
                                                    showMoreVideos = false
                                                    onVideoClick?.invoke(relatedVideo.videoId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Panel Lateral de Comentarios (Horizontal)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFullscreen && showComments && !showMoreVideos,
                        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.4f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Comentarios",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { showComments = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                                    }
                                }
                                Divider()
                                Box(modifier = Modifier.weight(1f)) {
                                    CommentsSection(
                                        comments = state.comments,
                                        isLoading = state.isLoadingComments,
                                        onLoadMore = { viewModel.loadMoreComments() },
                                        onLoadReplies = { commentId, repliesPage ->
                                            viewModel.loadReplies(commentId, repliesPage)
                                        },
                                        replies = state.replies,
                                        loadingReplies = state.loadingReplies
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (!isFullscreen) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Video info
                        item {
                            com.opentube.ui.screens.player.components.VideoInfoSection(
                                title = videoDetails.title,
                                uploader = videoDetails.uploader,
                                uploaderAvatar = videoDetails.uploaderAvatar,
                                uploaderVerified = videoDetails.uploaderVerified,
                                views = videoDetails.views,
                                likes = videoDetails.likes,
                                uploadDate = videoDetails.uploadDate,
                                description = videoDetails.description,
                                subscriberCount = videoDetails.subscriberCount,
                                isFavorite = state.isFavorite,
                                isSubscribed = state.isSubscribed,
                                onFavoriteClick = { viewModel.toggleFavorite() },
                                onSubscribeClick = { viewModel.toggleSubscription() },
                                onChannelClick = { 
                                    val channelId = videoDetails.uploaderUrl.substringAfterLast("/")
                                    onChannelClick?.invoke(channelId)
                                },
                                onShareClick = {
                                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                        
                        // Comments section
                        item {
                            CommentsSection(
                                comments = state.comments,
                                isLoading = state.isLoadingComments,
                                onLoadMore = { viewModel.loadMoreComments() },
                                onLoadReplies = { commentId, repliesPage ->
                                    viewModel.loadReplies(commentId, repliesPage)
                                },
                                replies = state.replies,
                                loadingReplies = state.loadingReplies
                            )
                        }
                        
                        // Related videos
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A continuación",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(videoDetails.relatedStreams.take(20)) { relatedVideo ->
                            VideoCard(
                                video = relatedVideo,
                                onClick = { 
                                    onVideoClick?.invoke(relatedVideo.videoId)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Settings sheet
            if (showSettingsSheet) {
                                PlayerSettingsSheet(
                    videoStreams = videoDetails.videoStreams,
                    audioStreams = videoDetails.audioStreams,
                    subtitleStreams = videoDetails.subtitleStreams,
                    currentQuality = playerSettings.selectedQuality,
                    currentAudioTrack = playerSettings.selectedAudioTrack,
                    currentSubtitleTrack = null,
                    currentSpeed = playerSettings.playbackSpeed,
                    subtitlesEnabled = false,
                    musicModeEnabled = false,
                    onQualitySelected = { viewModel.selectQuality(it) },
                    onAudioSelected = { viewModel.selectAudioTrack(it) },
                    onSubtitleSelected = { },
                    onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                    onSubtitlesToggle = { },
                    onMusicModeToggle = { },
                    onDismiss = { viewModel.hideSettingsSheet() }
                )
            }
        }
    }
}
