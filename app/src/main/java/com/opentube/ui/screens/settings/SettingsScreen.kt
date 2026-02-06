package com.opentube.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showInstanceDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Configuración",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Piped Settings Section
            item {
                SectionHeader(title = "Ajustes Piped")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.CloudOff,
                    title = "Modo local completo",
                    subtitle = if (settings.fullLocalMode) 
                        "Usando NewPipe Extractor (sin Piped)" 
                    else 
                        "Usando API de Piped",
                    checked = settings.fullLocalMode,
                    onCheckedChange = { viewModel.setFullLocalMode(it) }
                )
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.Download,
                    title = "Extracción local de streams",
                    subtitle = "Extraer URLs de video localmente para mejor rendimiento",
                    checked = settings.localStreamExtraction,
                    onCheckedChange = { viewModel.setLocalStreamExtraction(it) },
                    enabled = !settings.fullLocalMode
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "Instancia Piped",
                    subtitle = if (settings.fullLocalMode) "Deshabilitado (modo local)" else settings.instanceApiUrl,
                    onClick = { if (!settings.fullLocalMode) showInstanceDialog = true },
                    enabled = !settings.fullLocalMode
                )
            }
            
            item { Divider() }

            // Content Section
            item {
                SectionHeader(title = "Contenido")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Idioma del contenido",
                    subtitle = getLanguageName(settings.contentLanguage),
                    onClick = { showLanguageDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Public,
                    title = "País del contenido",
                    subtitle = getCountryName(settings.contentCountry),
                    onClick = { showCountryDialog = true }
                )
            }
            
            item { Divider() }

            // Appearance Section
            item {
                SectionHeader(title = "Apariencia")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Material UI",
                    subtitle = if (settings.materialYouEnabled) "Usando diseño Material You" else "Usando diseño estilo YouTube",
                    checked = settings.materialYouEnabled,
                    onCheckedChange = { viewModel.setMaterialYouEnabled(it) }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Tema",
                    subtitle = when (settings.theme) {
                        ThemeMode.SYSTEM -> "Sistema"
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Oscuro"
                        ThemeMode.YOUTUBE -> "YouTube"
                        ThemeMode.BLUE -> "Azul"
                        ThemeMode.SKY_BLUE -> "Celeste"
                        ThemeMode.RED -> "Rojo"
                        ThemeMode.PINK -> "Rosa"
                        ThemeMode.LIGHT_GREEN -> "Verde Claro"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            item { Divider() }
            
            item { Divider() }
            
            // Playback Section
            item {
                SectionHeader(title = "Reproducción")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Calidad predeterminada",
                    subtitle = settings.defaultQuality,
                    onClick = { showQualityDialog = true }
                )
            }
            
            item { Divider() }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Autoplay",
                    subtitle = "Reproducir automáticamente videos relacionados",
                    checked = settings.autoplayEnabled,
                    onCheckedChange = { viewModel.setAutoplayEnabled(it) }
                )
            }
            
            item { Divider() }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.SignalCellularAlt,
                    title = "Autoplay con datos móviles",
                    subtitle = "Permitir autoplay usando datos móviles",
                    checked = settings.autoplayOnMobileData,
                    onCheckedChange = { viewModel.setAutoplayOnMobileData(it) },
                    enabled = settings.autoplayEnabled
                )
            }
            
            item { Divider() }
            
            // Data & Storage Section
            item {
                SectionHeader(title = "Datos y almacenamiento")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.DataSaverOff,
                    title = "Modo ahorro de datos",
                    subtitle = "Reducir el uso de datos",
                    checked = settings.dataSaverMode,
                    onCheckedChange = { viewModel.setDataSaverMode(it) }
                )
            }
            
            item { Divider() }
            
            // Privacy Section
            item {
                SectionHeader(title = "Privacidad")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.History,
                    title = "Historial de reproducción",
                    subtitle = "Guardar videos vistos",
                    checked = settings.historyEnabled,
                    onCheckedChange = { viewModel.setHistoryEnabled(it) }
                )
            }
            
            item { Divider() }
            
            // Notifications Section
            item {
                SectionHeader(title = "Notificaciones")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = "Recibir notificaciones de nuevos videos",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
            }
            
            item { Divider() }
            
            // About Section
            item {
                SectionHeader(title = "Acerca de")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de OpenTube",
                    subtitle = "Versión 1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }
            
            item { Divider() }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Código fuente",
                    subtitle = "Ver en GitHub",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xavigsm5/OpenTube"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item { Divider() }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Licencias de código abierto",
                    subtitle = "Ver licencias",
                    onClick = { /* Open licenses */ }
                )
            }
            
            // Spacer at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = settings.theme,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Quality Selection Dialog
    if (showQualityDialog) {
        QualitySelectionDialog(
            currentQuality = settings.defaultQuality,
            onQualitySelected = { quality ->
                viewModel.setDefaultQuality(quality)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
    
    // Language Dialog
    if (showLanguageDialog) {
        SelectionDialog(
            title = "Seleccionar idioma",
            options = languageMap,
            currentValue = settings.contentLanguage,
            onSelected = { 
                viewModel.setContentLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Country Dialog
    if (showCountryDialog) {
        SelectionDialog(
            title = "Seleccionar país",
            options = countryMap,
            currentValue = settings.contentCountry,
            onSelected = { 
                viewModel.setContentCountry(it)
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    if (showInstanceDialog) {
        InstanceDialog(
            currentUrl = settings.instanceApiUrl,
            onUrlSelected = { 
                viewModel.setInstanceApiUrl(it)
                showInstanceDialog = false
            },
            onDismiss = { showInstanceDialog = false }
        )
    }
}

@Composable
private fun InstanceDialog(
    currentUrl: String,
    onUrlSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instancia Piped") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("URL de la API") },
                    singleLine = true,
                    // isError = !text.startsWith("http"), supportText = "Debe ser una URL válida"
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Opciones rápidas:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { text = "https://pipedapi.adminforge.de/" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar adminforge.de (Recomendado)")
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { text = "https://pipedapi.kavin.rocks/" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar kavin.rocks")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (text.isNotBlank()) {
                    onUrlSelected(text)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
             TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) 
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar tema") },
        text = {
            Column {
                ThemeMode.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Oscuro"
                                ThemeMode.YOUTUBE -> "YouTube"
                                ThemeMode.BLUE -> "Azul"
                                ThemeMode.SKY_BLUE -> "Celeste"
                                ThemeMode.RED -> "Rojo"
                                ThemeMode.PINK -> "Rosa"
                                ThemeMode.LIGHT_GREEN -> "Verde Claro"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun QualitySelectionDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("2160p", "1440p", "1080p", "720p", "480p", "360p", "Auto")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calidad predeterminada") },
        text = {
            Column {
                qualities.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentQuality == quality,
                            onClick = { onQualitySelected(quality) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = quality)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("OpenTube") },
        text = {
            Column {
                Text(
                    text = "Versión 1.0.0",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Un cliente de YouTube libre y de código abierto para Android.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Desarrollado con ❤️ usando:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Jetpack Compose", style = MaterialTheme.typography.bodySmall)
                Text("• Material Design 3", style = MaterialTheme.typography.bodySmall)
                Text("• Piped API", style = MaterialTheme.typography.bodySmall)
                Text("• ExoPlayer", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private val languageMap: Map<String, String> by lazy {
    java.util.Locale.getISOLanguages()
        .map { code -> code to java.util.Locale(code).getDisplayLanguage(java.util.Locale.getDefault()) }
        .sortedBy { it.second }
        .toMap()
}

private val countryMap: Map<String, String> by lazy {
    java.util.Locale.getISOCountries()
        .map { code -> code to java.util.Locale("", code).getDisplayCountry(java.util.Locale.getDefault()) }
        .sortedBy { it.second }
        .toMap()
}

private fun getLanguageName(code: String): String = languageMap[code] ?: code
private fun getCountryName(code: String): String = countryMap[code] ?: code

@Composable
private fun SelectionDialog(
    title: String,
    options: Map<String, String>,
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(options.toList()) { (key, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == key,
                            onClick = { onSelected(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
