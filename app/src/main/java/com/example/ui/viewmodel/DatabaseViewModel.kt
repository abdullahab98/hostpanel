package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.DatabaseInstance
import com.example.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class DatabaseUiState {
    object Loading : DatabaseUiState()
    data class Success(val databases: List<DatabaseInstance>) : DatabaseUiState()
    data class Error(val message: String) : DatabaseUiState()
}

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)
    private lateinit var repository: DatabaseRepository

    private val _uiState = MutableStateFlow<DatabaseUiState>(DatabaseUiState.Loading)
    val uiState: StateFlow<DatabaseUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settings.serverUrl.first()
            val key = settings.apiKey.first()
            val api = ApiClient.getApi(url, key)
            repository = DatabaseRepository(api)
            loadDatabases()
        }
    }

    fun loadDatabases() {
        viewModelScope.launch {
            _uiState.value = DatabaseUiState.Loading
            repository.getDatabases().collect { result ->
                result.onSuccess { dbs ->
                    _uiState.value = DatabaseUiState.Success(dbs)
                }.onFailure { e ->
                    _uiState.value = DatabaseUiState.Error(e.message ?: "Failed to load databases")
                }
            }
        }
    }

    fun installDatabase(id: String) {
        viewModelScope.launch {
            val result = repository.installDatabase(id)
            result.onSuccess { msg ->
                _actionMessage.value = msg
                loadDatabases()
            }.onFailure { e ->
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun startDatabase(id: String) {
        viewModelScope.launch {
            val result = repository.startDatabase(id)
            result.onSuccess { msg ->
                _actionMessage.value = msg
                loadDatabases()
            }.onFailure { e ->
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun stopDatabase(id: String) {
        viewModelScope.launch {
            val result = repository.stopDatabase(id)
            result.onSuccess { msg ->
                _actionMessage.value = msg
                loadDatabases()
            }.onFailure { e ->
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
