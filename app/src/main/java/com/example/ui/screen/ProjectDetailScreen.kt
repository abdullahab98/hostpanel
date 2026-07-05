package com.example.ui.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EnvVarEditor
import com.example.ui.components.FrameworkChip
import com.example.ui.components.LogConsole
import com.example.ui.components.StatusBadge
import com.example.ui.components.SslBadge
import com.example.ui.viewmodel.ProjectDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectName: String,
    onBack: () -> Unit,
    viewModel: ProjectDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tabs = listOf("Logs", "Environment", "Domain", "Info")

    LaunchedEffect(projectName) {
        viewModel.loadProject(projectName)
    }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab == 0) viewModel.startLogStream(projectName)
    }

    LaunchedEffect(state.actionMessage) {
        if (state.actionMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(projectName, fontWeight = FontWeight.Bold)
                        state.project?.let { FrameworkChip(it.framework) }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.project == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            state.project?.let { project ->
                // Action bar
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(project.status)
                            Spacer(Modifier.weight(1f))
                            if (project.sslEnabled) SslBadge("valid")
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (project.status != "running") {
                                OutlinedButton(onClick = { viewModel.startContainer(projectName) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                                    Spacer(Modifier.width(4.dp)); Text("Start")
                                }
                            }
                            if (project.status == "running") {
                                OutlinedButton(onClick = { viewModel.stopContainer(projectName) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                                    Spacer(Modifier.width(4.dp)); Text("Stop")
                                }
                            }
                            OutlinedButton(onClick = { viewModel.restartContainer(projectName) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Restart")
                            }
                            OutlinedButton(onClick = { viewModel.rebuildContainer(projectName) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp), tint = Color(0xFFA855F7))
                                Spacer(Modifier.width(4.dp)); Text("Rebuild")
                            }
                        }
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = state.selectedTab == i,
                            onClick = { viewModel.setTab(i) },
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                Box(Modifier.weight(1f).padding(16.dp)) {
                    when (state.selectedTab) {
                        0 -> {
                            // Logs tab
                            Column(Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(14.dp), tint = Color(0xFF60A5FA))
                                    Spacer(Modifier.width(6.dp))
                                    Text("LIVE LOGS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF60A5FA))
                                    if (state.isStreamingLogs) {
                                        Spacer(Modifier.width(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFF10B981))
                                    }
                                }
                                LogConsole(logs = state.logs, modifier = Modifier.fillMaxSize())
                            }
                        }
                        1 -> {
                            // Env vars tab
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                EnvVarEditor(
                                    envVars = state.envVars,
                                    onAdd = viewModel::addEnvVar,
                                    onUpdate = viewModel::updateEnvVar,
                                    onRemove = viewModel::removeEnvVar
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.saveEnvVars(projectName) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Save Environment Variables")
                                }
                            }
                        }
                        2 -> {
                            // Domain tab
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                project.customDomain?.let { domain ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("Active Domain", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(8.dp))
                                            Text(domain, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(8.dp))
                                            SslBadge(if (project.sslEnabled) "valid" else "pending")
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    val url = "https://$domain"
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Open in Browser")
                                            }
                                        }
                                    }
                                } ?: run {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No custom domain configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Info tab
                            val clipboardManager = LocalClipboardManager.current
                            val tunnelUrlFlow = remember { com.example.data.datastore.SettingsDataStore(context).tunnelUrl }
                            val tunnelUrl by tunnelUrlFlow.collectAsState(initial = "")
                            val apiKeyFlow = remember { com.example.data.datastore.SettingsDataStore(context).apiKey }
                            val apiKey by apiKeyFlow.collectAsState(initial = "hostpanel-local")

                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                
                                // Webhook Section
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("CI/CD Webhook URL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Paste this into GitHub Settings > Webhooks to auto-rebuild on push.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(8.dp))
                                        
                                        val webhookUrl = if (tunnelUrl.isNotEmpty()) "$tunnelUrl/api/webhook/$projectName?token=$apiKey" else "Tunnel not running"
                                        Text(webhookUrl, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = if(tunnelUrl.isNotEmpty()) Color.Unspecified else MaterialTheme.colorScheme.error)
                                        
                                        if (tunnelUrl.isNotEmpty()) {
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(webhookUrl)) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy Webhook URL")
                                            }
                                        }
                                    }
                                }

                                InfoRow("Container ID", project.id)
                                InfoRow("Image", project.image)
                                InfoRow("Ports", project.ports.ifEmpty { "N/A" })
                                InfoRow("Created", project.created)
                                InfoRow("Uptime", project.uptime.ifEmpty { "N/A" })
                                InfoRow("Git URL", project.gitUrl ?: "N/A")
                                InfoRow("Branch", project.branch)
                                InfoRow("CPU", "${project.cpuPercent}%")
                                InfoRow("Memory", "${"%.0f".format(project.memoryUsageMb)} MB")
                            }
                        }
                    }
                }

                // Snackbar
                state.actionMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (msg.startsWith("✓")) Color(0xFF166534) else Color(0xFF7F1D1D)
                    ) { Text(msg) }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}
