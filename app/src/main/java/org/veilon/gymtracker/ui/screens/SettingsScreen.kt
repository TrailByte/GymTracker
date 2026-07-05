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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val useLbs by viewModel.useLbs.collectAsState()
    val restSeconds by viewModel.restSeconds.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val (level, prestige) = viewModel.levelAndPrestige.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
            Column(Modifier.padding(16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold)
                Text(
                    "Unlock new looks by leveling up and prestiging",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                val allTiers = org.veilon.gymtracker.ui.theme.ThemeUnlocks.LADDER +
                        org.veilon.gymtracker.ui.theme.ThemeUnlocks.PRESTIGE_THEME
                allTiers.forEachIndexed { index, tier ->
                    val isUnlocked = if (tier.id == "prestige") prestige > 0 else level >= tier.unlockLevel
                    val isSelected = selectedTheme == tier.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { m -> if (isUnlocked) m.clickable { viewModel.setSelectedTheme(tier.id) } else m }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                tier.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isUnlocked) {
                                Text(
                                    if (tier.id == "prestige") "Unlocks via Prestige"
                                    else "Unlocks at Level ${tier.unlockLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else if (!isUnlocked) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index != allTiers.lastIndex) {
                        HorizontalDivider()
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