package com.opentube.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.opentube.data.models.AudioStream
import com.opentube.data.models.VideoStream
import com.opentube.data.models.SubtitleStream

import androidx.compose.material.icons.filled.Timer

enum class SettingsTab {
    MAIN,
    QUALITY,
    SPEED,
    AUDIO,
    SUBTITLES,
    SLEEP_TIMER
}

@Composable
fun PlayerSettingsSheet(
    videoStreams: List<VideoStream>,
    audioStreams: List<AudioStream>,
    subtitleStreams: List<SubtitleStream>,
    currentQuality: VideoStream?,
    currentAudioTrack: AudioStream?,
    currentSubtitleTrack: SubtitleStream?,
    currentSpeed: Float,
    subtitlesEnabled: Boolean,
    musicModeEnabled: Boolean,
    onQualitySelected: (VideoStream) -> Unit,
    onAudioSelected: (AudioStream) -> Unit,
    onSubtitleSelected: (SubtitleStream?) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSleepTimerSelected: (Long) -> Unit = {}, // New parameter with default for backward compatibility
    onSubtitlesToggle: () -> Unit,
    onMusicModeToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(SettingsTab.MAIN) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Header
                Text(
                    text = when (currentTab) {
                        SettingsTab.MAIN -> "Configuración"
                        SettingsTab.QUALITY -> "Calidad"
                        SettingsTab.SPEED -> "Velocidad"
                        SettingsTab.AUDIO -> "Pista de audio"
                        SettingsTab.SUBTITLES -> "Subtítulos"
                        SettingsTab.SLEEP_TIMER -> "Temporizador"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                Divider()
                
                // Content
                when (currentTab) {
                    SettingsTab.MAIN -> MainSettings(
                        currentQuality = currentQuality,
                        currentSpeed = currentSpeed,
                        currentAudioTrack = currentAudioTrack,
                        subtitlesEnabled = subtitlesEnabled,
                        currentSubtitleTrack = currentSubtitleTrack,
                        musicModeEnabled = musicModeEnabled,
                        onQualityClick = { currentTab = SettingsTab.QUALITY },
                        onSpeedClick = { currentTab = SettingsTab.SPEED },
                        onAudioClick = { currentTab = SettingsTab.AUDIO },
                        onSubtitlesClick = { currentTab = SettingsTab.SUBTITLES },
                        onSleepTimerClick = { currentTab = SettingsTab.SLEEP_TIMER },
                        onMusicModeClick = {
                            onMusicModeToggle()
                            onDismiss()
                        }
                    )
                    
                    SettingsTab.QUALITY -> QualitySettings(
                        videoStreams = videoStreams,
                        currentQuality = currentQuality,
                        onQualitySelected = {
                            onQualitySelected(it)
                            currentTab = SettingsTab.MAIN
                        },
                        onBack = { currentTab = SettingsTab.MAIN }
                    )
                    
                    SettingsTab.SPEED -> SpeedSettings(
                        currentSpeed = currentSpeed,
                        onSpeedSelected = {
                            onSpeedSelected(it)
                            currentTab = SettingsTab.MAIN
                        },
                        onBack = { currentTab = SettingsTab.MAIN }
                    )
                    
                    SettingsTab.AUDIO -> AudioSettings(
                        audioStreams = audioStreams,
                        currentAudioTrack = currentAudioTrack,
                        onAudioSelected = {
                            onAudioSelected(it)
                            currentTab = SettingsTab.MAIN
                        },
                        onBack = { currentTab = SettingsTab.MAIN }
                    )
                    
                    SettingsTab.SUBTITLES -> SubtitlesSettings(
                        subtitleStreams = subtitleStreams,
                        currentSubtitleTrack = currentSubtitleTrack,
                        subtitlesEnabled = subtitlesEnabled,
                        onSubtitleSelected = {
                            onSubtitleSelected(it)
                            currentTab = SettingsTab.MAIN
                        },
                        onToggle = {
                            onSubtitlesToggle()
                            currentTab = SettingsTab.MAIN
                        },
                        onBack = { currentTab = SettingsTab.MAIN }
                    )

                    SettingsTab.SLEEP_TIMER -> SleepTimerSettings(
                        onTimerSelected = {
                            onSleepTimerSelected(it)
                            currentTab = SettingsTab.MAIN
                            onDismiss()
                        },
                        onBack = { currentTab = SettingsTab.MAIN }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSettings(
    currentQuality: VideoStream?,
    currentSpeed: Float,
    currentAudioTrack: AudioStream?,
    subtitlesEnabled: Boolean,
    currentSubtitleTrack: SubtitleStream?,
    musicModeEnabled: Boolean,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onMusicModeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {

        
        item {
            SettingsItem(
                icon = Icons.Default.HighQuality,
                title = "Calidad",
                subtitle = currentQuality?.quality ?: "Auto",
                onClick = onQualityClick
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Speed,
                title = "Velocidad",
                subtitle = "${currentSpeed}x",
                onClick = onSpeedClick
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Timer,
                title = "Temporizador",
                subtitle = "Apagado",
                onClick = onSleepTimerClick
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.GraphicEq,
                title = "Pista de audio",
                subtitle = currentAudioTrack?.let { stream -> 
                    getAudioTrackDisplayName(stream)
                } ?: "Audio principal",
                onClick = onAudioClick
            )
        }
        
        item {
            SettingsItem(
                icon = Icons.Default.Subtitles,
                title = "Subtítulos",
                subtitle = if (subtitlesEnabled) currentSubtitleTrack?.language ?: "Activados" else "Desactivados",
                onClick = onSubtitlesClick
            )
        }
    }
}

@Composable
private fun SleepTimerSettings(
    onTimerSelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                icon = Icons.Default.ArrowBack,
                title = "Volver",
                onClick = onBack
            )
            Divider()
        }
        
        val options = listOf(
            -1L to "Apagado",
            15L to "15 minutos",
            30L to "30 minutos",
            60L to "1 hora",
            -2L to "Fin del video"
        )
        
        items(options) { (value, label) ->
            SelectableSettingsItem(
                title = label,
                isSelected = false, // TODO: Check actual state
                onClick = { onTimerSelected(value) }
            )
        }
    }
}

@Composable
private fun QualitySettings(
    videoStreams: List<VideoStream>,
    currentQuality: VideoStream?,
    onQualitySelected: (VideoStream) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                icon = Icons.Default.ArrowBack,
                title = "Volver",
                onClick = onBack
            )
            Divider()
        }
        
        // Agrupar por calidad - MOSTRAR TODAS las calidades disponibles
        val qualityGroups = videoStreams
            .groupBy { it.quality }
            .toSortedMap(compareByDescending { quality ->
                // Extraer el número de la calidad (ej: "1080p" -> 1080)
                quality.replace("p", "").toIntOrNull() ?: 0
            })
        
        items(qualityGroups.entries.toList()) { (quality, streams) ->
            val stream = streams.first()
            val subtitle = buildString {
                append(stream.format)
                stream.fps?.let { append(" • ${it}fps") }
                if (stream.videoOnly) append(" • Solo video")
            }
            
            SelectableSettingsItem(
                title = quality,
                subtitle = subtitle,
                isSelected = currentQuality?.quality == quality,
                onClick = { onQualitySelected(stream) }
            )
        }
    }
}

