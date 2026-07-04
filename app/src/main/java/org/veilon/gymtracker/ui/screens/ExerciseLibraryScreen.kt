package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.ui.ExerciseLibraryViewModel
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var showAddEdit by remember { mutableStateOf(false) }
    var blockedExercise by remember { mutableStateOf<Exercise?>(null) }

    if (showAddEdit) {
        ExerciseEditDialog(
            existing = editing,
            muscleGroups = viewModel.muscleGroups,
            onDismiss = { showAddEdit = false; editing = null },
            onConfirm = { name, group ->
                val current = editing
                if (current == null) viewModel.addExercise(name, group)
                else viewModel.updateExercise(current, name, group)
                showAddEdit = false; editing = null
            }
        )
    }

    blockedExercise?.let { ex ->
        AlertDialog(
            onDismissRequest = { blockedExercise = null },
            title = { Text("Can't delete") },
            text = { Text("\"${ex.name}\" has workout history, so it can't be deleted. Archive it instead? It'll be hidden from the library, picker, and templates, but your past workouts stay intact.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveExercise(ex)
                    blockedExercise = null
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { blockedExercise = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showAddEdit = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Exercise") }
            )
        }
    ) { padding ->
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        val active = exercises.filter { !it.archived }
        val archived = exercises.filter { it.archived }

        val byFilter = if (selectedFilter == "All") active
        else active.filter { it.muscleGroup == selectedFilter }
        val filtered = if (searchQuery.isBlank()) byFilter
        else byFilter.filter { it.name.contains(searchQuery, ignoreCase = true) }
        val grouped = filtered.groupBy { it.muscleGroup }

        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search — fixed, not part of the scrolling list
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Muscle-group filter chips — fixed, horizontally scrollable
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All") }
                    )
                }
                items(viewModel.muscleGroups) { mg ->
                    FilterChip(
                        selected = selectedFilter == mg,
                        onClick = { selectedFilter = mg },
                        label = { Text(mg) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (grouped.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No exercises match.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                grouped.forEach { (group, list) ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                group.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(list, key = { it.id }) { ex ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ex.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { editing = ex; showAddEdit = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                TextButton(onClick = {
                                    viewModel.removeExercise(
                                        ex,
                                        onBlocked = { blockedExercise = ex },
                                        onDeleted = { }
                                    )
                                }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }

                if (archived.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "ARCHIVED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Hidden from pickers, history preserved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(archived, key = { it.id }) { ex ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        ex.muscleGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { viewModel.restoreExercise(ex) }) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditDialog(
    existing: Exercise?,
    muscleGroups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, group: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var group by remember { mutableStateOf(existing?.muscleGroup ?: muscleGroups.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Exercise" else "Edit Exercise") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                Text("Muscle group", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    muscleGroups.chunked(3).forEach { rowGroups ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowGroups.forEach { mg ->
                                FilterChip(
                                    selected = group == mg,
                                    onClick = { group = mg },
                                    label = { Text(mg) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), group) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}