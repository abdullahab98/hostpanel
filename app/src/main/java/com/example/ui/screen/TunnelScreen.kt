package com.example.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.datastore.SettingsDataStore
import com.example.ui.viewmodel.TunnelViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen() {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }

    // Collect server settings to pass into factory
    val serverUrl by settingsDataStore.serverUrl.collectAsState(initial = "http://localhost:3001")
    val apiKey by settingsDataStore.apiKey.collectAsState(initial = "hostpanel-local")

    // Create ViewModel using the Factory — no Hilt required
    val viewModel: TunnelViewModel = viewModel(
        factory = TunnelViewModel.Factory(settingsDataStore, serverUrl, apiKey)
    )

    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var selectedProvider by remember { mutableStateOf("cloudflare") }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(4000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Public Tunnel", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.checkStatus() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Expose Local Server to Internet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Since HostPanel is running on your phone, you need a tunnel to access your projects from the internet. Cloudflare Quick Tunnels are free and require no account.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Provider selection
                if (!state.isRunning) {
                    Text("Select Tunnel Provider:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                        FilterChip(
                            selected = selectedProvider == "cloudflare",
                            onClick = { selectedProvider = "cloudflare" },
                            label = { Text("Cloudflare") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedProvider == "ngrok",
                            onClick = { selectedProvider = "ngrok" },
                            label = { Text("ngrok") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isRunning) Color(0xFF1E3A2F) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Please wait...")
                        } else if (state.isRunning) {
                            Text(
                                "Tunnel is Active",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                state.activeUrl,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(state.activeUrl))
                                }) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }

                                Button(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.activeUrl)))
                                }) {
                                    Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open")
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = { viewModel.stopTunnel() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Text("Stop Tunnel")
                            }
                        } else {
                            Text("Tunnel is Offline", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.startTunnel(selectedProvider) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Tunnel")
                            }
                        }
                    }
                }
            }

            // Snackbar
            state.message?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = if (msg.startsWith("✓")) Color(0xFF166534) else Color(0xFF7F1D1D)
                ) { Text(msg) }
            }
        }
    }
}