@Composable
private fun SpeedSettings(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onBack: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                icon = Icons.Default.ArrowBack,
                title = "Volver",
                onClick = onBack
            )
            Divider()
        }
        
        items(speeds) { speed ->
            SelectableSettingsItem(
                title = if (speed == 1.0f) "Normal" else "${speed}x",
                isSelected = currentSpeed == speed,
                onClick = { onSpeedSelected(speed) }
            )
        }
    }
}

@Composable
private fun AudioSettings(
    audioStreams: List<AudioStream>,
    currentAudioTrack: AudioStream?,
    onAudioSelected: (AudioStream) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                icon = Icons.Default.ArrowBack,
                title = "Volver",
                onClick = onBack
            )
            Divider()
        }
        
        // Group audio streams by language/track to avoid duplicates
        // Piped can return multiple qualities of the SAME language. We should group them!
        val groupedStreams = audioStreams
            .groupBy { stream -> 
                getAudioTrackDisplayName(stream)
            }
            .map { (name, streams) -> 
                // Pick the highest quality stream from each group (Opus preferred if bitrate is same)
                name to streams.sortedWith(
                    compareBy<AudioStream> { it.bitrate ?: 0 }
                        .thenBy { if (it.format.contains("opus", ignoreCase=true) || it.mimeType.contains("opus", ignoreCase=true)) 1 else 0 }
                ).last()
            }
        
        items(groupedStreams) { (displayName, audioStream) ->
            SelectableSettingsItem(
                title = displayName,
                subtitle = getAudioTrackSubtitle(audioStream),
                isSelected = currentAudioTrack?.url == audioStream.url,
                onClick = { onAudioSelected(audioStream) }
            )
        }
    }
}

