package com.opentube.data.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Channel information
 */
data class Channel(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatarUrl")
    val avatarUrl: String,
    
    @SerializedName("bannerUrl")
    val bannerUrl: String?,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("subscriberCount")
    val subscriberCount: Long,
    
    @SerializedName("verified")
    val verified: Boolean,
    
    @SerializedName("relatedStreams")
    val videos: List<Video> = emptyList(),
    
    @SerializedName("nextpage")
    val nextPage: String?
)

/**
 * Playlist information
 */
data class PipedPlaylist(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String,
    
    @SerializedName("uploaderName")
    val uploaderName: String?,
    
    @SerializedName("uploaderUrl")
    val uploaderUrl: String?,
    
    @SerializedName("uploaderAvatar")
    val uploaderAvatar: String?,
    
    @SerializedName("videos")
    val videos: Int,
    
    @SerializedName("relatedStreams")
    val videoList: List<Video> = emptyList(),
    
    @SerializedName("nextpage")
    val nextPage: String?
)

/**
 * Search results
 */
data class SearchResults(
    @SerializedName("items")
    val items: List<SearchItem>,
    
    @SerializedName("nextpage")
    val nextPage: String?,
    
    @SerializedName("suggestion")
    val suggestion: String?,
    
    @SerializedName("corrected")
    val corrected: Boolean
)



/**
 * Search item (can be video, channel, or playlist)
 */
@Serializable
data class SearchItem(
    @SerializedName("url")
    val url: String,
    
    @SerializedName("type")
    val type: String, // "stream", "channel", "playlist"
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("thumbnail")
    val thumbnail: String,
    
    @SerializedName("uploaderName")
    val uploaderName: String?,
    
    @SerializedName("uploaderUrl")
    val uploaderUrl: String?,
    
    @SerializedName("uploaderAvatar")
    val uploaderAvatar: String?,
    
    @SerializedName("uploaderVerified")
    val uploaderVerified: Boolean?,
    
    @SerializedName("uploadedDate")
    val uploadedDate: String?,
    
    @SerializedName("duration")
    val duration: Long?,
    
    @SerializedName("views")
    val views: Long?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("subscribers")
    val subscribers: Long?,
    
    @SerializedName("videos")
    val videos: Int?,
    
    @SerializedName("verified")
    val verified: Boolean?
)

/**
 * Trending videos response
 */
data class TrendingResponse(
    @SerializedName("items")
    val items: List<Video>
)
