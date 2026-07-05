package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onDeployClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HOST PANEL", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(
                            if (state.isConnected) "● Connected" else "○ Disconnected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMetrics() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onDeployClick,
                icon = { Icon(Icons.Default.Add, "Deploy") },
                text = { Text("Deploy Project") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state.isLoading && state.metrics == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1515)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Connection Error", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            state.metrics?.let { m ->
                // Hero banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E3A5F), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(if (m.activeContainers > 0) "running" else "stopped")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${m.activeContainers} Container${if (m.activeContainers != 1) "s" else ""} Active",
                                color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${m.osInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF60A5FA)
                        )
                        Text(
                            m.dockerVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Uptime: ${formatUptime(m.uptimeSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // CPU + RAM
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        label = "CPU",
                        value = "${m.cpuUsagePercent.roundToInt()}%",
                        subtitle = "usage",
                        progress = (m.cpuUsagePercent / 100f).toFloat(),
                        accentColor = when {
                            m.cpuUsagePercent > 80 -> Color(0xFFEF4444)
                            m.cpuUsagePercent > 50 -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "RAM",
                        value = "${"%.1f".format(m.ramUsedGb)} GB",
                        subtitle = "of ${"%.1f".format(m.ramTotalGb)} GB",
                        progress = if (m.ramTotalGb > 0) (m.ramUsedGb / m.ramTotalGb).toFloat() else 0f,
                        accentColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Disk + Network
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        label = "DISK",
                        value = "${"%.1f".format(m.diskUsedGb)} GB",
                        subtitle = "of ${"%.1f".format(m.diskTotalGb)} GB",
                        progress = if (m.diskTotalGb > 0) (m.diskUsedGb / m.diskTotalGb).toFloat() else 0f,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "NETWORK",
                        value = "↓${"%.1f".format(m.networkRxMb)}",
                        subtitle = "↑${"%.1f".format(m.networkTxMb)} MB",
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Recent activity
            if (state.recentLogs.isNotEmpty()) {
                Text(
                    "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                state.recentLogs.forEach { log ->
                    val statusColor = when (log.status) {
                        "SUCCESS" -> Color(0xFF10B981)
                        "FAILURE" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${log.action} — ${log.target}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(log.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(log.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
