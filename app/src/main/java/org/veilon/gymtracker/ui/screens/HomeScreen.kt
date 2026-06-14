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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onStartEmpty: (String) -> Unit,
    onStartFromTemplate: (templateId: Long, name: String) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sessions by viewModel.recentSessions.collectAsState()
    val templates by viewModel.templates.collectAsState()

    var showChoice by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }

    // Step 1: choose Empty or From Template
    if (showChoice) {
        AlertDialog(
            onDismissRequest = { showChoice = false },
            title = { Text("Start Workout") },
            text = { Text("How do you want to start?") },
            confirmButton = {
                TextButton(onClick = {
                    showChoice = false; showNameDialog = true
                }) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChoice = false; showTemplatePicker = true
                }) { Text("From Template") }
            }
        )
    }

    // Step 2a: name an empty workout
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
                        onStartEmpty(sessionName.trim())
                        showNameDialog = false
                        sessionName = ""
                    }
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Step 2b: pick a template
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
                                onClick = {
                                    onStartFromTemplate(template.id, template.name)
                                    showTemplatePicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(template.name, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showChoice = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Start Workout") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text("Recent Workouts", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (sessions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No workouts yet. Hit Start!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(sessions) { session ->
                    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                    val date = remember(session.date, locale) {
                        SimpleDateFormat("EEE, MMM d · h:mm a", locale).format(Date(session.date))
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenSession(session.id) }
                    ) {
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
}