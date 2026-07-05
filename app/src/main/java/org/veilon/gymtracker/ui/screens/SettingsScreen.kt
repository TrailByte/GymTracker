package org.veilon.gymtracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.gamification.Achievements
import org.veilon.gymtracker.gamification.Celebration
import org.veilon.gymtracker.gamification.CelebrationBus
import org.veilon.gymtracker.ui.SettingsViewModel
import org.veilon.gymtracker.ui.theme.ScreenTitle
import org.veilon.gymtracker.ui.theme.ThemeUnlocks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val useLbs by viewModel.useLbs.collectAsState()
    val restSeconds by viewModel.restSeconds.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val (level, prestige) = viewModel.levelAndPrestige.collectAsState().value
    val backupStatus by viewModel.backupStatus.collectAsState()

    var showImportWarning by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importBackup(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScreenTitle("Settings")
        Spacer(Modifier.height(4.dp))

        SectionLabel("APPEARANCE")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold)
                Text(
                    "Unlock new looks by leveling up and prestiging",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                val allTiers = ThemeUnlocks.LADDER + ThemeUnlocks.PRESTIGE_THEME
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
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        } else if (!isUnlocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index != allTiers.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("WORKOUT")
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weight unit", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (useLbs) "Pounds (lbs)" else "Kilograms (kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Text(
                    "New workouts start with this rest duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        "${restSeconds / 60}:${String.format(Locale.US, "%02d", restSeconds % 60)}",
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
                Text(
                    "Workouts per week to keep your streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        Spacer(Modifier.height(16.dp))
        SectionLabel("DATA")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup & Restore", fontWeight = FontWeight.SemiBold)
                Text(
                    "The app stores everything on this device only. Export a " +
                        "backup file to move your data to a new phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            exportLauncher.launch("forj_backup_$stamp.zip")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export Backup") }
                    OutlinedButton(
                        onClick = { showImportWarning = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import Backup") }
                }
                backupStatus?.let { status ->
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { viewModel.clearBackupStatus() }) { Text("Dismiss") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("DEBUG")
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
                            CelebrationBus.push(
                                listOf(
                                    Celebration.LevelUp(
                                        newLevel = 5,
                                        prestige = 0,
                                        newTheme = ThemeUnlocks.themeUnlockedAtLevel(5)
                                    )
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Preview Level Up") }
                    OutlinedButton(
                        onClick = {
                            CelebrationBus.push(
                                listOf(Celebration.AchievementUnlocked(Achievements.ALL.first()))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Preview Achievement") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            title = { Text("Replace all current data?") },
            text = {
                Text(
                    "Importing a backup replaces every workout, exercise, and " +
                        "achievement currently on this device with what's in the " +
                        "backup file. This can't be undone. After importing, " +
                        "you'll need to close and reopen the app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportWarning = false
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }) { Text("Replace Data") }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarning = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
