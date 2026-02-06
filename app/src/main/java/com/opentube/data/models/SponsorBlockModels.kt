package com.opentube.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SegmentData(
    val segments: List<Segment>
)

@Serializable
data class Segment(
    val category: String,
    val actionType: String,
    val segment: List<Float>,
    val UUID: String,
    val locked: Int,
    val votes: Int,
    val videoDuration: Float
) {
    val start: Float
        get() = segment.firstOrNull() ?: 0f
    
    val end: Float
        get() = segment.lastOrNull() ?: 0f
        
    var skipped: Boolean = false
    
    // Helper to get start/end pair
    val segmentStartAndEnd: Pair<Float, Float>
        get() = (segment.firstOrNull() ?: 0f) to (segment.lastOrNull() ?: 0f)
}

@Serializable
data class SearchResult(
    val items: List<SearchItem>,
    val nextpage: String? = null,
    val suggestion: String? = null,
    val corrected: Boolean = false
)


