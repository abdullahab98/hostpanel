package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Framework

@Composable
fun FrameworkChip(framework: String, modifier: Modifier = Modifier) {
    val fw = Framework.fromId(framework)
    val bgColor = when (framework) {
        "nodejs" -> Color(0xFF166534)
        "nextjs" -> Color(0xFF1F2937)
        "react" -> Color(0xFF1E3A5F)
        "django" -> Color(0xFF14532D)
        "springboot" -> Color(0xFF7F1D1D)
        "static" -> Color(0xFF1E293B)
        else -> Color(0xFF1E293B)
    }
    val textColor = when (framework) {
        "nodejs" -> Color(0xFF4ADE80)
        "nextjs" -> Color(0xFFD1D5DB)
        "react" -> Color(0xFF60A5FA)
        "django" -> Color(0xFF86EFAC)
        "springboot" -> Color(0xFFFCA5A5)
        else -> Color(0xFF94A3B8)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = fw.emoji, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = fw.displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
