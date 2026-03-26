package com.opentube.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opentube.data.models.Video
import com.opentube.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onVideoClick: (String, String, String, String, androidx.compose.ui.geometry.Rect?) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Biblioteca",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (selectedTab == 0 && uiState.historyVideos.isNotEmpty()) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Borrar historial",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Historial") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
                
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favoritos") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                )
                
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Playlists") },
                    icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> HistoryTab(
                    videos = uiState.historyVideos,
                    onVideoClick = onVideoClick,
                    onDeleteClick = { viewModel.deleteFromHistory(it) }
                )
                
                1 -> FavoritesTab(
                    videos = uiState.favoriteVideos,
                    onVideoClick = onVideoClick,
                    onDeleteClick = { viewModel.deleteFromFavorites(it) }
                )
                
                2 -> PlaylistsTab()
            }
        }
    }
    
    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Borrar historial") },
            text = { Text("¿Estás seguro de que quieres borrar todo el historial de reproducción?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HistoryTab(
    videos: List<com.opentube.data.local.WatchHistoryEntity>,
    onVideoClick: (String, String, String, String, androidx.compose.ui.geometry.Rect?) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No hay videos en el historial",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = videos,
                key = { it.videoId }
            ) { historyItem ->
                CustomSwipeToDismissBox(
                    onDismissed = { onDeleteClick(historyItem.videoId) },
                    content = {
                        VideoCard(
                            video = Video(
                                url = "/watch?v=${historyItem.videoId}",
                                title = historyItem.title,
                                thumbnail = historyItem.thumbnail,
                                uploaderName = historyItem.uploaderName,
                                uploaderUrl = null,
                                uploaderAvatar = null,
                                uploadedDate = null,
                                duration = historyItem.duration,
                                views = historyItem.viewCount,
                                uploaderVerified = false,
                                isShort = false
                            ),
                            onClickWithRect = { rect -> onVideoClick(historyItem.videoId, historyItem.title, historyItem.uploaderName, historyItem.thumbnail, rect) },
                            onClick = { onVideoClick(historyItem.videoId, historyItem.title, historyItem.uploaderName, historyItem.thumbnail, null) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    videos: List<com.opentube.data.local.FavoriteEntity>,
    onVideoClick: (String, String, String, String, androidx.compose.ui.geometry.Rect?) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No hay videos favoritos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = videos,
                key = { it.videoId }
            ) { favoriteItem ->
                CustomSwipeToDismissBox(
                    onDismissed = { onDeleteClick(favoriteItem.videoId) },
                    content = {
                        VideoCard(
                            video = Video(
                                url = "/watch?v=${favoriteItem.videoId}",
                                title = favoriteItem.title,
                                thumbnail = favoriteItem.thumbnail,
                                uploaderName = favoriteItem.uploaderName,
                                uploaderUrl = null,
                                uploaderAvatar = null,
                                uploadedDate = null,
                                duration = favoriteItem.duration,
                                views = favoriteItem.viewCount,
                                uploaderVerified = false,
                                isShort = false
                            ),
                            onClickWithRect = { rect -> onVideoClick(favoriteItem.videoId, favoriteItem.title, favoriteItem.uploaderName, favoriteItem.thumbnail, rect) },
                            onClick = { onVideoClick(favoriteItem.videoId, favoriteItem.title, favoriteItem.uploaderName, favoriteItem.thumbnail, null) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Próximamente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSwipeToDismissBox(
    onDismissed: () -> Unit,
    content: @Composable () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }
    
    if (!isDismissed) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.StartToEnd || 
                    dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    isDismissed = true
                    onDismissed()
                    true
                } else {
                    false
                }
            }
        )
        
        androidx.compose.material3.SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            content = { content() }
        )
    }
}
