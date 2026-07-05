package com.example.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DatabaseInstance
import com.example.ui.viewmodel.DatabaseUiState
import com.example.ui.viewmodel.DatabaseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabasesScreen(
    viewModel: DatabaseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Databases", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadDatabases() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is DatabaseUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DatabaseUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is DatabaseUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.databases) { db ->
                            DatabaseCard(
                                db = db,
                                onInstall = { viewModel.installDatabase(db.id) },
                                onStart = { viewModel.startDatabase(db.id) },
                                onStop = { viewModel.stopDatabase(db.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseCard(
    db: DatabaseInstance,
    onInstall: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = db.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (db.isInstalled) {
                    if (db.isRunning) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("Running") }
                    } else {
                        Badge(containerColor = MaterialTheme.colorScheme.error) { Text("Stopped") }
                    }
                } else {
                    Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("Not Installed") }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Port: ${db.port}", style = MaterialTheme.typography.bodyMedium)
            
            if (db.isInstalled && db.connectionString != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Conn: ${db.connectionString}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(db.connectionString)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!db.isInstalled) {
                    Button(onClick = onInstall) {
                        Text("Install")
                    }
                } else {
                    if (db.isRunning) {
                        OutlinedButton(onClick = onStop) {
                            Text("Stop")
                        }
                    } else {
                        Button(onClick = onStart) {
                            Text("Start")
                        }
                    }
                }
            }
        }
    }
}
