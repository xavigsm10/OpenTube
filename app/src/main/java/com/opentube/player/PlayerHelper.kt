package com.opentube.player

import com.opentube.data.models.Segment
import com.opentube.data.models.SegmentData

object PlayerHelper {
    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"
    
    // Default categories to skip
    val defaultCategories = listOf(
        "sponsor", 
        "selfpromo", 
        "interaction", 
        "intro", 
        "outro", 
        "preview", 
        "music_offtopic", 
        "filler"
    )

    fun getCurrentSegment(
        currentPosition: Long,
        segments: List<Segment>
    ): Segment? {
        val positionSeconds = currentPosition / 1000f
        
        // Find segment that contains current position
        // Ignoring highlight category for skipping
        return segments
            .filter { it.category != SPONSOR_HIGHLIGHT_CATEGORY }
            .firstOrNull { segment ->
                val (start, end) = segment.segmentStartAndEnd
                
                // Avoid re-skipping if we are very close to end (debounce)
                // But for simplicity, just check if we are inside
                positionSeconds >= start && positionSeconds < end && !segment.skipped
            }
    }
}
