package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.SessionDetailViewModel
import org.veilon.gymtracker.ui.formatWeight
import org.veilon.gymtracker.ui.theme.ScreenTitle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onRepeat: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: SessionDetailViewModel = viewModel()
) {
    LaunchedEffect(sessionId) { viewModel.setSession(sessionId) }

    val logs by viewModel.logs.collectAsState()
    val orderedIds by viewModel.orderedExerciseIds.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    val sessionName by viewModel.sessionName.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete workout?") },
            text = { Text("This workout and all its sets will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteSession { onBack() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sessionName.uppercase()) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit(sessionId)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                confirmDelete = true
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Button(
                    onClick = { onRepeat(sessionId) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("Repeat this workout") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
            }
            if (logs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Nothing was logged in this workout.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val grouped = logs.groupBy { it.exerciseId }
                items(orderedIds, key = { it }) { exerciseId ->
                    val exercise = exercises.find { it.id == exerciseId }
                    val sets = grouped[exerciseId] ?: emptyList()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exercise?.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                            Text(exercise?.muscleGroup ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            sets.forEach { log ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text("Set ${log.setNumber}", Modifier.weight(1f),
                                        fontFamily = FontFamily.Monospace)
                                    Text("${log.reps} reps", Modifier.weight(1f),
                                        fontFamily = FontFamily.Monospace)
                                    Text(formatWeight(log.weight, useLbs), Modifier.weight(1f),
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}