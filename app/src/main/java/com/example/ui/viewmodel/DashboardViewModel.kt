package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.AuditLog
import com.example.data.model.SystemMetrics
import com.example.data.repository.DomainRepository
import com.example.data.repository.MetricsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val metrics: SystemMetrics? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val recentLogs: List<AuditLog> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)
    private val metricsRepo = MetricsRepository(settings)
    private val domainRepo = DomainRepository(settings)
    private val notifier = com.example.utils.NotificationHelper(application)
    private var lastNotifiedLogId: String? = null

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
        startAutoRefresh()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = metricsRepo.getMetrics()
            result.onSuccess { metrics ->
                _uiState.value = _uiState.value.copy(
                    metrics = metrics, isLoading = false, isConnected = true
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = err.message,
                    isConnected = false
                )
            }

            // Also load recent audit logs
            domainRepo.getAuditLogs().onSuccess { logs ->
                _uiState.value = _uiState.value.copy(recentLogs = logs.take(5))
                logs.firstOrNull()?.let { latest ->
                    if (latest.id != lastNotifiedLogId) {
                        if (lastNotifiedLogId != null && (latest.status == "FAILURE" || latest.action.contains("RESTART", true) || latest.action.contains("CRASH", true) || latest.details.contains("crash", true))) {
                            notifier.showServerAlert("⚠️ Server Alert: ${latest.target}", "${latest.action}: ${latest.details.ifEmpty { "Process status changed" }}")
                        }
                        lastNotifiedLogId = latest.id
                    }
                }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000) // Refresh every 15 seconds
                if (_uiState.value.isConnected) loadMetrics()
            }
        }
    }
}
