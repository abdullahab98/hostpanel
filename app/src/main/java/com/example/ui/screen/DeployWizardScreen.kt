package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Framework
import com.example.ui.components.EnvVarEditor
import com.example.ui.components.LogConsole
import com.example.ui.viewmodel.DeployUiState
import com.example.ui.viewmodel.DeployViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployWizardScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: DeployViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val steps = listOf("Source", "Framework", "Config", "Domain", "Deploy")

    LaunchedEffect(state.deploySuccess) {
        if (state.deploySuccess) {
            kotlinx.coroutines.delay(2000)
            viewModel.reset()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy Project", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.currentStep == 0) onBack() else viewModel.prevStep()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { i, label ->
                    val isActive = i == state.currentStep
                    val isDone = i < state.currentStep
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(
                                when { isDone -> Color(0xFF10B981); isActive -> MaterialTheme.colorScheme.primary; else -> Color(0xFF1E293B) }
                            ), contentAlignment = Alignment.Center
                        ) {
                            if (isDone) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                            else Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color.White else Color(0xFF475569))
                        }
                        Text(label, fontSize = 9.sp, color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF475569), modifier = Modifier.padding(top = 2.dp))
                    }
                    if (i < steps.size - 1) {
                        Box(Modifier.weight(0.3f).height(1.dp).background(if (isDone) Color(0xFF10B981) else Color(0xFF1E293B)))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                when (state.currentStep) {
                    0 -> StepSource(state.gitUrl, state.branch, viewModel::setGitUrl, viewModel::setBranch)
                    1 -> StepFramework(state.selectedFramework, viewModel::setFramework)
                    2 -> StepConfig(state, viewModel)
                    3 -> StepDomain(state.customDomain, state.useAutoDomain, viewModel::setCustomDomain, viewModel::setUseAutoDomain)
                    4 -> StepDeploy(state, viewModel)
                }
            }

            // Bottom buttons
            if (state.currentStep < 4) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    val canNext = when (state.currentStep) {
                        0 -> state.gitUrl.isNotBlank()
                        2 -> state.projectName.isNotBlank()
                        else -> true
                    }
                    Button(
                        onClick = {
                            if (state.currentStep == 3) viewModel.nextStep().also { viewModel.startDeployment() }
                            else viewModel.nextStep()
                        },
                        enabled = canNext,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (state.currentStep == 3) "Deploy Now 🚀" else "Continue")
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSource(gitUrl: String, branch: String, setUrl: (String) -> Unit, setBranch: (String) -> Unit) {
    Column {
        SectionTitle("Git Repository URL")
        OutlinedTextField(
            value = gitUrl, onValueChange = setUrl,
            placeholder = { Text("https://github.com/user/repo.git") },
            leadingIcon = { Icon(Icons.Default.Code, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Branch")
        OutlinedTextField(
            value = branch, onValueChange = setBranch,
            placeholder = { Text("main") },
            leadingIcon = { Icon(Icons.Default.Source, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun StepFramework(selected: Framework, onSelect: (Framework) -> Unit) {
    Column {
        SectionTitle("Select Framework")
        Spacer(Modifier.height(8.dp))
        Framework.entries.forEach { fw ->
            val isSelected = selected == fw
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onSelect(fw) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fw.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(fw.displayName, fontWeight = FontWeight.Medium)
                        Text("Port ${fw.defaultPort}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StepConfig(state: DeployUiState, vm: DeployViewModel) {
    Column {
        SectionTitle("Project Name")
        OutlinedTextField(
            value = state.projectName, onValueChange = vm::setProjectName,
            placeholder = { Text("my-awesome-app") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Port")
        OutlinedTextField(
            value = state.targetPort, onValueChange = vm::setTargetPort,
            placeholder = { Text(state.selectedFramework.defaultPort.toString()) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Environment Variables")
        EnvVarEditor(
            envVars = state.envVars,
            onAdd = vm::addEnvVar,
            onUpdate = vm::updateEnvVar,
            onRemove = vm::removeEnvVar
        )
    }
}

@Composable
private fun StepDomain(domain: String, useAuto: Boolean, setDomain: (String) -> Unit, setAuto: (Boolean) -> Unit) {
    Column {
        SectionTitle("Domain Configuration")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = useAuto, onClick = { setAuto(true) })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Auto-assign subdomain", fontWeight = FontWeight.Medium)
                        Text("e.g. myapp.yourserver.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !useAuto, onClick = { setAuto(false) })
                    Spacer(Modifier.width(8.dp))
                    Text("Custom domain", fontWeight = FontWeight.Medium)
                }
                if (!useAuto) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = domain, onValueChange = setDomain,
                        placeholder = { Text("myapp.com") },
                        leadingIcon = { Icon(Icons.Default.Language, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2F)), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "💡 Point your domain's A record to your server's IP before deploying. SSL will be provisioned automatically via Let's Encrypt.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF86EFAC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDeploy(state: DeployUiState, vm: DeployViewModel) {
    Column {
        if (state.deploySuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Deployment Successful!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                    state.deployUrl?.let { url ->
                        Spacer(Modifier.height(8.dp))
                        Text(url, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF60A5FA))
                    }
                }
            }
        }
        if (state.isDeploying || state.deployLogs.isNotEmpty()) {
            SectionTitle("Build Log")
            Spacer(Modifier.height(8.dp))
            LogConsole(logs = state.deployLogs, modifier = Modifier.fillMaxWidth().height(300.dp))
            if (state.isDeploying) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Deploying...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        state.error?.let { err ->
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1515)), shape = RoundedCornerShape(12.dp)) {
                Text(err, modifier = Modifier.padding(16.dp), color = Color(0xFFEF4444), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}
