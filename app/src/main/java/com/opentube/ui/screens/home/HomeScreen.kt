package com.opentube.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opentube.R
import com.opentube.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (String, String, String, String, androidx.compose.ui.geometry.Rect?) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentRegion by viewModel.currentRegion.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var showRegionDialog by remember { mutableStateOf(false) }

    val availableCountries = remember {
        listOf(
            "Global" to "GLOBAL",
            "Argentina" to "AR",
            "Brazil" to "BR",
            "Canada" to "CA",
            "Chile" to "CL",
            "Colombia" to "CO",
            "France" to "FR",
            "Germany" to "DE",
            "India" to "IN",
            "Italy" to "IT",
            "Japan" to "JP",
            "Mexico" to "MX",
            "Russia" to "RU",
            "South Korea" to "KR",
            "Spain" to "ES",
            "United Kingdom" to "GB",
            "United States" to "US",
            "Venezuela" to "VE"
        ).sortedBy { it.first }
    }

    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text("Seleccionar Región") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(availableCountries) { (name, code) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateRegion(code)
                                    showRegionDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (code == currentRegion),
                                onClick = null 
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Modo normal de videos (YouTube-like)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "OpenTube",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { showRegionDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Public, // Globe icon
                            contentDescription = "Region",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Surface(
                         modifier = Modifier
                             .fillMaxSize(),
                         color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando contenido...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                
                is HomeUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp), // Extra padding to show content below Blur bar
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        // Tarjetas de categorías
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(HomeCategory.values()) { category ->
                                    FilterChip(
                                        selected = state.selectedCategory == category,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = { Text(category.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                        
                        // Lista de videos
                        items(
                            items = state.videos,
                            key = { it.url }
                        ) { video ->
                            VideoCard(
                                video = video,
                                onClickWithRect = { rect -> onVideoClick(video.videoId, video.title, video.uploaderName, video.thumbnail, rect) },
                                onClick = { onVideoClick(video.videoId, video.title, video.uploaderName, video.thumbnail, null) }
                            )
                        }
                        
                        // Infinite Scroll Trigger & Loading Indicator
                        item {
                            if (state.isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (state.nextPageUrl != null) {
                                // Trigger load more when this item becomes visible
                                LaunchedEffect(Unit) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    }
                }
                }
                
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.refresh() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}