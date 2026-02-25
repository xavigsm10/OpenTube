package com.opentube.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

// Extension para DataStore
private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_history"
)

data class SearchHistoryItem(
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null
)

@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val HISTORY_KEY = stringPreferencesKey("search_history")
        private const val MAX_HISTORY_ITEMS = 20
    }
    
    // Obtener historial como Flow
    val historyFlow: Flow<List<SearchHistoryItem>> = context.searchHistoryDataStore.data
        .map { preferences ->
            val historyJson = preferences[HISTORY_KEY] ?: "[]"
            try {
                parseHistory(historyJson)
                    .sortedByDescending { it.timestamp }
                    .take(MAX_HISTORY_ITEMS)
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    // Agregar búsqueda al historial
    suspend fun addSearch(query: String, thumbnailUrl: String? = null) {
        if (query.isBlank()) return
        
        context.searchHistoryDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val currentHistory = try {
                parseHistory(currentHistoryJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            // Si ya existe y no pasamos un nuevo thumbnail, conservamos el anterior.
            // Si pasamos un nuevo thumbnail, lo actualizamos.
            val existingItem = currentHistory.find { it.query.equals(query, ignoreCase = true) }
            val finalThumbnail = thumbnailUrl ?: existingItem?.thumbnailUrl
            
            val newItem = SearchHistoryItem(query, System.currentTimeMillis(), finalThumbnail)
            
            // Eliminar duplicados y agregar nuevo al inicio
            val newHistory = (listOf(newItem) + currentHistory.filter { !it.query.equals(query, ignoreCase = true) })
                .take(MAX_HISTORY_ITEMS)
            
            preferences[HISTORY_KEY] = serializeHistory(newHistory)
        }
    }
    
    // Eliminar una búsqueda específica
    suspend fun removeSearch(query: String) {
        context.searchHistoryDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val currentHistory = try {
                parseHistory(currentHistoryJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            val newHistory = currentHistory.filter { it.query != query }
            preferences[HISTORY_KEY] = serializeHistory(newHistory)
        }
    }
    
    // Limpiar todo el historial
    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }
    
    // Parsear JSON a lista de SearchHistoryItem
    private fun parseHistory(json: String): List<SearchHistoryItem> {
        val jsonArray = JSONArray(json)
        val list = mutableListOf<SearchHistoryItem>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val thumb = jsonObject.optString("thumbnailUrl", "")
            val finalThumb = if (thumb.isNullOrEmpty() || thumb == "null") null else thumb
            list.add(
                SearchHistoryItem(
                    query = jsonObject.getString("query"),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis()),
                    thumbnailUrl = finalThumb
                )
            )
        }
        
        return list
    }
    
    // Serializar lista a JSON
    private fun serializeHistory(history: List<SearchHistoryItem>): String {
        val jsonArray = JSONArray()
        
        history.forEach { item ->
            val jsonObject = JSONObject()
            jsonObject.put("query", item.query)
            jsonObject.put("timestamp", item.timestamp)
            item.thumbnailUrl?.let { jsonObject.put("thumbnailUrl", it) }
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
}
