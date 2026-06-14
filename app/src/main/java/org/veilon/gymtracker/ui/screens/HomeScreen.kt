package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.ExercisePR
import org.veilon.gymtracker.ui.HomeViewModel
import org.veilon.gymtracker.ui.formatWeight
import org.veilon.gymtracker.ui.theme.PlateBadge
import org.veilon.gymtracker.ui.theme.ScreenTitle
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onStartEmpty: (String) -> Unit,
    onStartFromTemplate: (templateId: Long, name: String) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sessions by viewModel.recentSessions.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()

    var showChoice by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }

    if (showChoice) {
        AlertDialog(
            onDismissRequest = { showChoice = false },
            title = { Text("Start Workout") },
            text = { Text("How do you want to start?") },
            confirmButton = {
                TextButton(onClick = { showChoice = false; showNameDialog = true }) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = { showChoice = false; showTemplatePicker = true }) { Text("From Template") }
            }
        )
    }
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("New Workout") },
            text = {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session name (e.g. Push Day)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (sessionName.isNotBlank()) {
                        onStartEmpty(sessionName.trim()); showNameDialog = false; sessionName = ""
                    }
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel") } }
        )
    }
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Pick Template") },
            text = {
                if (templates.isEmpty()) {
                    Text("No templates yet. Create one in the Plans tab.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(templates) { template ->
                            TextButton(
                                onClick = { onStartFromTemplate(template.id, template.name); showTemplatePicker = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(template.name, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            val today = remember {
                SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
            }
            Column {
                Text(today, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                ScreenTitle("Ready to train?")
            }
        }

        // Stat cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(Modifier.weight(1f), stats.workoutsThisWeek.toString(), "This Week")
                StatCard(Modifier.weight(1f), stats.weekStreak.toString(), "Week Streak")
                val totalDisplay = if (useLbs)
                    "${(stats.totalVolumeKg * 2.20462 / 1000).roundToInt()}K"
                else
                    "${(stats.totalVolumeKg / 1000).roundToInt()}K"
                StatCard(Modifier.weight(1f), totalDisplay, if (useLbs) "Total Klbs" else "Total Tons")
            }
        }

        // Start button
        item {
            Button(
                onClick = { showChoice = true },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("START WORKOUT", fontWeight = FontWeight.Bold)
            }
        }

        // Recent PRs
        if (stats.prs.isNotEmpty()) {
            item {
                Text("RECENT PRS", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(stats.prs) { pr ->
                PRCard(pr, useLbs)
            }
        }

        // Recent workouts
        item {
            Text("RECENT WORKOUTS", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sessions.isEmpty()) {
            item {
                Text("No workouts yet. Hit Start!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(sessions.take(5)) { session ->
                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                val date = remember(session.date, locale) {
                    SimpleDateFormat("EEE, MMM d · h:mm a", locale).format(Date(session.date))
                }
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenSession(session.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(session.name, fontWeight = FontWeight.SemiBold)
                        Text(date, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PRCard(pr: ExercisePR, useLbs: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val plateValue = if (useLbs) (pr.maxWeightKg * 2.20462).roundToInt().toString()
            else pr.maxWeightKg.roundToInt().toString()
            PlateBadge(value = plateValue)
            Column(Modifier.weight(1f)) {
                Text(pr.exerciseName, fontWeight = FontWeight.SemiBold)
                Text("Best set: ${formatWeight(pr.maxWeightKg, useLbs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Best volume: ${pr.maxVolumeReps}×${formatWeight(pr.maxVolumeWeightKg, useLbs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}