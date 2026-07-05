package com.example.data.repository

import com.example.data.api.ControlPlaneApi
import com.example.data.model.DatabaseInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * DatabaseRepository — no Hilt DI.
 * Instantiated directly in DatabaseViewModel using ApiClient.
 */
class DatabaseRepository(
    private val api: ControlPlaneApi
) {
    fun getDatabases(): Flow<Result<List<DatabaseInstance>>> = flow {
        try {
            val response = api.getDatabases()
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyList()))
            } else {
                emit(Result.failure(Exception("Failed to fetch databases: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun installDatabase(id: String): Result<String> {
        return try {
            val response = api.installDatabase(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Installation started")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Installation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startDatabase(id: String): Result<String> {
        return try {
            val response = api.startDatabase(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Database started")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to start database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopDatabase(id: String): Result<String> {
        return try {
            val response = api.stopDatabase(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Database stopped")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to stop database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
