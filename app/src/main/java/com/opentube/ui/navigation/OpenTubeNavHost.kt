package com.opentube.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opentube.ui.screens.home.HomeScreen
import com.opentube.ui.screens.splash.SplashScreen
import com.opentube.ui.screens.search.SearchScreen
import com.opentube.ui.screens.player.VideoPlayerScreen
import com.opentube.ui.screens.channel.ChannelScreen
import com.opentube.ui.screens.library.LibraryScreen
import com.opentube.ui.screens.subscriptions.SubscriptionsScreen
import com.opentube.ui.screens.settings.SettingsScreen
import com.opentube.ui.components.MiniPlayer
import com.opentube.ui.viewmodels.MiniPlayerViewModel

/**
 * Main navigation host for the app
 */
@Composable
fun OpenTubeNavHost(
    navController: NavHostController = rememberNavController(),
    miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val miniPlayerState by miniPlayerViewModel.miniPlayerState.collectAsState()
    
    // Show bottom bar only on main screens
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Shorts.route,
        Screen.Subscriptions.route,
        Screen.Library.route,
        Screen.Settings.route
    )
    
    // Hide mini player when on video player screen
    val isOnVideoPlayer = currentDestination?.route?.startsWith("video/") == true
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Screen.Home.route
                        } == true,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = Icons.Default.SlowMotionVideo,
                                contentDescription = null
                            ) 
                        },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Screen.Shorts.route
                        } == true,
                        onClick = {
                            navController.navigate(Screen.Shorts.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Subscriptions, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Screen.Subscriptions.route
                        } == true,
                        onClick = {
                            navController.navigate(Screen.Subscriptions.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Screen.Library.route
                        } == true,
                        onClick = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Screen.Settings.route
                        } == true,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // NavHost - SIEMPRE con el mismo padding para evitar que se mueva
            // El padding SIEMPRE incluye el bottomBar, independiente del mini player
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        // top = paddingValues.calculateTopPadding(), // Removed: Let screens handle top insets
                        bottom = if (showBottomBar) {
                            paddingValues.calculateBottomPadding()
                        } else {
                            0.dp
                        }
                    ),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onVideoClick = { videoId ->
                        android.util.Log.d("Navigation", "Home: Navigating to video: $videoId")
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    },

                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }
            
            composable(Screen.Shorts.route) {
                com.opentube.ui.screens.shorts.ShortsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }
            
            composable(
                route = Screen.Search.route,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(500)
                    ) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(500)
                    ) + fadeOut(animationSpec = tween(500))
                }
            ) {
                SearchScreen(
                    onBackClick = { navController.navigateUp() },
                    onVideoClick = { videoId ->
                        android.util.Log.d("Navigation", "Search: Navigating to video: $videoId")
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                com.opentube.ui.screens.playlist.PlaylistScreen(
                    playlistId = playlistId,
                    onBackClick = { navController.navigateUp() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    }
                )
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
                com.opentube.ui.screens.album.AlbumScreen(
                    albumId = albumId,
                    onBackClick = { navController.navigateUp() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    }
                )
            }
            
            composable(
                route = Screen.VideoPlayer.route,
                arguments = listOf(
                    navArgument("videoId") { type = NavType.StringType }
                ),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                android.util.Log.d("Navigation", "VideoPlayerScreen composable started for videoId: $videoId")
                
                // Solo ocultar mini player visualmente, NO liberar el player
                LaunchedEffect(Unit) {
                    miniPlayerViewModel.hideMiniPlayerOnly()
                }
                
                VideoPlayerScreen(
                    videoId = videoId,
                    existingPlayer = miniPlayerState.player, // Pasar el player existente del mini player
                    onNavigateBack = { 
                        navController.navigateUp()
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onVideoClick = { newVideoId ->
                        // Limpiar mini player si existe
                        miniPlayerViewModel.hideMiniPlayer()
                        // Navegar al nuevo video
                        navController.navigate(Screen.VideoPlayer.createRoute(newVideoId)) {
                            // Eliminar la instancia anterior del reproductor del backstack
                            popUpTo(Screen.VideoPlayer.route.replace("/{videoId}", "")) {
                                inclusive = false
                            }
                        }
                    },
                    onMinimize = { title, channel, thumbnailUrl, isPlaying, player ->
                        miniPlayerViewModel.showMiniPlayer(
                            videoId = videoId,
                            title = title,
                            channelName = channel,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            player = player
                        )
                        // No llamar a navigateUp() aquí - dejar que la navegación normal maneje el retroceso
                    }
                )
            }
            
            composable(
                route = Screen.Channel.route,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
                ChannelScreen(
                    channelId = channelId,
                    onNavigateBack = { navController.navigateUp() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    }
                )
            }
            
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }
            
            composable(Screen.Library.route) {
                LibraryScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                com.opentube.ui.screens.settings.SettingsScreen()
            }
        } // Fin del NavHost
            
        // Mini player positioned above bottom bar
        // Mini Player (Floating PiP)
        if (!isOnVideoPlayer && miniPlayerState.isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = if (showBottomBar) paddingValues.calculateBottomPadding() + 16.dp else 16.dp,
                        end = 16.dp
                    )
            ) {
                MiniPlayer(
                    state = miniPlayerState,
                    onPlayPauseClick = {
                        miniPlayerViewModel.togglePlayPause()
                    },
                    onClose = {
                        miniPlayerViewModel.closeMiniPlayer()
                    },
                    onClick = {
                        navController.navigate(Screen.VideoPlayer.createRoute(miniPlayerState.videoId))
                    },
                    onSeekForward = {
                        miniPlayerViewModel.seekForward()
                    },
                    onSeekBackward = {
                        miniPlayerViewModel.seekBackward()
                    }
                )
            }
        }
    } // Fin del Box padre
    } // Fin del Scaffold
} // Fin de la función OpenTubeNavHost

/**
 * Placeholder screen for unimplemented screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Esta pantalla está en desarrollo")
        }
    }
}
