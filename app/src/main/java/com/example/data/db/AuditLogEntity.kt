package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val actor: String,
    val action: String,
    val target: String,
    val status: String,
    val details: String
)
