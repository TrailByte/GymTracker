package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.veilon.gymtracker.data.Exercise
import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.ui.WorkoutViewModel

@Composable
fun WorkoutScreen(
    sessionId: Long,
    sessionName: String,
    viewModel: WorkoutViewModel = viewModel()
) {
    val logs by viewModel.getLogsForSession(sessionId).collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val restTimer by viewModel.restTimerSeconds.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    var showExercisePicker by remember { mutableStateOf(false) }

    // Elapsed timer tick
    LaunchedEffect(Unit) {
        while (true) { delay(1000); viewModel.tickElapsed() }
    }

    // Rest timer tick
    LaunchedEffect(restTimer) {
        if (restTimer != null) {
            delay(1000)
            viewModel.tickRestTimer()
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onDismiss = { showExercisePicker = false },
            onPick = { exercise ->
                val nextSet = logs.count { it.exerciseId == exercise.id } + 1
                viewModel.logSet(sessionId, exercise, nextSet, 0, 0.0)
                showExercisePicker = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showExercisePicker = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Set") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(sessionName, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        Text(formatElapsed(elapsed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Rest timer card
            restTimer?.let { secs ->
                item {
                    RestTimerCard(
                        seconds = secs,
                        onAddTime = { viewModel.addRestTime(15) },
                        onSkip = { viewModel.skipRest() }
                    )
                }
            }

            // Group logs by exercise
            val grouped = logs.groupBy { it.exerciseId }
            grouped.forEach { (exerciseId, sets) ->
                val exercise = exercises.find { it.id == exerciseId }
                item {
                    ExerciseSetCard(
                        exerciseName = exercise?.name ?: "Unknown",
                        muscleGroup = exercise?.muscleGroup ?: "",
                        sets = sets
                    )
                }
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
            Text(
                String.format("%d:%02d", mins, secs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddTime) { Text("+15s") }
                Button(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
fun ExerciseSetCard(exerciseName: String, muscleGroup: String, sets: List<ExerciseLog>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exerciseName, fontWeight = FontWeight.SemiBold)
                Text(muscleGroup, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                Text("Set", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Weight", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            sets.forEach { log ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${log.setNumber}", Modifier.weight(1f))
                    Text("${log.reps}", Modifier.weight(1f))
                    Text("${log.weight} kg", Modifier.weight(1f))
                }
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
    val grouped = exercises.groupBy { it.muscleGroup }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Exercise") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                grouped.forEach { (group, exList) ->
                    item {
                        Text(group, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(exList) { ex ->
                        TextButton(onClick = { onPick(ex) }, modifier = Modifier.fillMaxWidth()) {
                            Text(ex.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}