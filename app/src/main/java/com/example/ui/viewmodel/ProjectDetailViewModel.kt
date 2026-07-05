package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.db.AppDatabase
import com.example.data.model.EnvVar
import com.example.data.model.Project
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val project: Project? = null,
    val logs: List<String> = emptyList(),
    val envVars: List<EnvVar> = emptyList(),
    val isLoading: Boolean = true,
    val isStreamingLogs: Boolean = false,
    val actionMessage: String? = null,
    val error: String? = null,
    val selectedTab: Int = 0
)

class ProjectDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsDataStore(application)
    private val db = AppDatabase.getInstance(application)
    private val repo = ProjectRepository(settings, db)

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    fun loadProject(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getProject(name).onSuccess { project ->
                _uiState.value = _uiState.value.copy(project = project, isLoading = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
            }
            repo.getEnvVars(name).onSuccess { map ->
                _uiState.value = _uiState.value.copy(envVars = map.map { EnvVar(it.key, it.value) })
            }
        }
    }

    fun startLogStream(projectName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStreamingLogs = true, logs = listOf("[INFO] Connecting..."))
            try {
                repo.streamLogs(projectName).collect { line ->
                    val updated = _uiState.value.logs.toMutableList().apply {
                        add(line); if (size > 500) removeAt(0)
                    }
                    _uiState.value = _uiState.value.copy(logs = updated)
                }
            } catch (e: Exception) {
                val updated = _uiState.value.logs.toMutableList().apply { add("[ERROR] ${e.message}") }
                _uiState.value = _uiState.value.copy(logs = updated, isStreamingLogs = false)
            }
        }
    }

    fun setTab(tab: Int) { _uiState.value = _uiState.value.copy(selectedTab = tab) }
    fun startContainer(name: String) = action(name, "Starting") { repo.startContainer(name) }
    fun stopContainer(name: String) = action(name, "Stopping") { repo.stopContainer(name) }
    fun restartContainer(name: String) = action(name, "Restarting") { repo.restartContainer(name) }
    fun rebuildContainer(name: String) = action(name, "Rebuilding") { repo.rebuildContainer(name) }

    fun saveEnvVars(name: String) {
        viewModelScope.launch {
            val map = _uiState.value.envVars.filter { it.key.isNotBlank() }.associate { it.key to it.value }
            repo.updateEnvVars(name, map).onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "✓ Environment variables saved")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(actionMessage = "✗ ${err.message}")
            }
        }
    }

    fun addEnvVar() {
        _uiState.value = _uiState.value.copy(envVars = _uiState.value.envVars + EnvVar())
    }
    fun updateEnvVar(i: Int, k: String, v: String) {
        val updated = _uiState.value.envVars.toMutableList().also { if (i < it.size) it[i] = EnvVar(k, v) }
        _uiState.value = _uiState.value.copy(envVars = updated)
    }
    fun removeEnvVar(i: Int) {
        _uiState.value = _uiState.value.copy(envVars = _uiState.value.envVars.toMutableList().also { it.removeAt(i) })
    }
    fun clearMessage() { _uiState.value = _uiState.value.copy(actionMessage = null) }

    private fun action(name: String, verb: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionMessage = "$verb $name...")
            block().onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "✓ $verb complete")
                loadProject(name)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(actionMessage = "✗ ${err.message}")
            }
        }
    }
}
