package com.opentube.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.opentube.R
import com.opentube.ui.components.VideoCard
import com.opentube.data.models.Video

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Search screen with Paging 3 (Infinite Scrolling)
 * Implements behavior similar to LibreTube
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchResultsFlow by viewModel.searchResults.collectAsState()
    val pagingItems = searchResultsFlow.collectAsLazyPagingItems()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Request focus only when first entering the screen with no existing search
    // This prevents keyboard from appearing when returning from video player
    LaunchedEffect(Unit) {
        if (searchQuery.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                            showSuggestions = it.isNotEmpty()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.action_search)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.search()
                                showSuggestions = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.updateSearchQuery("")
                                    showSuggestions = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.search()
                            showSuggestions = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        enabled = searchQuery.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // SUGGESTIONS STATE
                showSuggestions && suggestions.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(suggestions) { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    viewModel.updateSearchQuery(suggestion)
                                    viewModel.search(suggestion)
                                    showSuggestions = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
                
                // HISTORY STATE
                searchQuery.isEmpty() && searchHistory.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Historial de búsqueda",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Limpiar")
                                }
                            }
                        }
                        items(searchHistory) { historyItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.updateSearchQuery(historyItem.query)
                                    viewModel.search(historyItem.query)
                                    showSuggestions = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ) {
                                ListItem(
                                    headlineContent = { Text(historyItem.query) },
                                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (historyItem.thumbnailUrl != null) {
                                                AsyncImage(
                                                    model = historyItem.thumbnailUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(56.dp, 32.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            IconButton(onClick = { viewModel.removeFromHistory(historyItem.query) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Eliminar")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // RESULTS STATE (Paging)
                else -> {
                    if (pagingItems.itemCount > 0 || pagingItems.loadState.refresh is LoadState.Loading) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pagingItems.itemCount) { index ->
                                val video = pagingItems[index]
                                if (video != null) {
                                    VideoCard(
                                        video = video,
                                        onClick = {
                                            if (searchQuery.isNotEmpty()) {
                                                viewModel.search(searchQuery, video.thumbnail)
                                            }
                                            onVideoClick(video.videoId)
                                        }
                                    )
                                }
                            }
                            
                            // 2. Append Loading State (Small loader at the end)
                            when (pagingItems.loadState.append) {
                                is LoadState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }
                                }
                                is LoadState.Error -> {
                                    item {
                                        Button(
                                            onClick = { pagingItems.retry() },
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                        ) {
                                            Text("Error al cargar más. Reintentar")
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    
                    // 1. Initial Loading State (Centered loader)
                    // 3. Retry Button (Initial Error)
                    when (pagingItems.loadState.refresh) {
                        is LoadState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is LoadState.Error -> {
                            val e = pagingItems.loadState.refresh as LoadState.Error
                            Column(
                                Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Error: ${e.error.localizedMessage ?: "Unknown Error"}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { pagingItems.retry() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                        else -> {
                            if (pagingItems.itemCount == 0 && searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No se encontraron resultados",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
