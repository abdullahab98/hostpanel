package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.db.AppDatabase
import com.example.data.model.DeployRequest
import com.example.data.model.EnvVar
import com.example.data.model.Framework
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

data class DeployUiState(
    val currentStep: Int = 0,
    // Step 1 - Source
    val gitUrl: String = "",
    val branch: String = "main",
    // Step 2 - Framework
    val selectedFramework: Framework = Framework.NODEJS,
    val detectedFramework: Framework? = null,
    // Step 3 - Config
    val projectName: String = "",
    val targetPort: String = "",
    val envVars: List<EnvVar> = listOf(EnvVar()),
    // Step 4 - Domain
    val customDomain: String = "",
    val useAutoDomain: Boolean = true,
    // Step 5 - Deploy
    val deployLogs: List<String> = emptyList(),
    val isDeploying: Boolean = false,
    val deploySuccess: Boolean = false,
    val deployUrl: String? = null,
    val error: String? = null
)

class DeployViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)
    private val db = AppDatabase.getInstance(application)
    private val repo = ProjectRepository(settings, db)

    private val _uiState = MutableStateFlow(DeployUiState())
    val uiState: StateFlow<DeployUiState> = _uiState.asStateFlow()

    fun nextStep() {
        if (_uiState.value.currentStep < 4) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
        }
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun setGitUrl(url: String) {
        _uiState.value = _uiState.value.copy(gitUrl = url)
        // Auto-detect project name from URL
        val guessedName = url.trimEnd('/').substringAfterLast('/').removeSuffix(".git")
            .replace(Regex("[^a-zA-Z0-9-]"), "-").lowercase()
        if (_uiState.value.projectName.isEmpty() && guessedName.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(projectName = guessedName)
        }
    }

    fun setBranch(branch: String) { _uiState.value = _uiState.value.copy(branch = branch) }
    fun setFramework(fw: Framework) {
        _uiState.value = _uiState.value.copy(
            selectedFramework = fw,
            targetPort = fw.defaultPort.toString()
        )
    }
    fun setProjectName(name: String) { _uiState.value = _uiState.value.copy(projectName = name) }
    fun setTargetPort(port: String) { _uiState.value = _uiState.value.copy(targetPort = port) }
    fun setCustomDomain(domain: String) { _uiState.value = _uiState.value.copy(customDomain = domain) }
    fun setUseAutoDomain(auto: Boolean) { _uiState.value = _uiState.value.copy(useAutoDomain = auto) }

    fun addEnvVar() {
        val updated = _uiState.value.envVars.toMutableList().apply { add(EnvVar()) }
        _uiState.value = _uiState.value.copy(envVars = updated)
    }

    fun updateEnvVar(index: Int, key: String, value: String) {
        val updated = _uiState.value.envVars.toMutableList()
        if (index < updated.size) updated[index] = EnvVar(key, value)
        _uiState.value = _uiState.value.copy(envVars = updated)
    }

    fun removeEnvVar(index: Int) {
        val updated = _uiState.value.envVars.toMutableList().apply { removeAt(index) }
        _uiState.value = _uiState.value.copy(envVars = updated)
    }

    fun startDeployment() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeploying = true, deployLogs = listOf("> Starting deployment..."))

            val envMap = state.envVars
                .filter { it.key.isNotBlank() }
                .associate { it.key to it.value }

            val request = DeployRequest(
                projectName = state.projectName,
                gitUrl = state.gitUrl,
                branch = state.branch,
                framework = state.selectedFramework.id,
                customDomain = if (state.useAutoDomain) null else state.customDomain.ifBlank { null },
                targetPort = state.targetPort.toIntOrNull() ?: state.selectedFramework.defaultPort,
                envVars = envMap
            )

            addLog("> Connecting to control plane...")

            repo.deploy(request).onSuccess { response ->
                addLog("> ${response.message}")
                if (response.success) {
                    // Start streaming build logs
                    addLog("> Streaming build logs...")
                    try {
                        var deployDone = false
                        repo.streamDeployLogs(state.projectName)
                            .takeWhile { !deployDone }
                            .collect { log ->
                                addLog(log)
                                if (log.contains("[SUCCESS]", ignoreCase = true) ||
                                    log.contains("deployment complete", ignoreCase = true)) {
                                    deployDone = true
                                }
                            }
                    } catch (e: Exception) {
                        // WebSocket may not connect if server doesn't support it yet — that's OK
                    }
                    _uiState.value = _uiState.value.copy(
                        isDeploying = false,
                        deploySuccess = true,
                        deployUrl = response.url
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeploying = false,
                        error = response.message
                    )
                }
            }.onFailure { err ->
                addLog("> Error: ${err.message}")
                _uiState.value = _uiState.value.copy(isDeploying = false, error = err.message)
            }
        }
    }

    fun reset() { _uiState.value = DeployUiState() }

    private fun addLog(msg: String) {
        val updated = _uiState.value.deployLogs.toMutableList().apply { add(msg) }
        _uiState.value = _uiState.value.copy(deployLogs = updated)
    }
}
