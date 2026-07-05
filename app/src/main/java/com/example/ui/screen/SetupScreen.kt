package com.example.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCheckingServer by remember { mutableStateOf(false) }
    var serverRunning by remember { mutableStateOf(false) }

    // Auto-check for localhost:3001
    LaunchedEffect(Unit) {
        while (!serverRunning) {
            try {
                isCheckingServer = true
                val url = URL("http://localhost:3001/api/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                
                if (connection.responseCode == 200) {
                    serverRunning = true
                    delay(1000)
                    onSetupComplete()
                }
            } catch (e: Exception) {
                // Ignore, server not running yet
            } finally {
                isCheckingServer = false
            }
            delay(3000) // Poll every 3 seconds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android-Native Setup", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to HostPanel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "To run projects directly on your phone without a VPS, we need to install the Control Plane in Termux.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Step 1
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Install Termux", fontWeight = FontWeight.Bold)
                    Text(
                        "Please install Termux from F-Droid (Play Store version is broken).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open F-Droid")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Run Setup Command", fontWeight = FontWeight.Bold)
                    Text(
                        "Open Termux and paste the following command to install the Control Plane:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    
                    val setupCmd = "pkg update -y && pkg install -y nodejs git python && npm install -g nodemon && git clone https://github.com/hostpanel-dev/control-plane.git ~/hostpanel-control-plane && cd ~/hostpanel-control-plane && npm install && cp .env.example .env && node index.js"
                    
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(setupCmd))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Setup Command")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status checker
            if (serverRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Control Plane Connected!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Waiting for connection on localhost:3001...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
