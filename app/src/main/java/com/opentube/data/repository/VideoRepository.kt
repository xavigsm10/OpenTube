package com.opentube.data.repository

import com.opentube.data.api.PipedApiService
import com.opentube.data.extractor.NewPipeHelper
import com.opentube.data.models.*
import com.opentube.data.models.Segment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.opentube.data.extractor.PagedResult

/**
 * Repository for video operations
 * Ahora usa NewPipe Extractor para obtener videos directamente de YouTube
 */
@Singleton
class VideoRepository @Inject constructor(
    private val newPipeHelper: NewPipeHelper,
    private val pipedApi: PipedApiService
) {
    
    /**
     * Get trending videos usando NewPipe
     */
    /**
     * Get trending videos usando NewPipe (Paged)
     */
    fun getTrendingPaged(pageUrl: String? = null): Flow<Result<PagedResult<Video>>> = flow {
        val result = newPipeHelper.getTrendingVideosPaged(pageUrl)
        emit(result)
    }

    /**
     * Get trending videos usando NewPipe (Legacy)
     */
    fun getTrending(region: String = "US"): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getTrendingVideos()
        emit(result)
    }
    
    /**
     * Get gaming videos
     */
    fun getGamingVideos(): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getGamingVideos()
        emit(result)
    }
    
    /**
     * Get music videos
     */
    fun getMusicVideos(): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getMusicVideos()
        emit(result)
    }
    
    /**
     * Get sports videos
     */
    fun getSportsVideos(): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getSportsVideos()
        emit(result)
    }
    
    /**
     * Get live videos
     */
    fun getLiveVideos(): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getLiveVideos()
        emit(result)
    }
    
    /**
     * Get shorts (videos cortos de menos de 60 segundos)
     */
    fun getShorts(page: Int = 0): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getShorts(page)
        emit(result)
    }
    
    /**
     * Get playlists by query
     */
    fun getPlaylists(query: String): Flow<Result<List<Playlist>>> = flow {
        val result = newPipeHelper.searchPlaylists(query)
        emit(result)
    }

    /**
     * Get albums by query
     */
    fun getAlbums(query: String): Flow<Result<List<Album>>> = flow {
        val result = newPipeHelper.searchAlbums(query)
        emit(result)
    }

    /**
     * Get playlist details (videos)
     */
    fun getPlaylistDetails(url: String): Flow<Result<List<Video>>> = flow {
        val result = newPipeHelper.getPlaylistDetails(url)
        emit(result)
    }
    
    /**
     * Get video details usando NewPipe
     */
    // Cache en memoria para VideoDetails (para transiciones rápidas)
    private var cachedVideoDetails: VideoDetails? = null

    /**
     * Get video details usando NewPipe
     */
    fun getVideoDetails(videoId: String): Flow<Result<VideoDetails>> = flow {
        android.util.Log.d("VideoRepository", "=== getVideoDetails() CALLED for videoId: $videoId ===")
        
        // 1. Emitir caché si existe y coincide
        cachedVideoDetails?.let { cached ->
            if (cached.videoId == videoId) {
                android.util.Log.d("VideoRepository", "Returning CACHED video details for: $videoId")
                emit(Result.success(cached))
            }
        }
        
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            android.util.Log.d("VideoRepository", "Fetching StreamInfo from NewPipe for URL: $url")
            
            // ⚠️ CRITICAL: Execute NewPipe call on IO thread to avoid blocking UI
            val streamInfo = withContext(Dispatchers.IO) {
                org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                    org.schabi.newpipe.extractor.ServiceList.YouTube, 
                    url
                )
            }
            
            android.util.Log.d("VideoRepository", "StreamInfo fetched successfully!")
            android.util.Log.d("VideoRepository", "NewPipe videoOnlyStreams count: ${streamInfo.videoOnlyStreams.size}")
            android.util.Log.d("VideoRepository", "NewPipe videoStreams (progressive) count: ${streamInfo.videoStreams.size}")
            
            // Approach similar to LibreTube: combine DASH (videoOnly) + Progressive (video+audio) streams
            // DashHelper will filter only DASH-compatible streams when creating manifest
            val dashStreams = streamInfo.videoOnlyStreams.map { stream ->
                VideoStream(
                    url = stream.content ?: "",
                    format = stream.format?.name ?: "mp4",
                    quality = "${stream.height}p",
                    mimeType = stream.format?.mimeType ?: "video/mp4",
                    codec = stream.codec,
                    videoOnly = true,  // DASH stream - needs separate audio
                    bitrate = stream.bitrate,
                    initStart = stream.initStart?.toInt() ?: 0,
                    initEnd = stream.initEnd?.toInt() ?: 0,
                    indexStart = stream.indexStart?.toInt() ?: 0,
                    indexEnd = stream.indexEnd?.toInt() ?: 0,
                    width = stream.width,
                    height = stream.height,
                    fps = stream.fps
                )
            }
            
            val progressiveStreams = streamInfo.videoStreams.map { stream ->
                VideoStream(
                    url = stream.content ?: "",
                    format = stream.format?.name ?: "mp4",
                    quality = "${stream.height}p",
                    mimeType = stream.format?.mimeType ?: "video/mp4",
                    codec = stream.codec,
                    videoOnly = false,  // Progressive stream - video+audio combined
                    bitrate = stream.bitrate,
                    initStart = 0,
                    initEnd = 0,
                    indexStart = 0,
                    indexEnd = 0,
                    width = stream.width,
                    height = stream.height,
                    fps = stream.fps
                )
            }
            
            val videoStreams = dashStreams + progressiveStreams
            
            android.util.Log.d("VideoRepository", "Converted DASH streams: ${dashStreams.size}")
            android.util.Log.d("VideoRepository", "Converted Progressive streams: ${progressiveStreams.size}")
            android.util.Log.d("VideoRepository", "Total video streams (DASH + Progressive): ${videoStreams.size}")
            
            // Get all audio streams - similar to LibreTube approach
            // DashHelper will filter only those with proper indexing
            val audioStreams = streamInfo.audioStreams.map { stream ->
                // Extract locale info
                val locale = stream.locale
                val languageCode = locale?.language
                // Get display language in Spanish (user preference)
                val displayLanguage = locale?.getDisplayLanguage(java.util.Locale("es", "ES"))?.replaceFirstChar { it.uppercase() }
                
                AudioStream(
                    url = stream.content ?: "",
                    format = stream.format?.name ?: "m4a",
                    quality = "${stream.averageBitrate / 1000}kbps",
                    mimeType = stream.format?.mimeType ?: "audio/mp4",
                    codec = stream.codec,
                    // Use language code as ID if available, otherwise null
                    audioTrackId = languageCode,
                    // Use display language -> specific track name -> format name
                    audioTrackName = displayLanguage ?: stream.audioTrackName, 
                    bitrate = stream.averageBitrate,
                    initStart = stream.initStart?.toInt() ?: 0,
                    initEnd = stream.initEnd?.toInt() ?: 0,
                    indexStart = stream.indexStart?.toInt() ?: 0,
                    indexEnd = stream.indexEnd?.toInt() ?: 0
                )
            }
            
            android.util.Log.d("VideoRepository", "Total audio streams: ${audioStreams.size}")
            android.util.Log.d("VideoRepository", "Video duration from NewPipe: ${streamInfo.duration} seconds")
            android.util.Log.d("VideoRepository", "HLS URL: ${streamInfo.hlsUrl ?: "Not available"}")
            
            val details = VideoDetails(
                title = streamInfo.name,
                description = streamInfo.description?.content ?: "",
                uploadDate = streamInfo.uploadDate?.offsetDateTime()?.toString() ?: "",
                uploader = streamInfo.uploaderName,
                uploaderUrl = streamInfo.uploaderUrl,
                uploaderAvatar = streamInfo.uploaderAvatars.maxByOrNull { it.height }?.url ?: "",
                uploaderVerified = streamInfo.isUploaderVerified,
                subscriberCount = streamInfo.uploaderSubscriberCount,
                likes = streamInfo.likeCount,
                dislikes = streamInfo.dislikeCount,
                duration = streamInfo.duration,
                views = streamInfo.viewCount,
                category = streamInfo.category ?: "",
                thumbnailUrl = streamInfo.thumbnails.maxByOrNull { it.height }?.url ?: "",
                videoStreams = videoStreams,
                audioStreams = audioStreams,
                relatedStreams = streamInfo.relatedItems
                    .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                    .take(10)
                    .map { item ->
                        Video(
                            url = item.url,
                            title = item.name,
                            thumbnail = item.thumbnails.maxByOrNull { it.height }?.url ?: "",
                            uploaderName = item.uploaderName ?: "",
                            uploaderUrl = item.uploaderUrl,
                            uploaderAvatar = item.uploaderAvatars.maxByOrNull { it.height }?.url,
                            uploadedDate = item.uploadDate?.offsetDateTime()?.toString(),
                            duration = item.duration,
                            views = item.viewCount,
                            uploaderVerified = item.isUploaderVerified,
                            isShort = item.duration < 60,
                // Improved live detection: Trust HLS presence if streamType is ambiguous
                isLive = streamInfo.streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM || !streamInfo.hlsUrl.isNullOrEmpty()
            )
        },
    liveNow = streamInfo.streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM || !streamInfo.hlsUrl.isNullOrEmpty(),
    hlsUrl = streamInfo.hlsUrl,
    dashUrl = streamInfo.dashMpdUrl,
    videoId = videoId
            )
            
            // Actualizar caché
            cachedVideoDetails = details
            
            emit(Result.success(details))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Search for content usando NewPipe
     */
    /**
     * Search for content usando NewPipe (Paged)
     */
    fun searchPaged(
        query: String,
        pageUrl: String? = null
    ): Flow<Result<PagedResult<Video>>> = flow {
        val result = newPipeHelper.searchVideosPaged(query, pageUrl)
        emit(result)
    }

    /**
     * Search for content usando NewPipe (Legacy)
     */
    fun search(
        query: String,
        filter: String = "all"
    ): Flow<Result<SearchResults>> = flow {
        val result = newPipeHelper.searchVideos(query)
        if (result.isSuccess) {
            val videos = result.getOrThrow()
            val searchResults = SearchResults(
                items = videos.map { video ->
                    SearchItem(
                        url = video.url,
                        type = "stream",
                        title = video.title,
                        name = video.title,
                        thumbnail = video.thumbnail,
                        uploaderName = video.uploaderName,
                        uploaderUrl = video.uploaderUrl,
                        uploaderAvatar = video.uploaderAvatar,
                        uploaderVerified = video.uploaderVerified,
                        uploadedDate = video.uploadedDate,
                        duration = video.duration,
                        views = video.views,
                        description = "",
                        subscribers = null,
                        videos = null,
                        verified = video.uploaderVerified
                    )
                },
                nextPage = null, // TODO: Use paged result if needed here
                suggestion = null,
                corrected = false
            )
            emit(Result.success(searchResults))
        } else {
            emit(Result.failure(result.exceptionOrNull() ?: Exception("Error en búsqueda")))
        }
    }
    
    /**
     * Load next page of search results
     */
    fun searchNextPage(
        query: String,
        filter: String = "all",
        nextPage: String
    ): Flow<Result<SearchResults>> = flow {
        // NewPipe no maneja paginación de la misma forma, retornar lista vacía
        emit(Result.success(SearchResults(emptyList(), null, null, false)))
    }
    
    /**
     * Get search suggestions usando NewPipe
     */
    fun getSuggestions(query: String): Flow<Result<List<String>>> = flow {
        try {
            val result = newPipeHelper.getSearchSuggestions(query)
            if (result.isSuccess) {
                emit(Result.success(result.getOrThrow()))
            } else {
                emit(Result.failure(result.exceptionOrNull() ?: Exception("Error obteniendo sugerencias")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get video comments usando NewPipe
     */
    fun getComments(videoId: String): Flow<Result<List<com.opentube.ui.screens.player.Comment>>> = flow {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            android.util.Log.d("VideoRepository", "Fetching comments for video: $videoId")
            
            val streamInfo = withContext(Dispatchers.IO) {
                try {
                    org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
                        org.schabi.newpipe.extractor.ServiceList.YouTube, 
                        url
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepository", "Error getting stream info for comments", e)
                    throw e
                }
            }
            
            // Obtener los comentarios
            val commentsInfo = withContext(Dispatchers.IO) {
                try {
                    org.schabi.newpipe.extractor.comments.CommentsInfo.getInfo(
                        org.schabi.newpipe.extractor.ServiceList.YouTube,
                        streamInfo.url
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepository", "Error getting comments info", e)
                    // Si los comentarios están deshabilitados, retornar lista vacía en lugar de fallar
                    return@withContext null
                }
            }
            
            if (commentsInfo == null) {
                android.util.Log.d("VideoRepository", "Comments are disabled or unavailable")
                emit(Result.success(emptyList()))
                return@flow
            }
            
            android.util.Log.d("VideoRepository", "Comments fetched: ${commentsInfo.relatedItems.size}")
            
            // Convertir los comentarios de NewPipe a nuestro modelo
            val comments = commentsInfo.relatedItems.mapNotNull { commentInfo ->
                try {
                    // Serializar la página de respuestas si existe
                    val repliesPageSerialized = try {
                        commentInfo.replies?.let { repliesPage ->
                            com.opentube.data.extractor.PagedResult.serializePage(repliesPage)
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    com.opentube.ui.screens.player.Comment(
                        id = commentInfo.commentId ?: "",
                        author = commentInfo.uploaderName ?: "Usuario desconocido",
                        authorAvatar = commentInfo.uploaderAvatars.firstOrNull()?.url ?: "",
                        text = commentInfo.commentText?.content ?: "",
                        likes = commentInfo.likeCount.toLong(),
                        publishedTime = formatCommentDate(commentInfo.uploadDate),
                        isVerified = commentInfo.isUploaderVerified,
                        replyCount = commentInfo.replyCount,
                        repliesPage = repliesPageSerialized
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepository", "Error parsing comment", e)
                    null
                }
            }
            
            emit(Result.success(comments))
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error fetching comments", e)
            // Emitir lista vacía en lugar de error para que la UI muestre "No hay comentarios"
            emit(Result.success(emptyList()))
        }
    }

    /**
     * Formatea una fecha de comentario a formato relativo en español
     * Ejemplo: "hace 2 días", "hace 3 semanas", "hace 1 año"
     */
    private fun formatCommentDate(dateWrapper: org.schabi.newpipe.extractor.localization.DateWrapper?): String {
        if (dateWrapper == null) return ""
        
        try {
            // Obtener el instante de tiempo del DateWrapper
            val commentTime = dateWrapper.offsetDateTime()
            if (commentTime == null) return ""
            
            // Calcular la diferencia de tiempo
            val now = java.time.OffsetDateTime.now()
            val duration = java.time.Duration.between(commentTime, now)
            
            val seconds = duration.seconds
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7
            val months = days / 30
            val years = days / 365
            
            return when {
                years > 0 -> if (years == 1L) "hace 1 año" else "hace $years años"
                months > 0 -> if (months == 1L) "hace 1 mes" else "hace $months meses"
                weeks > 0 -> if (weeks == 1L) "hace 1 semana" else "hace $weeks semanas"
                days > 0 -> if (days == 1L) "hace 1 día" else "hace $days días"
                hours > 0 -> if (hours == 1L) "hace 1 hora" else "hace $hours horas"
                minutes > 0 -> if (minutes == 1L) "hace 1 minuto" else "hace $minutes minutos"
                else -> if (seconds <= 10) "ahora" else "hace $seconds segundos"
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error formatting comment date", e)
            return ""
        }
    }
    /**
     * Get SponsorBlock segments
     */
    fun getSegments(videoId: String): Flow<Result<List<Segment>>> = flow {
        try {
            val segmentData = pipedApi.getSegments(videoId)
            emit(Result.success(segmentData.segments))
        } catch (e: Exception) {
            // It's okay if it fails (404 usually means no segments)
            emit(Result.success(emptyList()))
        }
    }
    
    /**
     * Get comment replies using the serialized Page
     */
    fun getCommentReplies(videoId: String, serializedPage: String): Flow<Result<List<com.opentube.ui.screens.player.Comment>>> = flow {
        try {
            val page = com.opentube.data.extractor.PagedResult.deserializePage(serializedPage)
            if (page == null) {
                emit(Result.failure(Exception("Invalid page data")))
                return@flow
            }
            
            val url = "https://www.youtube.com/watch?v=$videoId"
            val repliesInfo = withContext(Dispatchers.IO) {
                try {
                    org.schabi.newpipe.extractor.comments.CommentsInfo.getMoreItems(
                        org.schabi.newpipe.extractor.ServiceList.YouTube,
                        url,
                        page
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepository", "Error getting comment replies", e)
                    null
                }
            }
            
            if (repliesInfo == null) {
                emit(Result.success(emptyList()))
                return@flow
            }
            
            val replies = repliesInfo.items.mapNotNull { replyInfo ->
                try {
                    com.opentube.ui.screens.player.Comment(
                        id = replyInfo.commentId ?: "",
                        author = replyInfo.uploaderName ?: "Usuario desconocido",
                        authorAvatar = replyInfo.uploaderAvatars.firstOrNull()?.url ?: "",
                        text = replyInfo.commentText?.content ?: "",
                        likes = replyInfo.likeCount.toLong(),
                        publishedTime = formatCommentDate(replyInfo.uploadDate),
                        isVerified = replyInfo.isUploaderVerified,
                        replyCount = 0, // Replies don't have nested replies
                        repliesPage = null
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VideoRepository", "Error parsing reply", e)
                    null
                }
            }
            
            emit(Result.success(replies))
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error fetching replies", e)
            emit(Result.success(emptyList()))
        }
    }
}
