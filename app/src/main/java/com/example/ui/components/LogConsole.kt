package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogConsole(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF020408))
            .border(1.dp, Color(0xFF1A2540), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        LazyColumn(state = listState) {
            items(logs) { line ->
                val color = when {
                    line.startsWith("[ERROR]") || line.startsWith("> Error") -> Color(0xFFFF6B6B)
                    line.startsWith("[SUCCESS]") -> Color(0xFF00E676)
                    line.startsWith("[INFO]") -> Color(0xFF60A5FA)
                    line.startsWith("[WARN]") -> Color(0xFFFBBF24)
                    line.startsWith("> ") -> Color(0xFF10B981)
                    else -> Color(0xFF94A3B8)
                }
                Text(
                    text = line,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
