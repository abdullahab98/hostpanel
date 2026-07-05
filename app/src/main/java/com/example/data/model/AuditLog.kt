package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuditLog(
    @Json(name = "id") val id: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "actor") val actor: String = "admin",
    @Json(name = "action") val action: String,  // DEPLOY, START, STOP, RESTART, DELETE, DOMAIN_ADD, SSL_RENEW
    @Json(name = "target") val target: String,
    @Json(name = "status") val status: String,  // SUCCESS, FAILURE, PENDING
    @Json(name = "details") val details: String = ""
)
