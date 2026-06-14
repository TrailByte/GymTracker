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
    onStartWorkout: (String) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sessions by viewModel.recentSessions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                        onStartWorkout(sessionName.trim())
                        showDialog = false
                        sessionName = ""
                    }
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
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
                Text(
                    "Recent Workouts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
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