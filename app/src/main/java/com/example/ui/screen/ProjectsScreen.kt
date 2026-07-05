package com.example.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Project
import com.example.ui.components.FrameworkChip
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.ProjectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit,
    onDeployClick: () -> Unit,
    viewModel: ProjectsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.actionMessage) {
        if (state.actionMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onDeployClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "Deploy") }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.projects.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.projects.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(64.dp), tint = Color(0xFF334155))
                            Spacer(Modifier.height(16.dp))
                            Text("No projects deployed yet", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onDeployClick) { Text("Deploy your first project") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.projects, key = { it.id }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.name) },
                                onStart = { viewModel.startContainer(project.name) },
                                onStop = { viewModel.stopContainer(project.name) },
                                onRestart = { viewModel.restartContainer(project.name) },
                                onDelete = { viewModel.deleteContainer(project.name) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Snackbar
            state.actionMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = if (msg.startsWith("✓")) Color(0xFF166534) else Color(0xFF7F1D1D)
                ) { Text(msg) }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${project.name}?") },
            text = { Text("This will permanently delete the container and all its data.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    FrameworkChip(project.framework)
                }
                StatusBadge(project.status)
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        if (project.status != "running") {
                            DropdownMenuItem(
                                text = { Text("Start") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF10B981)) },
                                onClick = { onStart(); showMenu = false }
                            )
                        }
                        if (project.status == "running") {
                            DropdownMenuItem(
                                text = { Text("Stop") },
                                leadingIcon = { Icon(Icons.Default.Stop, null, tint = Color(0xFFF59E0B)) },
                                onClick = { onStop(); showMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Restart") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, tint = Color(0xFF3B82F6)) },
                            onClick = { onRestart(); showMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) },
                            onClick = { showDeleteDialog = true; showMenu = false }
                        )
                    }
                }
            }

            project.customDomain?.let { domain ->
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .clickable {
                            val url = if (project.sslEnabled) "https://$domain" else "http://$domain"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(12.dp), tint = Color(0xFF60A5FA))
                    Spacer(Modifier.width(4.dp))
                    Text(domain, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF60A5FA))
                }
            }

            if (project.cpuPercent > 0 || project.memoryUsageMb > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        "CPU ${project.cpuPercent}%  •  RAM ${"%.0f".format(project.memoryUsageMb)}MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (project.uptime.isNotEmpty()) {
                        Text(
                            "  •  Up ${project.uptime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
