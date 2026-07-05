package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Domain(
    @Json(name = "id") val id: String,
    @Json(name = "domain") val domain: String,
    @Json(name = "projectName") val projectName: String,
    @Json(name = "sslStatus") val sslStatus: String = "pending", // valid, pending, error, expired
    @Json(name = "sslExpiry") val sslExpiry: String? = null,
    @Json(name = "autoRenew") val autoRenew: Boolean = true,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "dnsVerified") val dnsVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AddDomainRequest(
    @Json(name = "domain") val domain: String,
    @Json(name = "projectName") val projectName: String
)

@JsonClass(generateAdapter = true)
data class DnsVerifyResponse(
    @Json(name = "verified") val verified: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "resolvedIp") val resolvedIp: String? = null
)
