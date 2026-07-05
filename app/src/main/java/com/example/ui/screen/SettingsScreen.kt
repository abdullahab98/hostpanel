package com.example.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        if (state.message != null) { kotlinx.coroutines.delay(3000); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

                // Connection status indicator
                val connColor = when (state.connectionStatus) {
                    ConnectionStatus.CONNECTED -> Color(0xFF10B981)
                    ConnectionStatus.FAILED -> Color(0xFFEF4444)
                    ConnectionStatus.CHECKING -> Color(0xFFF59E0B)
                    ConnectionStatus.UNCHECKED -> Color(0xFF475569)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (state.connectionStatus) {
                                ConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                                ConnectionStatus.FAILED -> Icons.Default.Cancel
                                ConnectionStatus.CHECKING -> Icons.Default.Sync
                                ConnectionStatus.UNCHECKED -> Icons.Default.Circle
                            },
                            contentDescription = null, tint = connColor, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when (state.connectionStatus) {
                                ConnectionStatus.CONNECTED -> "Connected to Control Plane"
                                ConnectionStatus.FAILED -> "Cannot reach server"
                                ConnectionStatus.CHECKING -> "Testing connection..."
                                ConnectionStatus.UNCHECKED -> "Not tested yet"
                            },
                            fontWeight = FontWeight.Medium, color = connColor
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                SectionHeader("Control Plane Server")

                OutlinedTextField(
                    value = state.serverUrl, onValueChange = viewModel::setServerUrl,
                    label = { Text("Server URL") },
                    placeholder = { Text("https://your-server.com") },
                    leadingIcon = { Icon(Icons.Default.Cloud, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.apiKey, onValueChange = viewModel::setApiKey,
                    label = { Text("API Secret Key") },
                    placeholder = { Text("your-jwt-secret-key") },
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.serverDomain, onValueChange = viewModel::setServerDomain,
                    label = { Text("Server Base Domain (optional)") },
                    placeholder = { Text("yourserver.com") },
                    leadingIcon = { Icon(Icons.Default.Language, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.testConnection() }, modifier = Modifier.weight(1f)) {
                        if (state.connectionStatus == ConnectionStatus.CHECKING) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Test Connection")
                    }
                    Button(onClick = { viewModel.save() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(24.dp))

                SectionHeader("Cloudflare Tunnel")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable Tunnel", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Switch(checked = state.isTunnelEnabled, onCheckedChange = viewModel::setTunnelEnabled)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Cloudflare Tunnel provides public HTTPS access without port forwarding or a static IP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.isTunnelEnabled) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.tunnelToken, onValueChange = viewModel::setTunnelToken,
                                label = { Text("Tunnel Token") },
                                placeholder = { Text("eyJhIjoiLi4u") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                SectionHeader("App Preferences")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Refresh Metrics", fontWeight = FontWeight.Medium)
                            Text("Refresh every 15 seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.autoRefresh, onCheckedChange = viewModel::setAutoRefresh)
                    }
                }
                Spacer(Modifier.height(24.dp))

                SectionHeader("About")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row { Text("App", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("HostPanel v3.0") }
                        Spacer(Modifier.height(4.dp))
                        Row { Text("Backend", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Node.js (Termux-native)") }
                        Spacer(Modifier.height(4.dp))
                        Row { Text("Database", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("MariaDB / PostgreSQL / Redis") }
                        Spacer(Modifier.height(4.dp))
                        Row { Text("Public URL", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Cloudflare / ngrok Tunnel") }
                        Spacer(Modifier.height(4.dp))
                        Row { Text("CI/CD", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("GitHub Webhook → auto-rebuild") }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }

            state.message?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = if (msg.startsWith("✓")) Color(0xFF166534) else Color(0xFF7F1D1D)
                ) { Text(msg) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
