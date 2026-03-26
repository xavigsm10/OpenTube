package com.opentube.ui.screens.channel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opentube.ui.components.VideoCard
import com.opentube.ui.screens.channel.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelId: String,
    onNavigateBack: () -> Unit,
    onVideoClick: (String, String, String, String, androidx.compose.ui.geometry.Rect?) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ChannelTab.VIDEOS) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Canal",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(
                    top = 8.dp,
                    bottom = 0.dp
                )
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ChannelUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is ChannelUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { viewModel.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }
                
                is ChannelUiState.Success -> {
                    val channel = state.channel
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Channel Header
                        item {
                            ChannelHeader(
                                channelName = channel.name,
                                channelAvatar = channel.avatarUrl,
                                channelBanner = channel.bannerUrl,
                                subscriberCount = channel.subscriberCount,
                                isSubscribed = state.isSubscribed,
                                isVerified = channel.verified,
                                onSubscribeClick = { viewModel.toggleSubscription() },
                                onShareClick = { /* TODO: Share channel */ }
                            )
                        }
                        
                        // Tabs
                        item {
                            ChannelTabs(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                        
                        // Tab Content
                        when (selectedTab) {
                            ChannelTab.VIDEOS -> {
                                if (channel.videos.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No hay videos disponibles",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = channel.videos,
                                        key = { it.url }
                                    ) { video ->
                                        VideoCard(
                                            video = video,
                                            onClickWithRect = { rect -> onVideoClick(video.videoId, video.title, video.uploaderName ?: "Unknown", video.thumbnail, rect) },
                                            onClick = { onVideoClick(video.videoId, video.title, video.uploaderName ?: "Unknown", video.thumbnail, null) },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            
                            ChannelTab.PLAYLISTS -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
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
                            }
                            
                            ChannelTab.ABOUT -> {
                                item {
                                    AboutTab(
                                        description = channel.description
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
