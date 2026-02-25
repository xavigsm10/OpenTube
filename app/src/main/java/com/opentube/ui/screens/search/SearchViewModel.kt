package com.opentube.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.opentube.data.api.PipedApiService
import com.opentube.data.models.Video
import com.opentube.data.repository.SearchPagingSource
import com.opentube.util.SearchHistoryManager
import com.opentube.util.SearchHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val newPipeHelper: com.opentube.data.extractor.NewPipeHelper,
    private val pipedApiService: PipedApiService, // Need to remove this or keep it if used for suggestions, but suggestions exist in NewPipeHelper too.
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<Flow<PagingData<Video>>>(emptyFlow())
    val searchResults: StateFlow<Flow<PagingData<Video>>> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            searchHistoryManager.historyFlow.collectLatest {
                _searchHistory.value = it
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            fetchSuggestions(query)
        } else {
            _suggestions.value = emptyList()
        }
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            try {
                // Use NewPipe for suggestions too if possible
                val results = newPipeHelper.getSearchSuggestions(query)
                results.onSuccess { 
                    _suggestions.value = it
                }
            } catch (e: Exception) {
                // Ignore errors for suggestions
            }
        }
    }

    fun search(query: String = _searchQuery.value, thumbnailUrl: String? = null) {
        if (query.isBlank()) return

        viewModelScope.launch {
            searchHistoryManager.addSearch(query, thumbnailUrl)
        }

        val pagingSource = SearchPagingSource(newPipeHelper, query)
        
        _searchResults.value = Pager(
            config = PagingConfig(
                pageSize = 20,              // Cada página carga 20 videos
                enablePlaceholders = false,
                initialLoadSize = 20,        // Carga inicial: 20 videos
                prefetchDistance = 5         // Empieza a cargar la siguiente página cuando quedan 5 items
            ),
            pagingSourceFactory = { pagingSource }
        ).flow.cachedIn(viewModelScope)
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryManager.clearHistory()
        }
    }

    fun removeFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryManager.removeSearch(query)
        }
    }
}