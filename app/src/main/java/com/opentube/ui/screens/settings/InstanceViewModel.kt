package com.opentube.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opentube.data.models.PipedInstance
import com.opentube.data.repository.InstanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstanceViewModel @Inject constructor(
    private val repository: InstanceRepository
) : ViewModel() {

    private val _instances = MutableStateFlow<List<PipedInstance>>(emptyList())
    val instances: StateFlow<List<PipedInstance>> = _instances.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    // Status message for pinging
    private val _pingStatus = MutableStateFlow("")
    val pingStatus: StateFlow<String> = _pingStatus.asStateFlow()

    init {
        loadCurrentInstance()
        loadInstances()
    }

    private fun loadCurrentInstance() {
        viewModelScope.launch {
            repository.getSelectedInstance().collectLatest {
                _currentUrl.value = it
            }
        }
    }

    fun loadInstances() {
        viewModelScope.launch {
            _isLoading.value = true
            _pingStatus.value = "Obteniendo lista..."
            
            val fetched = repository.getInstances()
            _instances.value = fetched
            
            _pingStatus.value = "Midiendo latencia..."
            
            // Ping logic: Ping each one and update the list progressively
            val testedInstances = fetched.map { it.copy() } // Deep copy ideally, but data class is immutable mostly
            
            // Parallel or sequential? Parallel is faster.
            val jobs = testedInstances.map { instance ->
                launch {
                   val ping = repository.pingInstance(instance.apiUrl)
                   // Update the specific instance in the list safely
                   updateInstancePing(instance.apiUrl, ping)
                }
            }
            
            // Wait for all (or just let them update flow)
            // We let them update flow dynamically
            _isLoading.value = false
            _pingStatus.value = "Listo"
        }
    }

    private fun updateInstancePing(url: String, ping: Long) {
        val currentList = _instances.value.toMutableList()
        val index = currentList.indexOfFirst { it.apiUrl == url }
        if (index != -1) {
            currentList[index] = currentList[index].copy(ping = ping)
            // Sort by ping (fastest first, but keep selected at top or similar?)
            // For now just update
            _instances.value = currentList.sortedBy { 
                if (it.ping == -1L) Long.MAX_VALUE else it.ping 
            }
        }
    }

    fun selectInstance(instance: PipedInstance) {
        viewModelScope.launch {
            repository.saveInstance(instance.apiUrl)
        }
    }
}
