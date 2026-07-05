package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Project(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: String = "",
    @Json(name = "status") val status: String, // running, stopped, building, error, restarting
    @Json(name = "created") val created: String = "",
    @Json(name = "ports") val ports: String = "",
    @Json(name = "customDomain") val customDomain: String? = null,
    @Json(name = "framework") val framework: String = "static", // nodejs, nextjs, react, django, springboot, static
    @Json(name = "cpuPercent") val cpuPercent: Double = 0.0,
    @Json(name = "memoryUsageMb") val memoryUsageMb: Double = 0.0,
    @Json(name = "gitUrl") val gitUrl: String? = null,
    @Json(name = "branch") val branch: String = "main",
    @Json(name = "sslEnabled") val sslEnabled: Boolean = false,
    @Json(name = "uptime") val uptime: String = ""
)

@JsonClass(generateAdapter = true)
data class ContainerStats(
    @Json(name = "id") val id: String,
    @Json(name = "cpuPercent") val cpuPercent: Double,
    @Json(name = "memoryUsageMb") val memoryUsageMb: Double,
    @Json(name = "networkRxMb") val networkRxMb: Double,
    @Json(name = "networkTxMb") val networkTxMb: Double
)