// Helper function to get display name for audio track
fun getAudioTrackDisplayName(stream: AudioStream): String {
    val trackName = stream.audioTrackName
    
    // Filter out codec raw strings that Piped mistakenly passes as "audioTrackName" occasionally
    val invalidNames = listOf("opus", "webm", "m4a", "mp4a", "aac", "vorbis")
    val isValidName = !trackName.isNullOrEmpty() && invalidNames.none { trackName.contains(it, ignoreCase = true) }
    
    if (isValidName) {
        return trackName!!
    }

    val trackId = stream.audioTrackId
    if (!trackId.isNullOrEmpty()) {
        val languageCode = trackId.split(".").firstOrNull() ?: trackId
        try {
            // Try treating it as a locale code, which covers almost all languages
            val localeName = java.util.Locale(languageCode).displayLanguage
            if (localeName.isNotEmpty() && localeName != languageCode) {
                return localeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            }
        } catch (e: Exception) {
            // Ignore locale error
        }
        
        // Fallback manual mapping
        val languageName = try {
            val localeName = java.util.Locale(languageCode).displayLanguage
            if (localeName.isNotEmpty() && localeName != languageCode) {
                localeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            } else {
                languageCode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            }
        } catch (e: Exception) {
            languageCode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
        
        return languageName
    }
    
    return "Audio original"
}

fun getAudioTrackSubtitle(stream: AudioStream): String {
    val bitrate = stream.bitrate?.let { "${it / 1000} kbps" } ?: ""
    val format = when {
        stream.mimeType.contains("opus", ignoreCase = true) -> "Opus"
        stream.mimeType.contains("aac", ignoreCase = true) -> "AAC"
        stream.mimeType.contains("mp4a", ignoreCase = true) -> "AAC"
        stream.mimeType.contains("vorbis", ignoreCase = true) -> "Vorbis"
        else -> stream.format
    }
    return if (bitrate.isNotEmpty()) "$format • $bitrate" else format
}

@Composable
private fun SubtitlesSettings(
    subtitleStreams: List<SubtitleStream>,
    currentSubtitleTrack: SubtitleStream?,
    subtitlesEnabled: Boolean,
    onSubtitleSelected: (SubtitleStream?) -> Unit,
    onToggle: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            SettingsItem(
                icon = Icons.Default.ArrowBack,
                title = "Volver",
                onClick = onBack
            )
            Divider()
        }
        
        item {
            SelectableSettingsItem(
                title = "Desactivados",
                isSelected = !subtitlesEnabled,
                onClick = { onSubtitleSelected(null) }
            )
        }
        
        items(subtitleStreams) { subtitle ->
            val displayLang = try {
                val localeName = java.util.Locale(subtitle.language).displayLanguage
                if (localeName.isNotEmpty() && localeName != subtitle.language) {
                    localeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                } else subtitle.language
            } catch (e: Exception) {
                subtitle.language
            }
            
            SelectableSettingsItem(
                title = displayLang,
                subtitle = if (subtitle.autoGenerated) "Autogenerado" else null,
                isSelected = subtitlesEnabled && currentSubtitleTrack?.url == subtitle.url,
                onClick = { onSubtitleSelected(subtitle) }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectableSettingsItem(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    Color.Transparent
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
