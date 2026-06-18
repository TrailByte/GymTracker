package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.LogViewModel
import org.veilon.gymtracker.ui.theme.ScreenTitle
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.combinedClickable

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LogScreen(
    onResumeActive: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: LogViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    var sessionToDelete by remember { mutableStateOf<org.veilon.gymtracker.data.WorkoutSession?>(null) }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("\"${session.name}\" and all its logged sets will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    sessionToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            ScreenTitle("Log")
            Spacer(Modifier.height(4.dp))
        }

        // Active workout resume card, pinned on top when one's running
        state.activeSession?.let { active ->
            item {
                Card(
                    onClick = { onResumeActive(active.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(Modifier.weight(1f)) {
                            Text("IN PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(active.name, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("Resume", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Text("HISTORY", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.history.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No past workouts yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.history, key = { it.id }) { session ->
                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                val date = remember(session.date, locale) {
                    SimpleDateFormat("EEE, MMM d · h:mm a", locale).format(Date(session.date))
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenSession(session.id) },
                            onLongClick = { sessionToDelete = session }
                        )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.name, fontWeight = FontWeight.SemiBold)
                        Text(date, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        val stats = state.statsBySession[session.id]
                        if (stats != null) {
                            Spacer(Modifier.height(4.dp))
                            // Exercise list, capped height with internal scroll
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 120.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                stats.exercises.forEach { line ->
                                    Text(
                                        "${line.setCount}× ${line.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            // Footer: duration · volume · PR pill (PR pill comes in #12)
                            val volText = org.veilon.gymtracker.ui.formatWeight(stats.totalVolumeKg, useLbs)
                            val footer = buildList {
                                session.durationSeconds?.let { add(formatDuration(it)) }
                                add("$volText vol")
                            }.joinToString("  ·  ")
                            Text(
                                footer,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}