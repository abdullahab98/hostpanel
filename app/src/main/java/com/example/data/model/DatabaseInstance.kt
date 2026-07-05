package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DatabaseInstance(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "port") val port: Int,
    @Json(name = "isInstalled") val isInstalled: Boolean,
    @Json(name = "isRunning") val isRunning: Boolean,
    @Json(name = "connectionString") val connectionString: String? = null
)
