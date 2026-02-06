package com.opentube.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class InstancePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val INSTANCE_API_URL = stringPreferencesKey("instance_api_url")
    
    // Default fallback - Using a more stable instance as default
    private val DEFAULT_API_URL = "https://pipedapi.moomoo.me/"

    val apiUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            val stored = preferences[INSTANCE_API_URL]
            // Auto-switch away from kavin.rocks if it's the stored one, as it's causing 403s
            if (stored == "https://pipedapi.kavin.rocks/" || stored == "https://pipedapi.kavin.rocks") {
                DEFAULT_API_URL 
            } else {
                stored ?: DEFAULT_API_URL
            }
        }

    suspend fun setApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[INSTANCE_API_URL] = url
        }
    }

    private val CONTENT_COUNTRY = stringPreferencesKey("content_country")
    private val CONTENT_LANGUAGE = stringPreferencesKey("content_language")

    val regionFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CONTENT_COUNTRY] ?: "ES" // Default to Spain or US
        }

    suspend fun setRegion(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[CONTENT_COUNTRY] = countryCode
        }
    }

    // Full Local Mode - Use NewPipe Extractor exclusively instead of Piped API
    private val FULL_LOCAL_MODE = booleanPreferencesKey("full_local_mode")
    
    val fullLocalModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FULL_LOCAL_MODE] ?: false
        }

    suspend fun setFullLocalMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FULL_LOCAL_MODE] = enabled
        }
    }

    // Local Stream Extraction - Extract streams locally even when using Piped
    private val LOCAL_STREAM_EXTRACTION = booleanPreferencesKey("local_stream_extraction")
    
    val localStreamExtractionFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[LOCAL_STREAM_EXTRACTION] ?: true
        }

    suspend fun setLocalStreamExtraction(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_STREAM_EXTRACTION] = enabled
        }
    }
}
