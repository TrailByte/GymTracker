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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.veilon.gymtracker.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
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
fun GymTrackerApp() {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.Home, Screen.Templates, Screen.Progress, Screen.Settings)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Hide the bottom bar while in an active workout
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { screen ->
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
                HomeScreen(onStartWorkout = { sessionId ->
                    navController.navigate("workout/$sessionId/My Workout")
                })
            }
            composable("workout/{sessionId}/{sessionName}") { backStack ->
                val sessionId = backStack.arguments?.getString("sessionId")?.toLong() ?: return@composable
                val sessionName = backStack.arguments?.getString("sessionName") ?: "Workout"
                WorkoutScreen(
                    sessionId = sessionId,
                    sessionName = sessionName,
                    onFinish = { navController.popBackStack() }
                )
            }
            composable(Screen.Templates.route) { TemplatesScreen() }
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}