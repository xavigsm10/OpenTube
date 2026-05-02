package com.opentube.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opentube.data.local.FavoriteDao
import com.opentube.data.local.FavoriteEntity
import com.opentube.data.local.WatchHistoryDao
import com.opentube.data.local.WatchHistoryEntity
import com.opentube.data.local.SubscriptionDao
import com.opentube.data.local.SubscriptionEntity
import com.opentube.data.models.VideoDetails
import com.opentube.data.models.VideoStream
import com.opentube.data.models.AudioStream
import com.opentube.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerSettings(
    val selectedQuality: VideoStream? = null,
    val selectedAudioTrack: AudioStream? = null,
    val playbackSpeed: Float = 1.0f,
    val subtitlesEnabled: Boolean = false,
    val isFullscreen: Boolean = false,
    val showControls: Boolean = true,
    val resizeMode: Int = 0 // 0=FIT, 1=FILL, 2=ZOOM, 3=FIXED_WIDTH, 4=FIXED_HEIGHT
)

sealed interface VideoPlayerUiState {
    object Loading : VideoPlayerUiState
    data class Success(
        val videoDetails: VideoDetails,
        val playerSettings: PlayerSettings = PlayerSettings(),
        val isFavorite: Boolean = false,
        val isSubscribed: Boolean = false,
        val currentPosition: Long = 0,
        val comments: List<Comment> = emptyList(),
        val commentsNextPage: String? = null,
        val commentsCount: Long = 0,
        val isLoadingComments: Boolean = false,
        val replies: Map<String, List<Comment>> = emptyMap(), // commentId -> replies
        val loadingReplies: Set<String> = emptySet() // Set of comment IDs currently loading replies
    ) : VideoPlayerUiState
    data class Error(val message: String) : VideoPlayerUiState
}

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val subscriptionDao: SubscriptionDao,
    private val playerManager: com.opentube.player.PlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    val player = playerManager.player
    
    private var videoId: String = savedStateHandle["videoId"] ?: ""
    
    fun setVideoId(id: String) {
        if (id.isEmpty()) return
        if (this.videoId == id) {
            // Already initialized for this video, just ensure we show success if cached
            if (playerManager.currentVideoId.value == id && playerManager.cachedVideoDetails != null) {
                // UI should already be in success state, or we can force it
            }
            return
        }
        
        this.videoId = id
        initializeVideo()
    }
    
    private val _uiState = MutableStateFlow<VideoPlayerUiState>(VideoPlayerUiState.Loading)
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()
    
    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()
    
    init {
        if (videoId.isNotEmpty()) {
            initializeVideo()
        }
    }
    
    private fun initializeVideo() {
        android.util.Log.d("VideoPlayerViewModel", "=== ViewModel INIT for videoId: $videoId ===")
        
        // Check if we are already playing this video and have cached details
        if (playerManager.currentVideoId.value == videoId && playerManager.cachedVideoDetails != null) {
            android.util.Log.d("VideoPlayerViewModel", "Using cached video details - seamless transition!")
            val cachedDetails = playerManager.cachedVideoDetails!!
            
            // Ensure videoId is set (should be already, but for safety)
            playerManager.setVideoId(videoId)
            
            // Immediately set success state with cached details
            _uiState.value = VideoPlayerUiState.Success(
                videoDetails = cachedDetails,
                playerSettings = PlayerSettings(
                    selectedQuality = cachedDetails.videoStreams
                        .filter { !it.url.isNullOrEmpty() }
                        .maxByOrNull { it.height ?: 0 },
                    selectedAudioTrack = cachedDetails.audioStreams
                        .filter { !it.url.isNullOrEmpty() }
                        .maxByOrNull { it.bitrate ?: 0 }
                )
            )
            
            // Still load fresh data in background, but UI is already showing
            loadVideo(skipLoadingState = true)
        } else {
            // Normal flow - show loading state
            loadVideo(skipLoadingState = false)
        }
        
        checkIfFavorite()
        checkIfSubscribed()
        loadComments()
    }
    
    private fun loadVideo(skipLoadingState: Boolean = false) {
        android.util.Log.d("VideoPlayerViewModel", "loadVideo() called for videoId: $videoId (skipLoadingState=$skipLoadingState)")
        viewModelScope.launch {
            try {
                if (!skipLoadingState) {
                    _uiState.value = VideoPlayerUiState.Loading
                }
                
                videoRepository.getVideoDetails(videoId).collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { details ->
                            try {
                                android.util.Log.d("VideoPlayerViewModel", "Video details loaded successfully")
                                
                                // Fetch SponsorBlock segments
                                viewModelScope.launch {
                                    videoRepository.getSegments(videoId).collect { result ->
                                        result.onSuccess { loadedSegments ->
                                            segments = loadedSegments
                                            startSponsorBlockMonitor()
                                        }
                                    }
                                }

                                android.util.Log.d("VideoPlayerViewModel", "HLS URL: ${details.hlsUrl ?: "null"}")
                                android.util.Log.d("VideoPlayerViewModel", "Video streams: ${details.videoStreams.size}")
                                android.util.Log.d("VideoPlayerViewModel", "Audio streams: ${details.audioStreams.size}")
                                
                                // Cache the details for future transitions
                                playerManager.cacheVideoDetails(details)
                                // Cache the details for future transitions
                                playerManager.cacheVideoDetails(details)
                                // playerManager.setVideoId(videoId) - Moved to onPlaybackStarted
                                
                                // Guardar en historial
                                saveToHistory(details)
                                
                                // Tomar el mejor video disponible (capped at 1080p for initial load to avoid buffering)
                                // User can manually select higher quality afterwards
                                val bestVideo = details.videoStreams
                                    .filter { !it.url.isNullOrEmpty() && (it.height ?: 0) <= 1080 }
                                    .maxByOrNull { it.height ?: 0 }
                                    ?: details.videoStreams // Fallback: if no <=1080p streams, take any
                                        .filter { !it.url.isNullOrEmpty() }
                                        .minByOrNull { it.height ?: Int.MAX_VALUE }
                                
                                android.util.Log.d("VideoPlayerViewModel", "Best video: ${bestVideo?.quality} (videoOnly=${bestVideo?.videoOnly})")
                                
                                // Si el video es solo video (sin audio), necesitamos audio
                                val bestAudio = if (bestVideo?.videoOnly == true) {
                                    val validAudioStreams = details.audioStreams.filter { !it.url.isNullOrEmpty() }
                                    
                                    val systemLang = java.util.Locale.getDefault().language // e.g. "es"
                                    
                                    // 1. Preferir pista original explícita
                                    val originalAudio = validAudioStreams.find { 
                                        it.audioTrackId?.contains("orig", ignoreCase = true) == true ||
                                        it.audioTrackName?.contains("orig", ignoreCase = true) == true ||
                                        it.audioTrackId?.contains("original", ignoreCase = true) == true ||
                                        it.audioTrackName?.contains("original", ignoreCase = true) == true 
                                    }
                                    
                                    // 2. Pista del idioma del sistema
                                    val localizedAudio = validAudioStreams.find { 
                                        it.audioTrackId?.startsWith(systemLang, ignoreCase = true) == true ||
                                        it.audioTrackName?.contains(java.util.Locale.getDefault().displayLanguage, ignoreCase = true) == true ||
                                        (systemLang == "es" && it.audioTrackName?.contains("Spanish", ignoreCase = true) == true)
                                    }
                                    
                                    // 3. Pista en español genérica (fallback fuerte contra el doblaje al inglés)
                                    val esTrack = validAudioStreams.find {
                                        it.audioTrackId?.startsWith("es", ignoreCase = true) == true ||
                                        it.audioTrackName?.contains("espanol", ignoreCase = true) == true ||
                                        it.audioTrackName?.contains("español", ignoreCase = true) == true ||
                                        it.audioTrackName?.contains("spanish", ignoreCase = true) == true
                                    }
                                    
                                    // 4. Pista sin ID (suele ser el default original)
                                    val unlabeledAudio = validAudioStreams.find { 
                                        it.audioTrackId == null && it.audioTrackName == null 
                                    }
                                    
                                    originalAudio ?: localizedAudio ?: esTrack ?: unlabeledAudio ?: validAudioStreams.firstOrNull()
                                } else {
                                    null
                                }
                                
                                android.util.Log.d("VideoPlayerViewModel", "Best audio: ${bestAudio?.quality}")
                                
                                VideoPlayerUiState.Success(
                                    videoDetails = details,
                                    playerSettings = PlayerSettings(
                                        selectedQuality = bestVideo,
                                        selectedAudioTrack = bestAudio
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("VideoPlayerViewModel", "Error processing video details", e)
                                VideoPlayerUiState.Error("Error al procesar el video: ${e.message}")
                            }
                        },
                        onFailure = { exception ->
                            android.util.Log.e("VideoPlayerViewModel", "Error loading video", exception)
                            VideoPlayerUiState.Error(
                                exception.message ?: "Error al cargar el video"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Fatal error in loadVideo", e)
                _uiState.value = VideoPlayerUiState.Error("Error crítico: ${e.message}")
            }
        }
    }
    
    private fun checkIfFavorite() {
        viewModelScope.launch {
            val favorite = favoriteDao.getFavorite(videoId)
            val currentState = _uiState.value
            if (currentState is VideoPlayerUiState.Success) {
                _uiState.value = currentState.copy(isFavorite = favorite != null)
            }
        }
    }
    
    private fun checkIfSubscribed() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is VideoPlayerUiState.Success) {
                val channelId = currentState.videoDetails.uploaderUrl.substringAfterLast("/")
                val subscription = subscriptionDao.getSubscription(channelId)
                _uiState.value = currentState.copy(isSubscribed = subscription != null)
            }
        }
    }
    
    private suspend fun saveToHistory(details: VideoDetails) {
        val existingEntry = watchHistoryDao.getHistoryEntry(videoId)
        val finalViews = if (details.views > 0) details.views else (existingEntry?.viewCount ?: 0)
        
        watchHistoryDao.insertHistory(
            WatchHistoryEntity(
                videoId = videoId,
                title = details.title,
                uploaderName = details.uploader,
                thumbnail = details.thumbnailUrl,
                duration = details.duration,
                viewCount = finalViews,
                position = existingEntry?.position ?: 0,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    fun selectQuality(videoStream: VideoStream) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    selectedQuality = videoStream
                )
            )
        }
        hideSettingsSheet()
    }
    
    fun selectAudioTrack(audioStream: AudioStream) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    selectedAudioTrack = audioStream
                )
            )
        }
        hideSettingsSheet()
    }
    
    fun setPlaybackSpeed(speed: Float) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    playbackSpeed = speed
                )
            )
        }
        hideSettingsSheet()
    }
    
    fun toggleSubtitles() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    subtitlesEnabled = !currentState.playerSettings.subtitlesEnabled
                )
            )
        }
    }
    
    fun toggleFullscreen() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    isFullscreen = !currentState.playerSettings.isFullscreen
                )
            )
        }
    }
    
    fun toggleControls() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    showControls = !currentState.playerSettings.showControls
                )
            )
        }
    }
    
    fun cycleResizeMode() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            val nextMode = (currentState.playerSettings.resizeMode + 1) % 5
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    resizeMode = nextMode
                )
            )
            android.util.Log.d("VideoPlayerViewModel", "Resize mode changed to: $nextMode")
        }
    }
    
    fun setResizeMode(mode: Int) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(
                playerSettings = currentState.playerSettings.copy(
                    resizeMode = mode
                )
            )
            android.util.Log.d("VideoPlayerViewModel", "Resize mode set to: $mode")
        }
    }
    
    fun updatePlaybackPosition(position: Long) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            _uiState.value = currentState.copy(currentPosition = position)
            
            // Guardar posición en historial
            viewModelScope.launch {
                watchHistoryDao.getHistoryEntry(videoId)?.let { entry ->
                    watchHistoryDao.insertHistory(
                        entry.copy(position = position)
                    )
                }
            }
        }
    }
    
    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            viewModelScope.launch {
                if (currentState.isFavorite) {
                    favoriteDao.deleteFavoriteById(videoId)
                    _uiState.value = currentState.copy(isFavorite = false)
                } else {
                    favoriteDao.insertFavorite(
                        FavoriteEntity(
                            videoId = videoId,
                            title = currentState.videoDetails.title,
                            uploaderName = currentState.videoDetails.uploader,
                            thumbnail = currentState.videoDetails.thumbnailUrl,
                            duration = currentState.videoDetails.duration,
                            viewCount = currentState.videoDetails.views
                        )
                    )
                    _uiState.value = currentState.copy(isFavorite = true)
                }
            }
        }
    }
    
    fun toggleSubscription() {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            viewModelScope.launch {
                val channelId = currentState.videoDetails.uploaderUrl.substringAfterLast("/")
                if (currentState.isSubscribed) {
                    subscriptionDao.deleteSubscriptionById(channelId)
                    _uiState.value = currentState.copy(isSubscribed = false)
                } else {
                    subscriptionDao.insertSubscription(
                        SubscriptionEntity(
                            channelId = channelId,
                            channelName = currentState.videoDetails.uploader,
                            channelAvatar = currentState.videoDetails.uploaderAvatar,
                            subscriberCount = currentState.videoDetails.subscriberCount,
                            isVerified = currentState.videoDetails.uploaderVerified
                        )
                    )
                    _uiState.value = currentState.copy(isSubscribed = true)
                }
            }
        }
    }
    
    fun showSettingsSheet() {
        _showSettingsSheet.value = true
    }
    
    fun hideSettingsSheet() {
        _showSettingsSheet.value = false
    }
    
    private fun loadComments() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is VideoPlayerUiState.Success) {
                _uiState.value = currentState.copy(isLoadingComments = true)
            }
            
            videoRepository.getComments(videoId).collect { result ->
                val state = _uiState.value
                if (state is VideoPlayerUiState.Success) {
                    result.fold(
                        onSuccess = { triple ->
                            val (commentsList, nextPage, totalCount) = triple
                            _uiState.value = state.copy(
                                comments = commentsList,
                                commentsNextPage = nextPage,
                                commentsCount = totalCount,
                                isLoadingComments = false
                            )
                        },
                        onFailure = { exception ->
                            android.util.Log.e("VideoPlayerViewModel", "Error loading comments", exception)
                            _uiState.value = state.copy(
                                comments = emptyList(),
                                isLoadingComments = false
                            )
                        }
                    )
                }
            }
        }
    }
    
    fun loadMoreComments() {
        val state = _uiState.value
        if (state !is VideoPlayerUiState.Success) return
        if (state.isLoadingComments) return
        val nextPage = state.commentsNextPage ?: return
        
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingComments = true)
            videoRepository.getMoreComments(videoId, nextPage).collect { result ->
                val currentState = _uiState.value
                if (currentState is VideoPlayerUiState.Success) {
                    result.fold(
                        onSuccess = { pair ->
                            val (newComments, newNextPage) = pair
                            // Check if the current page returned no comments to avoid infinite dummy loads
                            if (newComments.isEmpty() && newNextPage == nextPage) {
                                _uiState.value = currentState.copy(
                                    commentsNextPage = null,
                                    isLoadingComments = false
                                )
                            } else {
                                _uiState.value = currentState.copy(
                                    comments = currentState.comments + newComments,
                                    commentsNextPage = newNextPage,
                                    isLoadingComments = false
                                )
                            }
                        },
                        onFailure = { exception ->
                            android.util.Log.e("VideoPlayerViewModel", "Error loading more comments", exception)
                            _uiState.value = currentState.copy(isLoadingComments = false)
                        }
                    )
                }
            }
        }
    }
    
    fun loadReplies(commentId: String, repliesPage: String) {
        val state = _uiState.value
        if (state !is VideoPlayerUiState.Success) return
        
        // Don't reload if already loaded or loading
        if (state.replies.containsKey(commentId) || state.loadingReplies.contains(commentId)) return
        
        viewModelScope.launch {
            // Set loading state
            _uiState.value = state.copy(
                loadingReplies = state.loadingReplies + commentId
            )
            
            videoRepository.getCommentReplies(videoId, repliesPage).collect { result ->
                val currentState = _uiState.value
                if (currentState is VideoPlayerUiState.Success) {
                    result.fold(
                        onSuccess = { repliesList ->
                            android.util.Log.d("VideoPlayerViewModel", "Loaded ${repliesList.size} replies for comment $commentId")
                            _uiState.value = currentState.copy(
                                replies = currentState.replies + (commentId to repliesList),
                                loadingReplies = currentState.loadingReplies - commentId
                            )
                        },
                        onFailure = { exception ->
                            android.util.Log.e("VideoPlayerViewModel", "Error loading replies", exception)
                            _uiState.value = currentState.copy(
                                loadingReplies = currentState.loadingReplies - commentId
                            )
                        }
                    )
                }
            }
        }
    }
    
    fun isCurrentVideo(checkVideoId: String): Boolean {
        return playerManager.currentVideoId.value == checkVideoId
    }

    // SponsorBlock Logic
    private var segments: List<com.opentube.data.models.Segment> = emptyList()
    private var sponsorBlockJob: kotlinx.coroutines.Job? = null

    private fun startSponsorBlockMonitor() {
        sponsorBlockJob?.cancel()
        sponsorBlockJob = viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    com.opentube.player.PlayerHelper.getCurrentSegment(currentPos, segments)?.let { segment ->
                        // Skip segment
                        player.seekTo((segment.end * 1000).toLong())
                        segment.skipped = true
                        
                        // Show toast or message (optional)
                        android.util.Log.d("SponsorBlock", "Skipped segment: ${segment.category}")
                    }
                }
                kotlinx.coroutines.delay(500) // Check every 500ms
            }
        }
    }

    fun onPlaybackStarted() {
        playerManager.setVideoId(videoId)
    }

    fun updateRelatedVideos(newVideos: List<com.opentube.data.models.Video>) {
        val currentState = _uiState.value
        if (currentState is VideoPlayerUiState.Success) {
            val updatedDetails = currentState.videoDetails.copy(relatedStreams = newVideos)
            _uiState.value = currentState.copy(videoDetails = updatedDetails)
        }
    }

    fun loadRelatedVideosForTag(query: String) {
        viewModelScope.launch {
            try {
                videoRepository.search(query).collect { result ->
                    if (result.isSuccess) {
                        val searchResults = result.getOrNull()
                        val videos = searchResults?.items?.filter { it.type == "stream" }?.map { item ->
                            val parsedVideoId = if (item.url.contains("?v=")) item.url.substringAfterLast("?v=") else item.url.substringAfterLast("/")
                            com.opentube.data.models.Video(
                                url = item.url,
                                title = item.title ?: item.name ?: "",
                                thumbnail = item.thumbnail,
                                uploaderName = item.uploaderName ?: "",
                                uploaderUrl = item.uploaderUrl,
                                uploaderAvatar = item.uploaderAvatar,
                                uploadedDate = item.uploadedDate,
                                duration = item.duration ?: 0L,
                                views = item.views ?: 0L,
                                uploaderVerified = item.uploaderVerified ?: false,
                                isShort = false
                            )
                        } ?: emptyList()

                        val currentState = _uiState.value
                        if (currentState is VideoPlayerUiState.Success) {
                            val updatedDetails = currentState.videoDetails.copy(relatedStreams = videos)
                            _uiState.value = currentState.copy(videoDetails = updatedDetails)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sponsorBlockJob?.cancel()
    }
}


