package com.opentube.data.api

import com.opentube.data.models.Channel
import com.opentube.data.models.Playlist
import com.opentube.data.models.SearchResult
import com.opentube.data.models.SegmentData
import com.opentube.data.models.Video
import com.opentube.data.models.VideoDetails
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Piped API Service
 * Official Piped API documentation: https://docs.piped.video/docs/api-documentation/
 */
interface PipedApiService {
    
    /**
     * Get trending videos
     * @param region Region code (e.g., "US", "GB", "MX")
     */
    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String = "US"
    ): List<Video>
    
    /**
     * Get video details by video ID
     * @param videoId YouTube video ID
     */
    @GET("streams/{videoId}")
    suspend fun getVideoDetails(
        @Path("videoId") videoId: String
    ): VideoDetails
    
    /**
     * Get SponsorBlock segments for a video
     * @param videoId YouTube video ID
     * @param category JSON array of categories to fetch
     * @param actionType JSON array of action types
     */
    @GET("sponsors/{videoId}")
    suspend fun getSegments(
        @Path("videoId") videoId: String,
        @Query("category") category: String = "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"preview\",\"music_offtopic\",\"filler\"]",
        @Query("actionType") actionType: String? = null
    ): SegmentData

    /**
     * Search for videos, channels, and playlists
     * @param query Search query
     * @param filter Filter type: "all", "videos", "channels", "playlists"
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "all"
    ): SearchResult // Changed from SearchResults (local model) to SearchResult (LibreTube model)
    
    /**
     * Get next page of search results
     * @param query Original search query
     * @param filter Filter type
     * @param nextpage Next page token
     */
    @GET("nextpage/search") // Fixed endpoint path from "search" to "nextpage/search"
    suspend fun searchNextPage(
        @Query("q") query: String,
        @Query("filter") filter: String = "all",
        @Query("nextpage") nextpage: String
    ): SearchResult
    
    /**
     * Get channel information
     * @param channelId Channel ID or URL
     */
    @GET("channel/{channelId}")
    suspend fun getChannel(
        @Path("channelId") channelId: String
    ): Channel
    
    /**
     * Get next page of channel videos
     * @param channelId Channel ID
     * @param nextpage Next page token
     */
    @GET("channel/{channelId}")
    suspend fun getChannelNextPage(
        @Path("channelId") channelId: String,
        @Query("nextpage") nextpage: String
    ): Channel
    
    /**
     * Get playlist details
     * @param playlistId Playlist ID
     */
    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(
        @Path("playlistId") playlistId: String
    ): Playlist
    
    /**
     * Get next page of playlist videos
     * @param playlistId Playlist ID
     * @param nextpage Next page token
     */
    @GET("playlists/{playlistId}")
    suspend fun getPlaylistNextPage(
        @Path("playlistId") playlistId: String,
        @Query("nextpage") nextpage: String
    ): Playlist
    
    /**
     * Get search suggestions
     * @param query Partial search query
     */
    @GET("suggestions")
    suspend fun getSuggestions(
        @Query("query") query: String
    ): List<String>
}
