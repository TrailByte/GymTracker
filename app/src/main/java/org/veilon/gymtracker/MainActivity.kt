package org.veilon.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.veilon.gymtracker.ui.ActiveWorkoutViewModel
import org.veilon.gymtracker.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Log : Screen("log", "Log", Icons.AutoMirrored.Filled.List)
    object Templates : Screen("templates", "Plans", Icons.Default.Star)
    object Progress : Screen("progress", "Stats", Icons.AutoMirrored.Filled.TrendingUp)
    object Settings : Screen("settings", "Setup", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GymTrackerApp()
            }
        }
    }
}

@Composable
fun GymTrackerApp(activeVm: ActiveWorkoutViewModel = viewModel()) {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Home, Screen.Log, Screen.Templates, Screen.Progress, Screen.Settings)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val activeSessionId by activeVm.activeSessionId.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Templates detail is a pushed route; hide bar there
            val showBar = currentRoute in tabs.map { it.route }
            if (showBar) {
                NavigationBar {
                    tabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartWorkout = { name ->
                        activeVm.startEmpty(name) {
                            navController.navigate(Screen.Log.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onOpenSession = { /* read-only view comes in E4 */ }
                )
            }
            composable(Screen.Log.route) {
                LogScreen(
                    activeSessionId = activeSessionId,
                    onClearActive = { activeVm.clearActive() },
                    onGoToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Templates.route) {
                TemplatesScreen(onOpenTemplate = { templateId ->
                    navController.navigate("template/$templateId")
                })
            }
            composable("template/{templateId}") { backStack ->
                val templateId = backStack.arguments?.getString("templateId")?.toLong() ?: return@composable
                TemplateDetailScreen(
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}