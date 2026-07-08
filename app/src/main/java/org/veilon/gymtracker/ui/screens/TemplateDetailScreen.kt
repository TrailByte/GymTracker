package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.data.TemplateExercise
import org.veilon.gymtracker.ui.TemplatesViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailScreen(
    templateId: Long,
    onBack: () -> Unit,
    viewModel: TemplatesViewModel = viewModel()
) {
    LaunchedEffect(templateId) { viewModel.setCurrentTemplate(templateId) }
    val templateExercises by viewModel.currentTemplateExercises.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    // Local mutable copy for smooth dragging; synced from DB when it changes
    var orderedExercises by remember(templateExercises) { mutableStateOf(templateExercises) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedExercises = orderedExercises.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.saveReorder(orderedExercises)
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = allExercises,
            onDismiss = { showPicker = false },
            onPick = { exercise ->
                viewModel.addExerciseToTemplate(templateId, exercise.id, orderedExercises.size)
                showPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Exercise") }
            )
        }
    ) { padding ->
        if (orderedExercises.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No exercises yet. Add some!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
                val visibleExercises = orderedExercises.filter { te ->
                    allExercises.any { it.id == te.exerciseId }
                }
                items(visibleExercises, key = { it.id }) { te ->
                    ReorderableItem(reorderState, key = te.id) { isDragging ->
                        val exercise = allExercises.find { it.id == te.exerciseId }
                        val elevation = if (isDragging) 8.dp else 1.dp
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle()
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = "Drag to reorder")
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text(exercise?.name ?: "Unknown",
                                                fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${exercise?.muscleGroup ?: ""} · ${exercise?.equipmentType ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.removeExerciseFromTemplate(te) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    TargetStepper(
                                        label = "Sets",
                                        value = te.targetSets,
                                        onChange = { viewModel.updateTargets(te, it, te.targetReps) }
                                    )
                                    TargetStepper(
                                        label = "Reps",
                                        value = te.targetReps,
                                        onChange = { viewModel.updateTargets(te, te.targetSets, it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TargetStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = { onChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            Text("$value", Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium)
            OutlinedIconButton(onClick = { onChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
