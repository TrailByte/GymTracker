package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.SettingsViewModel
import org.veilon.gymtracker.ui.theme.ScreenTitle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val useLbs by viewModel.useLbs.collectAsState()
    val restSeconds by viewModel.restSeconds.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTitle("Settings")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Appearance", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = themeMode == value,
                                onClick = { viewModel.setThemeMode(value) },
                                label = { Text(label) }
                            )
                        }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weight unit", fontWeight = FontWeight.SemiBold)
                    Text(if (useLbs) "Pounds (lbs)" else "Kilograms (kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("kg")
                    Switch(checked = useLbs, onCheckedChange = { viewModel.setUseLbs(it) })
                    Text("lbs")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Default rest timer", fontWeight = FontWeight.SemiBold)
                Text("New workouts start with this rest duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { viewModel.setRestSeconds(restSeconds - 15) }) {
                        Text("-15s")
                    }
                    Text(
                        "${restSeconds / 60}:${String.format(java.util.Locale.US, "%02d", restSeconds % 60)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedButton(onClick = { viewModel.setRestSeconds(restSeconds + 15) }) {
                        Text("+15s")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Weekly goal", fontWeight = FontWeight.SemiBold)
                Text("Workouts per week to keep your streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { viewModel.setWeeklyGoal(weeklyGoal - 1) }) {
                        Text("-")
                    }
                    Text("$weeklyGoal", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { viewModel.setWeeklyGoal(weeklyGoal + 1) }) {
                        Text("+")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "DEBUG",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Test celebration overlay", fontWeight = FontWeight.SemiBold)
                Text(
                    "Preview only — doesn't touch real XP or achievements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            org.veilon.gymtracker.gamification.CelebrationBus.push(
                                listOf(
                                    org.veilon.gymtracker.gamification.Celebration.LevelUp(
                                        newLevel = 5,
                                        prestige = 0,
                                        newTheme = org.veilon.gymtracker.ui.theme.ThemeUnlocks.themeUnlockedAtLevel(5)
                                    )
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Preview Level Up") }
                    OutlinedButton(
                        onClick = {
                            org.veilon.gymtracker.gamification.CelebrationBus.push(
                                listOf(
                                    org.veilon.gymtracker.gamification.Celebration.AchievementUnlocked(
                                        org.veilon.gymtracker.gamification.Achievements.ALL.first()
                                    )
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Preview Achievement") }
                }
            }
        }
    }
}