package com.example.data.api

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ControlPlaneApi {

    @GET("api/health")
    suspend fun health(): Response<Map<String, String>>

    @GET("api/metrics")
    suspend fun getMetrics(): Response<SystemMetrics>

    @GET("api/containers")
    suspend fun getContainers(): Response<List<Project>>

    @GET("api/containers/{name}")
    suspend fun getContainer(@Path("name") name: String): Response<Project>

    @POST("api/deploy")
    suspend fun deploy(@Body request: DeployRequest): Response<DeployResponse>

    @POST("api/containers/{name}/start")
    suspend fun startContainer(@Path("name") name: String): Response<ActionResponse>

    @POST("api/containers/{name}/stop")
    suspend fun stopContainer(@Path("name") name: String): Response<ActionResponse>

    @POST("api/containers/{name}/restart")
    suspend fun restartContainer(@Path("name") name: String): Response<ActionResponse>

    @POST("api/containers/{name}/rebuild")
    suspend fun rebuildContainer(@Path("name") name: String): Response<ActionResponse>

    @DELETE("api/containers/{name}")
    suspend fun deleteContainer(@Path("name") name: String): Response<ActionResponse>

    @GET("api/containers/{name}/env")
    suspend fun getEnvVars(@Path("name") name: String): Response<Map<String, String>>

    @PUT("api/containers/{name}/env")
    suspend fun updateEnvVars(
        @Path("name") name: String,
        @Body envVars: Map<String, String>
    ): Response<ActionResponse>

    @GET("api/domains")
    suspend fun getDomains(): Response<List<Domain>>

    @POST("api/domains")
    suspend fun addDomain(@Body request: AddDomainRequest): Response<Domain>

    @DELETE("api/domains/{domain}")
    suspend fun deleteDomain(@Path("domain") domain: String): Response<ActionResponse>

    @POST("api/domains/{domain}/verify")
    suspend fun verifyDomain(@Path("domain") domain: String): Response<DnsVerifyResponse>

    @POST("api/domains/{domain}/ssl/renew")
    suspend fun renewSsl(@Path("domain") domain: String): Response<ActionResponse>

    @GET("api/audit-logs")
    suspend fun getAuditLogs(): Response<List<AuditLog>>

    @POST("api/tunnel/start")
    suspend fun startTunnel(@Body body: Map<String, String>): Response<ActionResponse>

    @POST("api/tunnel/stop")
    suspend fun stopTunnel(): Response<ActionResponse>

    @GET("api/tunnel")
    suspend fun getTunnelStatus(): Response<Map<String, Any>>

    // Databases
    @GET("api/databases")
    suspend fun getDatabases(): Response<List<DatabaseInstance>>

    @POST("api/databases/{id}/install")
    suspend fun installDatabase(@Path("id") id: String): Response<ActionResponse>

    @POST("api/databases/{id}/start")
    suspend fun startDatabase(@Path("id") id: String): Response<ActionResponse>

    @POST("api/databases/{id}/stop")
    suspend fun stopDatabase(@Path("id") id: String): Response<ActionResponse>
}
