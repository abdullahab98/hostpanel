package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeployRequest(
    @Json(name = "projectName") val projectName: String,
    @Json(name = "gitUrl") val gitUrl: String,
    @Json(name = "branch") val branch: String = "main",
    @Json(name = "framework") val framework: String,
    @Json(name = "customDomain") val customDomain: String? = null,
    @Json(name = "targetPort") val targetPort: Int,
    @Json(name = "envVars") val envVars: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class DeployResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class ActionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class EnvVar(
    val key: String = "",
    val value: String = ""
)

enum class Framework(
    val id: String,
    val displayName: String,
    val defaultPort: Int,
    val emoji: String
) {
    NODEJS("nodejs", "Node.js / Express", 3000, "🟢"),
    NEXTJS("nextjs", "Next.js", 3000, "⬛"),
    REACT("react", "React (Static)", 80, "⚛️"),
    DJANGO("django", "Django / Python", 8000, "🐍"),
    SPRINGBOOT("springboot", "Spring Boot", 8080, "☕"),
    STATIC("static", "Static HTML", 80, "📄");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: STATIC
    }
}
