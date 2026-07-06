package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.MetricsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val isRemoteMode: Boolean = false,
    val tunnelToken: String = "",
    val isTunnelEnabled: Boolean = false,
    val serverDomain: String = "",
    val autoRefresh: Boolean = true,
    val themeMode: String = "dark",
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNCHECKED,
    val message: String? = null
)

enum class ConnectionStatus { UNCHECKED, CHECKING, CONNECTED, FAILED }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsDataStore(application)
    private val metricsRepo = MetricsRepository(settings)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.serverUrl.collect { url ->
                _uiState.value = _uiState.value.copy(serverUrl = url)
            }
        }
        viewModelScope.launch {
            settings.apiKey.collect { key ->
                _uiState.value = _uiState.value.copy(apiKey = key)
            }
        }
        viewModelScope.launch {
            settings.isRemoteMode.collect { v ->
                _uiState.value = _uiState.value.copy(isRemoteMode = v)
            }
        }
        viewModelScope.launch {
            settings.tunnelToken.collect { v ->
                _uiState.value = _uiState.value.copy(tunnelToken = v)
            }
        }
        viewModelScope.launch {
            settings.isTunnelEnabled.collect { v ->
                _uiState.value = _uiState.value.copy(isTunnelEnabled = v)
            }
        }
        viewModelScope.launch {
            settings.serverDomain.collect { v ->
                _uiState.value = _uiState.value.copy(serverDomain = v)
            }
        }
        viewModelScope.launch {
            settings.autoRefresh.collect { v ->
                _uiState.value = _uiState.value.copy(autoRefresh = v)
            }
        }
        viewModelScope.launch {
            settings.themeMode.collect { v ->
                _uiState.value = _uiState.value.copy(themeMode = v)
            }
        }
    }

    fun setServerUrl(url: String) { _uiState.value = _uiState.value.copy(serverUrl = url) }
    fun setApiKey(key: String) { _uiState.value = _uiState.value.copy(apiKey = key) }
    fun setRemoteMode(enabled: Boolean) { _uiState.value = _uiState.value.copy(isRemoteMode = enabled) }
    fun setTunnelToken(token: String) { _uiState.value = _uiState.value.copy(tunnelToken = token) }
    fun setTunnelEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(isTunnelEnabled = enabled) }
    fun setServerDomain(domain: String) { _uiState.value = _uiState.value.copy(serverDomain = domain) }
    fun setAutoRefresh(enabled: Boolean) { _uiState.value = _uiState.value.copy(autoRefresh = enabled) }
    fun setThemeMode(mode: String) { _uiState.value = _uiState.value.copy(themeMode = mode) }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            settings.saveServerUrl(state.serverUrl)
            settings.saveApiKey(state.apiKey)
            settings.setRemoteMode(state.isRemoteMode)
            settings.saveTunnelToken(state.tunnelToken)
            settings.setTunnelEnabled(state.isTunnelEnabled)
            settings.saveServerDomain(state.serverDomain)
            settings.setAutoRefresh(state.autoRefresh)
            settings.saveThemeMode(state.themeMode)
            _uiState.value = _uiState.value.copy(message = "✓ Settings saved")
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.CHECKING)
            val ok = metricsRepo.pingServer()
            _uiState.value = _uiState.value.copy(
                connectionStatus = if (ok) ConnectionStatus.CONNECTED else ConnectionStatus.FAILED,
                message = if (ok) "✓ Connected to control plane!" else "✗ Cannot reach server"
            )
        }
    }

    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
