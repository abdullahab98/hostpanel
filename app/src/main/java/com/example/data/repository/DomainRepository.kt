package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.AuditLog
import com.example.data.model.Domain
import kotlinx.coroutines.flow.first

/**
 * DomainRepository — handles domain management and audit log retrieval.
 * Used by DashboardViewModel to show recent activity.
 */
class DomainRepository(private val settings: SettingsDataStore) {

    private suspend fun api() = ApiClient.getApi(
        settings.serverUrl.first(),
        settings.apiKey.first()
    )

    suspend fun getAuditLogs(limit: Int = 20): Result<List<AuditLog>> {
        return try {
            val response = api().getAuditLogs()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch audit logs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDomains(): Result<List<Domain>> {
        return try {
            val response = api().getDomains()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch domains: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
