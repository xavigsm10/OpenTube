package com.opentube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opentube.data.models.Video
import com.opentube.utils.formatDuration
import com.opentube.utils.formatViews

import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Video card component
 */
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClickWithRect: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    var boundsInWindow by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                boundsInWindow = coordinates.boundsInWindow()
            }
            .clickable(onClick = {
                if (onClickWithRect != null && boundsInWindow != null) {
                    onClickWithRect(boundsInWindow!!)
                } else {
                    onClick()
                }
            })
            .padding(vertical = 8.dp)
    ) {
        // Thumbnail with duration overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder color
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // LIVE badge (appears if video is live)
            if (video.isLive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    color = androidx.compose.ui.graphics.Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "● EN VIVO",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            } else if (video.duration > 0) {
                // Duration badge (only if not live)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else if (video.duration == 0L || video.duration < 0) {
                // For live streams with no duration info
                if (video.isLive) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFFF6B6B).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "EN DIRECTO",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Video info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Uploader avatar
            AsyncImage(
                model = video.uploaderAvatar,
                contentDescription = video.uploaderName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape), // Use CircleShape for a perfect circle
                contentScale = ContentScale.Crop
            )
            
            // Title and info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = buildString {
                        append(video.uploaderName)
                        if (video.uploaderVerified) append(" ✓")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = buildString {
                        if (video.views >= 0) {
                            append(formatViews(video.views))
                            append(" • ")
                        }
                        append(com.opentube.util.DateFormatter.formatUploadDate(video.uploadedDate))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact video item for lists
 */
@Composable
fun VideoListItem(
    video: Video,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClickWithRect: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    var boundsInWindow by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                boundsInWindow = coordinates.boundsInWindow()
            }
            .clickable(onClick = {
                if (onClickWithRect != null && boundsInWindow != null) {
                    onClickWithRect(boundsInWindow!!)
                } else {
                    onClick()
                }
            })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = formatDuration(video.duration),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = video.uploaderName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            
            Text(
                text = buildString {
                    if (video.views >= 0) {
                        append(formatViews(video.views))
                        append(" • ")
                    }
                    append(com.opentube.util.DateFormatter.formatUploadDate(video.uploadedDate))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
