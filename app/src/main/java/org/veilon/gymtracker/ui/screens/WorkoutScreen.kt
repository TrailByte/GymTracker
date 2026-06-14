package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.ui.WorkoutViewModel
import org.veilon.gymtracker.ui.displayWeight
import org.veilon.gymtracker.ui.toKg

@Composable
fun WorkoutScreen(
    sessionId: Long,
    sessionName: String,
    onFinish: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    LaunchedEffect(sessionId) { viewModel.setSession(sessionId) }

    val logs by viewModel.logs.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val restTimer by viewModel.restTimerSeconds.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    val restDuration by viewModel.restDuration.collectAsState()
    val restForExerciseId by viewModel.restForExerciseId.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    // A ticking "now" that updates every second while this screen is visible.
    // Elapsed is computed as now - startTime, so leaving/returning stays accurate.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val elapsed = startTime?.let { ((now - it) / 1000).toInt().coerceAtLeast(0) } ?: 0

    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRestConfig by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(restTimer) {
        if (restTimer != null) { delay(1000); viewModel.tickRestTimer() }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onDismiss = { showExercisePicker = false },
            onPick = { exercise ->
                // If already in the workout, this adds another set to the existing card;
                // addSet numbers it correctly. Either way: one card per exercise.
                viewModel.addSet(sessionId, exercise)
                showExercisePicker = false
            }
        )
    }

    if (showFinishDialog) {
        val hasIncomplete = logs.any { !it.completed }
        if (hasIncomplete) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Unmarked sets") },
                text = { Text("Some sets aren't marked complete. What would you like to do?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.finishWorkout(sessionId, discardIncomplete = false) {
                            showFinishDialog = false; onFinish()
                        }
                    }) { Text("Mark all complete") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.finishWorkout(sessionId, discardIncomplete = true) {
                            showFinishDialog = false; onFinish()
                        }
                    }) { Text("Discard unmarked") }
                }
            )
        } else {
            LaunchedEffect(Unit) {
                viewModel.finishWorkout(sessionId, discardIncomplete = false) {
                    showFinishDialog = false; onFinish()
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel workout?") },
            text = { Text("This will discard the entire workout and everything logged in it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelWorkout(sessionId) {
                        showCancelDialog = false; onFinish()
                    }
                }) { Text("Discard workout") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep going") }
            }
        )
    }

    if (showRestConfig) {
        AlertDialog(
            onDismissRequest = { showRestConfig = false },
            title = { Text("Rest duration") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { viewModel.setRestDuration(restDuration - 15) }) {
                        Text("-15s")
                    }
                    Text("${restDuration / 60}:${String.format("%02d", restDuration % 60)}",
                        style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { viewModel.setRestDuration(restDuration + 15) }) {
                        Text("+15s")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRestConfig = false }) { Text("Done") } }
        )
    }

    val unitLabel = if (useLbs) "lbs" else "kg"

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column {
                    Text(sessionName, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text(formatElapsed(elapsed), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = { showRestConfig = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Rest: ${restDuration / 60}:${String.format("%02d", restDuration % 60)} (tap to change)")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            val grouped = logs.groupBy { it.exerciseId }
            val orderedIds = logs.map { it.exerciseId }.distinct()
            items(orderedIds, key = { it }) { exerciseId ->
                val exercise = exercises.find { it.id == exerciseId }
                val sets = grouped[exerciseId] ?: emptyList()
                if (exercise != null) {
                    ExerciseSetCard(
                        exercise = exercise,
                        sets = sets,
                        unitLabel = unitLabel,
                        useLbs = useLbs,
                        onAddSet = { viewModel.addSet(sessionId, exercise) },
                        onUpdate = { log, reps, weight -> viewModel.updateSet(log, reps, weight) },
                        onToggle = { log -> viewModel.toggleComplete(log) },
                        onDeleteSet = { log -> viewModel.deleteSet(log) },
                        onDeleteExercise = { viewModel.deleteExercise(sessionId, exerciseId) }
                    )
                    // Rest timer appears under the exercise whose set was just completed
                    if (restTimer != null && restForExerciseId == exerciseId) {
                        Spacer(Modifier.height(8.dp))
                        RestTimerCard(
                            seconds = restTimer!!,
                            onAddTime = { viewModel.addRestTime(15) },
                            onSkip = { viewModel.skipRest() }
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Exercise")
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = logs.isNotEmpty()
                    ) { Text("Finish") }
                }
            }
        }
    }
}

@Composable
fun ExerciseSetCard(
    exercise: Exercise,
    sets: List<ExerciseLog>,
    unitLabel: String,
    useLbs: Boolean,
    onAddSet: () -> Unit,
    onUpdate: (ExerciseLog, Int, Double) -> Unit,
    onToggle: (ExerciseLog) -> Unit,
    onDeleteSet: (ExerciseLog) -> Unit,
    onDeleteExercise: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(exercise.name, fontWeight = FontWeight.SemiBold)
                    Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDeleteExercise) { Text("Remove") }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Set", Modifier.weight(0.6f), style = MaterialTheme.typography.labelMedium)
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(unitLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(0.6f))
            }
            sets.forEach { log ->
                SetRow(log, useLbs, onUpdate, onToggle, onDeleteSet)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetRow(
    log: ExerciseLog,
    useLbs: Boolean,
    onUpdate: (ExerciseLog, Int, Double) -> Unit,
    onToggle: (ExerciseLog) -> Unit,
    onDeleteSet: (ExerciseLog) -> Unit
) {
    var repsText by remember(log.id) { mutableStateOf(if (log.reps > 0) log.reps.toString() else "") }
    var weightText by remember(log.id) {
        mutableStateOf(if (log.weight > 0) displayWeight(log.weight, useLbs) else "")
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${log.setNumber}", Modifier.weight(0.6f))
        OutlinedTextField(
            value = repsText,
            onValueChange = {
                repsText = it.filter { c -> c.isDigit() }
                val reps = repsText.toIntOrNull() ?: 0
                val kg = toKg(weightText.toDoubleOrNull() ?: 0.0, useLbs)
                onUpdate(log, reps, kg)
            },
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = {
                weightText = it.filter { c -> c.isDigit() || c == '.' }
                val reps = repsText.toIntOrNull() ?: 0
                val kg = toKg(weightText.toDoubleOrNull() ?: 0.0, useLbs)
                onUpdate(log, reps, kg)
            },
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        Box(Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onToggle(log) }) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Mark complete",
                    tint = if (log.completed) Color(0xFF7FA563)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RestTimerCard(seconds: Int, onAddTime: () -> Unit, onSkip: () -> Unit) {
    val mins = seconds / 60
    val secs = seconds % 60
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Rest", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(String.format("%d:%02d", mins, secs),
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddTime) { Text("+15s") }
                Button(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises
        else exercises.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.muscleGroup.contains(query, ignoreCase = true)
        }
    }
    val grouped = filtered.groupBy { it.muscleGroup }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    grouped.forEach { (group, exList) ->
                        item {
                            Text(group, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp))
                        }
                        items(exList) { ex ->
                            TextButton(onClick = { onPick(ex) },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(ex.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}