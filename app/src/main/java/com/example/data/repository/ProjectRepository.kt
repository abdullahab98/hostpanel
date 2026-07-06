package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.ControlPlaneApi
import com.example.data.api.WebSocketManager
import com.example.data.datastore.SettingsDataStore
import com.example.data.db.AppDatabase
import com.example.data.db.ProjectEntity
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProjectRepository(
    private val settings: SettingsDataStore,
    private val db: AppDatabase
) {
    private val wsManager = WebSocketManager()

    fun getCachedProjects(): Flow<List<Project>> {
        return db.projectDao().getAllProjects().map { entities ->
            entities.map { it.toProject() }
        }
    }

    suspend fun fetchAndCacheProjects(): Result<List<Project>> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.getContainers()
            if (response.isSuccessful) {
                val projects = response.body() ?: emptyList()
                db.projectDao().insertAll(projects.map { it.toEntity() })
                Result.success(projects)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getProject(name: String): Result<Project> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.getContainer(name)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deploy(request: DeployRequest): Result<DeployResponse> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.deploy(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Deploy failed: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startContainer(name: String): Result<ActionResponse> = containerAction { it.startContainer(name) }
    suspend fun stopContainer(name: String): Result<ActionResponse> = containerAction { it.stopContainer(name) }
    suspend fun restartContainer(name: String): Result<ActionResponse> = containerAction { it.restartContainer(name) }
    suspend fun rebuildContainer(name: String): Result<ActionResponse> = containerAction { it.rebuildContainer(name) }

    suspend fun deleteContainer(name: String): Result<ActionResponse> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.deleteContainer(name)
            if (response.isSuccessful) {
                db.projectDao().deleteById(name)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnvVars(name: String): Result<Map<String, String>> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.getEnvVars(name)
            if (response.isSuccessful) Result.success(response.body() ?: emptyMap())
            else Result.failure(Exception("Error ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEnvVars(name: String, envVars: Map<String, String>): Result<ActionResponse> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = api.updateEnvVars(name, envVars)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Error ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamLogs(projectName: String): Flow<String> = kotlinx.coroutines.flow.flow {
        val baseUrl = settings.serverUrl.first()
        val apiKey = settings.apiKey.first()
        wsManager.streamLogs(baseUrl, apiKey, projectName).collect { emit(it) }
    }

    fun streamDeployLogs(projectName: String): Flow<String> = kotlinx.coroutines.flow.flow {
        val baseUrl = settings.serverUrl.first()
        val apiKey = settings.apiKey.first()
        wsManager.streamDeployLogs(baseUrl, apiKey, projectName).collect { emit(it) }
    }

    private suspend fun containerAction(block: suspend (ControlPlaneApi) -> retrofit2.Response<ActionResponse>): Result<ActionResponse> {
        return try {
            val api = getApi() ?: return Result.failure(Exception("Server not configured"))
            val response = block(api)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Error ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getApi(): ControlPlaneApi? {
        val url = settings.serverUrl.first()
        val key = settings.apiKey.first()
        if (url.isBlank() || key.isBlank()) return null
        return ApiClient.getApi(url, key)
    }
}

// Extension mappers
fun Project.toEntity() = ProjectEntity(
    id = id, name = name, status = status, framework = framework,
    customDomain = customDomain, image = image, ports = ports,
    created = created, cpuPercent = cpuPercent, memoryUsageMb = memoryUsageMb,
    gitUrl = gitUrl, branch = branch, sslEnabled = sslEnabled, uptime = uptime
)

fun ProjectEntity.toProject() = Project(
    id = id, name = name, status = status, framework = framework,
    customDomain = customDomain, image = image, ports = ports,
    created = created, cpuPercent = cpuPercent, memoryUsageMb = memoryUsageMb,
    gitUrl = gitUrl, branch = branch, sslEnabled = sslEnabled, uptime = uptime
)
