package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.db.AppDatabase
import com.example.data.model.Project
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)
    private val db = AppDatabase.getInstance(application)
    private val repo = ProjectRepository(settings, db)

    private val _uiState = MutableStateFlow(ProjectsUiState(isLoading = true))
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        // Observe cached data immediately
        viewModelScope.launch {
            repo.getCachedProjects().collect { cached ->
                _uiState.value = _uiState.value.copy(projects = cached)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.fetchAndCacheProjects().onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
            }
        }
    }

    fun startContainer(name: String) = containerAction(name, "Starting") { repo.startContainer(name) }
    fun stopContainer(name: String) = containerAction(name, "Stopping") { repo.stopContainer(name) }
    fun restartContainer(name: String) = containerAction(name, "Restarting") { repo.restartContainer(name) }

    fun deleteContainer(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionMessage = "Deleting $name...")
            repo.deleteContainer(name).onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "✓ $name deleted")
                refresh()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(actionMessage = "✗ ${err.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null, error = null)
    }

    private fun containerAction(name: String, verb: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionMessage = "$verb $name...")
            block().onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "✓ $name $verb done")
                refresh()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(actionMessage = "✗ ${err.message}")
            }
        }
    }
}
