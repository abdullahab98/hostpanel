package com.example.data.repository

import com.example.data.api.ControlPlaneApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * TunnelRepository — no Hilt DI.
 * Instantiated directly in TunnelViewModel.Factory.
 */
class TunnelRepository(
    private val api: ControlPlaneApi
) {
    fun getTunnelStatus(): Flow<Result<Map<String, Any>>> = flow {
        try {
            val response = api.getTunnelStatus()
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyMap()))
            } else {
                emit(Result.failure(Exception("Failed to fetch tunnel status: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun startTunnel(port: Int = 3001, provider: String = "cloudflare"): Result<Map<String, String>> {
        return try {
            val requestBody = mapOf(
                "port" to port.toString(),
                "provider" to provider
            )
            val response = api.startTunnel(requestBody)
            if (response.isSuccessful && response.body()?.success == true) {
                // Extract url from the raw body because the response model doesn't have a url field
                val url = response.body()?.message
                    ?.substringAfter("url: ")
                    ?.trim()
                    ?: ""
                Result.success(mapOf("url" to url, "provider" to provider))
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to start tunnel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopTunnel(): Result<String> {
        return try {
            val response = api.stopTunnel()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Tunnel stopped")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to stop tunnel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
