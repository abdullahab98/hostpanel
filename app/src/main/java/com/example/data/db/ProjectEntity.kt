package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val framework: String,
    val customDomain: String?,
    val image: String,
    val ports: String,
    val created: String,
    val cpuPercent: Double,
    val memoryUsageMb: Double,
    val gitUrl: String?,
    val branch: String,
    val sslEnabled: Boolean,
    val uptime: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
