package com.opentube.ui.screens.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.opentube.data.local.dataStore


data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val defaultQuality: String = "720p",
    val autoplayEnabled: Boolean = true,
    val historyEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dataSaverMode: Boolean = false,
    val autoplayOnMobileData: Boolean = false,
    val musicModeEnabled: Boolean = false,
    val contentLanguage: String = "es",
    val contentCountry: String = "ES",
    val materialYouEnabled: Boolean = true,
    val instanceApiUrl: String = "https://pipedapi.adminforge.de/",
    val fullLocalMode: Boolean = false,
    val localStreamExtraction: Boolean = true
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    YOUTUBE,
    BLUE,
    SKY_BLUE,
    RED,
    PINK,
    LIGHT_GREEN
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicModeManager: com.opentube.ui.theme.MusicModeManager
) : ViewModel() {
    
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")
        val AUTOPLAY_ON_MOBILE_DATA = booleanPreferencesKey("autoplay_on_mobile_data")
        val MUSIC_MODE_ENABLED = booleanPreferencesKey("music_mode_enabled")
        val CONTENT_LANGUAGE = stringPreferencesKey("content_language")
        val CONTENT_COUNTRY = stringPreferencesKey("content_country")
        val MATERIAL_YOU_ENABLED = booleanPreferencesKey("material_you_enabled")
        val INSTANCE_API_URL = stringPreferencesKey("instance_api_url")
        val FULL_LOCAL_MODE = booleanPreferencesKey("full_local_mode")
        val LOCAL_STREAM_EXTRACTION = booleanPreferencesKey("local_stream_extraction")
    }
    
    val settings: StateFlow<AppSettings> = context.dataStore.data
        .map { preferences ->
            AppSettings(
                theme = preferences[PreferencesKeys.THEME]?.let { 
                    ThemeMode.valueOf(it) 
                } ?: ThemeMode.DARK,
                defaultQuality = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "720p",
                autoplayEnabled = preferences[PreferencesKeys.AUTOPLAY_ENABLED] ?: true,
                historyEnabled = preferences[PreferencesKeys.HISTORY_ENABLED] ?: true,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                dataSaverMode = preferences[PreferencesKeys.DATA_SAVER_MODE] ?: false,
                autoplayOnMobileData = preferences[PreferencesKeys.AUTOPLAY_ON_MOBILE_DATA] ?: false,
                musicModeEnabled = preferences[PreferencesKeys.MUSIC_MODE_ENABLED] ?: false,
                contentLanguage = preferences[PreferencesKeys.CONTENT_LANGUAGE] ?: "es",
                contentCountry = preferences[PreferencesKeys.CONTENT_COUNTRY] ?: "ES",
                materialYouEnabled = preferences[PreferencesKeys.MATERIAL_YOU_ENABLED] ?: true,
                instanceApiUrl = preferences[PreferencesKeys.INSTANCE_API_URL] ?: "https://pipedapi.adminforge.de/",
                fullLocalMode = preferences[PreferencesKeys.FULL_LOCAL_MODE] ?: false,
                localStreamExtraction = preferences[PreferencesKeys.LOCAL_STREAM_EXTRACTION] ?: true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    
    init {
        // Sync MusicModeManager with saved preference
        viewModelScope.launch {
            settings.collect { appSettings ->
                musicModeManager.setMusicMode(appSettings.musicModeEnabled)
            }
        }
    }
    
    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME] = theme.name
            }
        }
    }
    
    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_QUALITY] = quality
            }
        }
    }
    
    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTOPLAY_ENABLED] = enabled
            }
        }
    }
    
    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.HISTORY_ENABLED] = enabled
            }
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            }
        }
    }
    
    fun setDataSaverMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DATA_SAVER_MODE] = enabled
            }
        }
    }
    
    fun setAutoplayOnMobileData(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTOPLAY_ON_MOBILE_DATA] = enabled
            }
        }
    }
    
    fun setMusicModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.MUSIC_MODE_ENABLED] = enabled
            }
            // Also update MusicModeManager immediately
            musicModeManager.setMusicMode(enabled)
        }
    }

    fun setContentLanguage(language: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.CONTENT_LANGUAGE] = language
            }
        }
    }

    fun setContentCountry(country: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.CONTENT_COUNTRY] = country
            }
        }
    }
    
    fun setInstanceApiUrl(url: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.INSTANCE_API_URL] = url
            }
        }
        // Force app restart or re-init might be stored in a separate mechanism, 
        // but updating DataStore is enough for now as InstancePreferences reads from it.
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.MATERIAL_YOU_ENABLED] = enabled
                if (!enabled) {
                    preferences[PreferencesKeys.THEME] = ThemeMode.YOUTUBE.name
                }
            }
        }
    }

    fun setFullLocalMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FULL_LOCAL_MODE] = enabled
            }
        }
    }

    fun setLocalStreamExtraction(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LOCAL_STREAM_EXTRACTION] = enabled
            }
        }
    }
}
