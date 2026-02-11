package com.opentube.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.opentube.data.repository.UpdateInfo
import com.opentube.ui.screens.settings.UpdateUiState

/**
 * Dialog for displaying update information and progress
 */
@Composable
fun UpdateDialog(
    uiState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    
    when (uiState) {
        is UpdateUiState.Idle -> {
            // Initial state - show check for updates option
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Buscar actualizaciones") },
                text = {
                    Text("¿Deseas verificar si hay una nueva versión de OpenTube disponible?")
                },
                confirmButton = {
                    Button(onClick = onCheckForUpdates) {
                        Text("Verificar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        is UpdateUiState.Checking -> {
            AlertDialog(
                onDismissRequest = { /* Can't dismiss while checking */ },
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("Verificando...") },
                text = {
                    Text("Buscando actualizaciones disponibles...")
                },
                confirmButton = { }
            )
        }
        
        is UpdateUiState.NoUpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("¡Estás al día!") },
                text = {
                    Text("Ya tienes la última versión de OpenTube instalada.")
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("Aceptar")
                    }
                }
            )
        }
        
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                updateInfo = uiState.updateInfo,
                onDownload = onDownloadUpdate,
                onDismiss = onDismiss,
                onOpenInBrowser = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.updateInfo.htmlUrl))
                    context.startActivity(intent)
                }
            )
        }
        
        is UpdateUiState.Downloading -> {
            DownloadingDialog(
                progress = uiState.progress,
                downloadedMB = uiState.downloadedMB,
                totalMB = uiState.totalMB
            )
        }
        
        is UpdateUiState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { /* Can't dismiss when ready to install */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Descarga completada") },
                text = {
                    Column {
                        Text("La actualización se ha descargado correctamente.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pulsa 'Instalar' para instalar la nueva versión.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onInstallUpdate) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instalar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Más tarde")
                    }
                }
            )
        }
        
        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Error") },
                text = {
                    Column {
                        Text(uiState.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Si el problema persiste, intenta descargar la actualización manualmente desde GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onCheckForUpdates) {
                        Text("Reintentar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "¡Nueva versión disponible!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Version info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Actual",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v${updateInfo.currentVersion}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nueva",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v${updateInfo.latestVersion}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Release notes
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "Novedades:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Size info
                updateInfo.apkSizeMB?.let { size ->
                    Text(
                        text = "Tamaño: ${String.format("%.1f", size)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                if (updateInfo.downloadUrl != null) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargar e instalar")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onOpenInBrowser,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver en GitHub")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Más tarde")
                }
            }
        }
    }
}

@Composable
private fun DownloadingDialog(
    progress: Float,
    downloadedMB: Float,
    totalMB: Float
) {
    AlertDialog(
        onDismissRequest = { /* Can't dismiss while downloading */ },
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = { 
            Text(
                text = "Descargando actualización...",
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", downloadedMB)} / ${String.format("%.1f", totalMB)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Por favor, no cierres la aplicación",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { }
    )
}
