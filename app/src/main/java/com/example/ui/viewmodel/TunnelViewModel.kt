package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.TunnelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TunnelUiState(
    val isRunning: Boolean = false,
    val activeUrl: String = "",
    val provider: String = "cloudflare",
    val isLoading: Boolean = false,
    val message: String? = null
)

class TunnelViewModel(
    private val repository: TunnelRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunnelUiState())
    val uiState: StateFlow<TunnelUiState> = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getTunnelStatus().collect { result ->
                result.onSuccess { status ->
                    val running = status["running"] as? Boolean ?: false
                    val url = status["url"] as? String ?: ""
                    val prov = status["provider"] as? String ?: "cloudflare"

                    if (running) {
                        settingsDataStore.saveTunnelUrl(url)
                    } else {
                        settingsDataStore.saveTunnelUrl("")
                    }

                    _uiState.value = _uiState.value.copy(
                        isRunning = running,
                        activeUrl = url,
                        provider = prov,
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Error checking status: ${e.message}"
                    )
                }
            }
        }
    }

    fun startTunnel(provider: String = "cloudflare") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Starting tunnel, please wait...")
            val result = repository.startTunnel(port = 3001, provider = provider)
            result.onSuccess { data ->
                val url = data["url"] ?: ""
                settingsDataStore.saveTunnelUrl(url)
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    activeUrl = url,
                    provider = provider,
                    isLoading = false,
                    message = "✓ Tunnel started successfully!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to start tunnel: ${e.message}"
                )
            }
        }
    }

    fun stopTunnel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.stopTunnel()
            result.onSuccess {
                settingsDataStore.saveTunnelUrl("")
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    activeUrl = "",
                    isLoading = false,
                    message = "Tunnel stopped"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Failed to stop tunnel: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    // Factory for creating TunnelViewModel without Hilt.
    // baseUrl and apiKey are collected from DataStore as state in the composable.
    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val baseUrl: String,
        private val apiKey: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.getApi(baseUrl, apiKey)
            val repository = TunnelRepository(api)
            return TunnelViewModel(repository, settingsDataStore) as T
        }
    }
}
