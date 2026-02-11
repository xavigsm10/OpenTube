package com.opentube.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opentube.data.repository.DownloadProgress
import com.opentube.data.repository.UpdateInfo
import com.opentube.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State representing the update check/download process
 */
sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateUiState
    object NoUpdateAvailable : UpdateUiState
    data class Downloading(val progress: Float, val downloadedMB: Float, val totalMB: Float) : UpdateUiState
    object ReadyToInstall : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()
    
    private var pendingDownloadUrl: String? = null
    
    /**
     * Check for available updates
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            
            val result = updateRepository.checkForUpdates()
            
            result.fold(
                onSuccess = { updateInfo ->
                    if (updateInfo.hasUpdate) {
                        pendingDownloadUrl = updateInfo.downloadUrl
                        _uiState.value = UpdateUiState.UpdateAvailable(updateInfo)
                    } else {
                        _uiState.value = UpdateUiState.NoUpdateAvailable
                    }
                },
                onFailure = { exception ->
                    _uiState.value = UpdateUiState.Error(
                        exception.message ?: "Error desconocido al verificar actualizaciones"
                    )
                }
            )
        }
    }
    
    /**
     * Download the update APK
     */
    fun downloadUpdate() {
        val downloadUrl = pendingDownloadUrl
        if (downloadUrl == null) {
            _uiState.value = UpdateUiState.Error("URL de descarga no disponible")
            return
        }
        
        viewModelScope.launch {
            updateRepository.downloadApk(downloadUrl).collect { result ->
                result.fold(
                    onSuccess = { progress ->
                        val downloadedMB = progress.bytesDownloaded / (1024f * 1024f)
                        val totalMB = progress.totalBytes / (1024f * 1024f)
                        
                        _uiState.value = UpdateUiState.Downloading(
                            progress = progress.progress,
                            downloadedMB = downloadedMB,
                            totalMB = totalMB
                        )
                        
                        // Check if download is complete
                        if (progress.progress >= 1.0f) {
                            _uiState.value = UpdateUiState.ReadyToInstall
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = UpdateUiState.Error(
                            exception.message ?: "Error al descargar la actualización"
                        )
                    }
                )
            }
        }
    }
    
    /**
     * Install the downloaded update
     */
    fun installUpdate() {
        val success = updateRepository.installApk()
        if (!success) {
            _uiState.value = UpdateUiState.Error("Error al instalar la actualización")
        }
    }
    
    /**
     * Reset the UI state to idle
     */
    fun resetState() {
        updateRepository.cleanupDownloadedApk()
        _uiState.value = UpdateUiState.Idle
    }
    
    /**
     * Dismiss the current state (go back to idle)
     */
    fun dismiss() {
        if (_uiState.value is UpdateUiState.Downloading) {
            // Don't dismiss while downloading
            return
        }
        _uiState.value = UpdateUiState.Idle
    }
}
