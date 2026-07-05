package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemMetrics(
    @Json(name = "cpuUsagePercent") val cpuUsagePercent: Double = 0.0,
    @Json(name = "ramTotalGb") val ramTotalGb: Double = 0.0,
    @Json(name = "ramUsedGb") val ramUsedGb: Double = 0.0,
    @Json(name = "diskTotalGb") val diskTotalGb: Double = 0.0,
    @Json(name = "diskUsedGb") val diskUsedGb: Double = 0.0,
    @Json(name = "networkRxMb") val networkRxMb: Double = 0.0,
    @Json(name = "networkTxMb") val networkTxMb: Double = 0.0,
    @Json(name = "osInfo") val osInfo: String = "",
    @Json(name = "dockerVersion") val dockerVersion: String = "",
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long = 0L,
    @Json(name = "activeContainers") val activeContainers: Int = 0
)
