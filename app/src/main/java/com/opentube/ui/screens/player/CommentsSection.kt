package com.opentube.ui.screens.player

import android.text.Html
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

data class Comment(
    val id: String,
    val author: String,
    val authorAvatar: String,
    val text: String,
    val likes: Long,
    val publishedTime: String,
    val isVerified: Boolean,
    val replyCount: Int,
    val repliesPage: String? = null // Serialized Page for loading replies
)

@Composable
fun CommentsSection(
    comments: List<Comment>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onLoadReplies: ((String, String) -> Unit)? = null, // (commentId, repliesPage) -> Unit
    replies: Map<String, List<Comment>> = emptyMap(), // commentId -> replies
    loadingReplies: Set<String> = emptySet(), // Set of comment IDs currently loading
    modifier: Modifier = Modifier
) {
    var showComments by remember { mutableStateOf(true) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Encabezado con botón para mostrar/ocultar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showComments = !showComments }
                .padding(bottom = if (showComments) 16.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comentarios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (comments.isNotEmpty()) {
                    Text(
                        text = formatCount(comments.size.toLong()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = if (showComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showComments) "Ocultar comentarios" else "Mostrar comentarios",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Contenido de comentarios (solo si showComments es true)
        if (showComments) {
            if (isLoading && comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay comentarios disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Lista de comentarios
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comments.take(5).forEach { comment ->
                        CommentItem(
                            comment = comment,
                            replies = replies[comment.id] ?: emptyList(),
                            isLoadingReplies = comment.id in loadingReplies,
                            onLoadReplies = onLoadReplies
                        )
                    }
                    
                    if (comments.size > 5) {
                        FilledTonalButton(
                            onClick = onLoadMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text("Ver más comentarios (${comments.size - 5})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    replies: List<Comment> = emptyList(),
    isLoadingReplies: Boolean = false,
    onLoadReplies: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isReply: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var showReplies by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isReply) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar del autor
            AsyncImage(
                model = comment.authorAvatar,
                contentDescription = "Avatar de ${comment.author}",
                modifier = Modifier
                    .size(if (isReply) 36.dp else 48.dp)
                    .clip(CircleShape)
            )
            
            // Contenido del comentario
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Autor y tiempo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (comment.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = comment.publishedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Texto del comentario (limpiar HTML)
                val cleanText = remember(comment.text) {
                    Html.fromHtml(comment.text, Html.FROM_HTML_MODE_COMPACT).toString().trim()
                }
                
                Text(
                    text = cleanText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Ver más/menos si el texto es largo
                if (cleanText.length > 150) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (expanded) "Ver menos" else "Ver más",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Likes y respuestas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Me gusta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        if (comment.likes > 0) {
                            Text(
                                text = formatCount(comment.likes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Botón de respuestas (solo para comentarios principales)
                    if (comment.replyCount > 0 && !isReply) {
                        AssistChip(
                            onClick = { 
                                if (!showReplies && replies.isEmpty() && comment.repliesPage != null && onLoadReplies != null) {
                                    // Cargar respuestas si aún no se han cargado
                                    onLoadReplies(comment.id, comment.repliesPage)
                                }
                                showReplies = !showReplies 
                            },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (showReplies) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${comment.replyCount} ${if (comment.replyCount == 1) "respuesta" else "respuestas"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        )
                    }
                }
                
                // Sección de respuestas
                AnimatedVisibility(
                    visible = showReplies && !isReply,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoadingReplies) {
                            // Cargando respuestas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cargando respuestas...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (replies.isEmpty()) {
                            // No hay respuestas cargadas pero hay página disponible
                            if (comment.repliesPage != null && onLoadReplies != null) {
                                TextButton(
                                    onClick = { onLoadReplies(comment.id, comment.repliesPage) }
                                ) {
                                    Text("Cargar ${comment.replyCount} respuestas")
                                }
                            } else {
                                Text(
                                    text = "No se pudieron cargar las respuestas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            // Mostrar respuestas
                            replies.forEach { reply ->
                                ReplyItem(reply = reply)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: Comment,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar pequeño
            AsyncImage(
                model = reply.authorAvatar,
                contentDescription = "Avatar de ${reply.author}",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Autor y tiempo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reply.author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (reply.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verificado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    Text(
                        text = reply.publishedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Texto de la respuesta
                val cleanText = remember(reply.text) {
                    Html.fromHtml(reply.text, Html.FROM_HTML_MODE_COMPACT).toString().trim()
                }
                
                Text(
                    text = cleanText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (cleanText.length > 100) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (expanded) "Ver menos" else "Ver más",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                // Likes
                if (reply.likes > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Me gusta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatCount(reply.likes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
