package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.SystemMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class MetricsRepository(private val settings: SettingsDataStore) {

    suspend fun getMetrics(): Result<SystemMetrics> {
        return try {
            val url = settings.serverUrl.first()
            val key = settings.apiKey.first()
            if (url.isBlank() || key.isBlank()) {
                return Result.failure(Exception("Server not configured. Go to Settings."))
            }
            val api = ApiClient.getApi(url, key)
            val response = api.getMetrics()
            if (response.isSuccessful) {
                Result.success(response.body() ?: SystemMetrics())
            } else {
                Result.failure(Exception("Server error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot reach server: ${e.message}"))
        }
    }

    suspend fun pingServer(): Boolean {
        return try {
            val url = settings.serverUrl.first()
            val key = settings.apiKey.first()
            if (url.isBlank() || key.isBlank()) return false
            val api = ApiClient.getApi(url, key)
            val response = api.health()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
