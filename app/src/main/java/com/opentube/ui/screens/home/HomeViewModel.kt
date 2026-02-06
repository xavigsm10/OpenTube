package com.opentube.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opentube.data.models.Video
import com.opentube.data.extractor.PagedResult
import com.opentube.data.models.Playlist
import com.opentube.data.models.Album
import com.opentube.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Categorías disponibles en la pantalla de inicio
 */
enum class HomeCategory(val displayName: String) {
    ALL("Todos"),
    GAMING("Gaming"),
    MUSIC("Música"),
    SPORTS("Deportes"),
    LIVE("En vivo")
}

/**
 * UI State for Home screen
 */
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val videos: List<Video>, 
        val selectedCategory: HomeCategory,
        val playlists: List<Playlist> = emptyList(),
        val albums: List<Album> = emptyList(),
        val nextPageUrl: String? = null,
        val isLoadingMore: Boolean = false
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * ViewModel for Home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val instancePreferences: com.opentube.data.local.InstancePreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val currentRegion = instancePreferences.regionFlow.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        "ES"
    )
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private var currentCategory = HomeCategory.ALL
    
    init {
        android.util.Log.d("HomeViewModel", "=== HomeViewModel INIT ===")
        loadContentForCategory(HomeCategory.ALL)
    }

    fun updateRegion(regionCode: String) {
        viewModelScope.launch {
            instancePreferences.setRegion(regionCode)
            // Wait slightly for NewPipeHelper to pick up the change via DataStore observation
            kotlinx.coroutines.delay(200) 
            refresh()
        }
    }
    
    fun refresh() {
        android.util.Log.d("HomeViewModel", "refresh() called")
        viewModelScope.launch {
            _isRefreshing.value = true
            loadContentForCategory(currentCategory)
            _isRefreshing.value = false
        }
    }
    
    fun selectCategory(category: HomeCategory) {
        android.util.Log.d("HomeViewModel", "selectCategory: ${category.displayName}")
        if (currentCategory == category) return
        currentCategory = category
        loadContentForCategory(category)
    }
    
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success && !currentState.isLoadingMore && currentState.nextPageUrl != null) {
            android.util.Log.d("HomeViewModel", "loadMore() called. Next page: ${currentState.nextPageUrl}")
            viewModelScope.launch {
                _uiState.value = currentState.copy(isLoadingMore = true)
                
                // Determine which method to call based on category
                if (currentCategory != HomeCategory.ALL && currentCategory != HomeCategory.MUSIC) {
                     videoRepository.getTrendingPaged(currentState.nextPageUrl).collect { result ->
                        result.fold(
                            onSuccess = { pagedResult ->
                                _uiState.value = currentState.copy(
                                    videos = currentState.videos + pagedResult.items,
                                    nextPageUrl = pagedResult.nextPageUrl,
                                    isLoadingMore = false
                                )
                            },
                            onFailure = {
                                _uiState.value = currentState.copy(isLoadingMore = false)
                            }
                        )
                     }
                } else {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            }
        }
    }
    
    private fun loadContentForCategory(category: HomeCategory) {
        android.util.Log.d("HomeViewModel", "loadContentForCategory: ${category.displayName}")
        // USAR IO DISPATCHER PARA EVITAR BLOQUEOS (Pantalla Negra)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = HomeUiState.Loading
            processCategoryLoad(category)
        }
    }

    private suspend fun processCategoryLoad(category: HomeCategory) {
        when (category) {
            HomeCategory.ALL -> loadAllRandomContent()
            HomeCategory.MUSIC -> loadMusicContent()
            HomeCategory.GAMING, HomeCategory.SPORTS, HomeCategory.LIVE -> {
                val flow = when (category) {
                    HomeCategory.GAMING -> videoRepository.getGamingVideos()
                    HomeCategory.SPORTS -> videoRepository.getSportsVideos()
                    HomeCategory.LIVE -> videoRepository.getLiveVideos()
                    else -> videoRepository.getTrending()
                }
                flow.collect { result ->
                    handleListResult(result, category)
                }
            }

        }
    }

    private fun handleListResult(result: Result<List<Video>>, category: HomeCategory) {
        _uiState.value = result.fold(
            onSuccess = { videos ->
                HomeUiState.Success(videos, category)
            },
            onFailure = { exception ->
                HomeUiState.Error(exception.message ?: "Error desconocido")
            }
        )
    }

    private fun handlePagedResult(result: Result<PagedResult<Video>>, category: HomeCategory) {
        _uiState.value = result.fold(
            onSuccess = { pagedResult ->
                HomeUiState.Success(
                    videos = pagedResult.items,
                    selectedCategory = category,
                    nextPageUrl = pagedResult.nextPageUrl
                )
            },
            onFailure = { exception ->
                HomeUiState.Error(exception.message ?: "Error desconocido")
            }
        )
    }

    private suspend fun loadMusicContent() {
        try {
            // Cargar videos, playlists y álbumes en paralelo (o secuencial por ahora)
            var videos: List<Video> = emptyList()
            var playlists: List<Playlist> = emptyList()
            var albums: List<Album> = emptyList()
            
            // 1. Videos de música
            videoRepository.getMusicVideos().collect { result ->
                result.onSuccess { videos = it }
            }
            
            // 2. Playlists (Buscamos "Music Playlists")
            videoRepository.getPlaylists("Music").collect { result ->
                result.onSuccess { playlists = it }
            }
            
            // 3. Álbumes (Buscamos "Full Album")
            videoRepository.getAlbums("Full Album").collect { result ->
                result.onSuccess { albums = it }
            }
            
            if (videos.isNotEmpty() || playlists.isNotEmpty() || albums.isNotEmpty()) {
                _uiState.value = HomeUiState.Success(
                    videos = videos,
                    selectedCategory = HomeCategory.MUSIC,
                    playlists = playlists,
                    albums = albums
                )
            } else {
                _uiState.value = HomeUiState.Error("No se encontró contenido musical")
            }
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error("Error cargando música: ${e.message}")
        }
    }
    
    private suspend fun loadAllRandomContent() {
        // Ejecutar en IO para evitar bloqueos en el hilo principal
        // Ejecutar en IO con timeout de 15 segundos para evitar bloqueos infinitos
        withContext(Dispatchers.IO) {
            try {
                kotlinx.coroutines.withTimeout(15000L) {
                    val mutex = Mutex()
                    val allVideos = mutableListOf<Video>()
                    
                    // 1. Cargar Trending primero
                    val trendingFlow = videoRepository.getTrending()
                    
                    trendingFlow.collect { result -> 
                        result.fold(
                            onSuccess = { videos ->
                                if (videos.isNotEmpty()) {
                                    android.util.Log.d("HomeViewModel", "Loaded ${videos.size} trending videos. Updating UI to Success.")
                                    mutex.withLock {
                                        allVideos.addAll(videos.take(10))
                                    }
                                    _uiState.value = HomeUiState.Success(allVideos.toList(), HomeCategory.ALL)
                                } else {
                                    android.util.Log.e("HomeViewModel", "Trending returned empty list")
                                }
                            },
                            onFailure = { e ->
                                android.util.Log.e("HomeViewModel", "Error loading trending", e)
                                // No hacemos nada aquí, dejamos que las otras coroutines intenten cargar contenido
                            }
                        )
                    }
                    
                    // 2. Cargar el resto en paralelo
                    launch {
                        val musicFlow = videoRepository.getMusicVideos()
                        musicFlow.collect { result -> 
                            result.fold(
                                onSuccess = { videos ->
                                    mutex.withLock {
                                        allVideos.addAll(videos)
                                    }
                                    val distinctVideos = mutex.withLock { allVideos.toList() }.distinctBy { video -> video.url }.shuffled()
                                    _uiState.value = HomeUiState.Success(distinctVideos, HomeCategory.ALL)
                                },
                                onFailure = { e ->
                                    android.util.Log.e("HomeViewModel", "Error loading music", e)
                                }
                            )
                        }
                    }
                    
                    launch {
                        val gamingFlow = videoRepository.getGamingVideos()
                        gamingFlow.collect { result -> 
                            result.fold(
                                onSuccess = { videos ->
                                    mutex.withLock {
                                        allVideos.addAll(videos)
                                    }
                                    val distinctVideos = mutex.withLock { allVideos.toList() }.distinctBy { video -> video.url }.shuffled()
                                    _uiState.value = HomeUiState.Success(distinctVideos, HomeCategory.ALL)
                                },
                                onFailure = { e ->
                                    android.util.Log.e("HomeViewModel", "Error loading gaming", e)
                                }
                            )
                        }
                    }
                    
                    launch {
                        val sportsFlow = videoRepository.getSportsVideos()
                        sportsFlow.collect { result -> 
                            result.fold(
                                onSuccess = { videos ->
                                    mutex.withLock {
                                        allVideos.addAll(videos)
                                    }
                                    val distinctVideos = mutex.withLock { allVideos.toList() }.distinctBy { video -> video.url }.shuffled()
                                    _uiState.value = HomeUiState.Success(distinctVideos, HomeCategory.ALL)
                                },
                                onFailure = { e ->
                                    android.util.Log.e("HomeViewModel", "Error loading sports", e)
                                }
                            )
                        }
                    }
                }
                
                // Final check: si después de todo (o timeout) no hay videos y seguimos en Loading, mostrar error
                if (_uiState.value is HomeUiState.Loading) {
                     _uiState.value = HomeUiState.Error("No se pudo cargar contenido. Verifica tu conexión a internet.")
                }
                
                Unit
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error in loadAllRandomContent", e)
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error("Tiempo de espera agotado o error: ${e.message}")
                }
                Unit
            }
        }
    }
    
    fun retry() {
        loadContentForCategory(currentCategory)
    }
}