package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screen.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = com.example.data.datastore.SettingsDataStore(this)
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = "dark")
            MyApplicationTheme(themeMode = themeMode) {
                HostPanelApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Projects : Screen("projects", "Projects", Icons.Default.Cloud)
    object Tunnel : Screen("tunnel", "Tunnel", Icons.Default.Public)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Dashboard, Screen.Projects, Screen.Tunnel, Screen.Terminal, Screen.Settings
)

@Composable
fun HostPanelApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { HostPanelBottomNav(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "setup", // Start at setup by default, it will auto-forward
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("setup") {
                SetupScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onDeployClick = { navController.navigate("deploy") }
                )
            }

            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onProjectClick = { name -> navController.navigate("project_detail/$name") },
                    onDeployClick = { navController.navigate("deploy") }
                )
            }

            composable(Screen.Tunnel.route) {
                TunnelScreen()
            }

            composable(Screen.Terminal.route) {
                TerminalScreen()
            }

            composable(Screen.Settings.route) {
                // Assuming SettingsScreen has a button to navigate to databases
                Column {
                    SettingsScreen()
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("databases") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text("Manage Native Databases")
                    }
                }
            }

            // Full-screen flows (not in bottom nav)
            composable("databases") {
                DatabasesScreen()
            }

            composable("deploy") {
                DeployWizardScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.Projects.route) {
                            popUpTo("deploy") { inclusive = true }
                        }
                    }
                )
            }

            composable("project_detail/{name}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: return@composable
                ProjectDetailScreen(
                    projectName = name,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun HostPanelBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val fullScreenRoutes = listOf("setup", "deploy", "project_detail/{name}", "databases")

    // Hide bottom bar on full-screen pages
    if (fullScreenRoutes.any { currentRoute?.startsWith(it.substringBefore("{")) == true }) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
