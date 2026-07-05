package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (color, label) = when (status.lowercase()) {
        "running" -> Pair(Color(0xFF10B981), "RUNNING")
        "stopped" -> Pair(Color(0xFF6B7280), "STOPPED")
        "building" -> Pair(Color(0xFFF59E0B), "BUILDING")
        "restarting" -> Pair(Color(0xFF3B82F6), "RESTARTING")
        "error" -> Pair(Color(0xFFEF4444), "ERROR")
        "online" -> Pair(Color(0xFF10B981), "ONLINE")
        "offline" -> Pair(Color(0xFFEF4444), "OFFLINE")
        else -> Pair(Color(0xFF6B7280), status.uppercase())
    }

    val isAnimated = status in listOf("building", "restarting")
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color.copy(alpha = alpha), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun SslBadge(sslStatus: String, modifier: Modifier = Modifier) {
    val (color, label, icon) = when (sslStatus.lowercase()) {
        "valid" -> Triple(Color(0xFF10B981), "SSL VALID", "🔒")
        "pending" -> Triple(Color(0xFFF59E0B), "SSL PENDING", "⏳")
        "expired" -> Triple(Color(0xFFEF4444), "SSL EXPIRED", "⚠️")
        "error" -> Triple(Color(0xFFEF4444), "SSL ERROR", "✗")
        else -> Triple(Color(0xFF6B7280), "NO SSL", "🔓")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = icon, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
    }
}
