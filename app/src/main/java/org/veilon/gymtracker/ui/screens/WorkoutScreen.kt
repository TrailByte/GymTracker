package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import org.veilon.gymtracker.ui.theme.ScreenTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable

@Composable
fun WorkoutScreen(
    sessionId: Long,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    LaunchedEffect(sessionId) { viewModel.setSession(sessionId) }

    val logs by viewModel.logs.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val restEndsAt by viewModel.restEndsAt.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    val restDuration by viewModel.restDuration.collectAsState()
    val restForExerciseId by viewModel.restForExerciseId.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val sessionName by viewModel.sessionName.collectAsState()
    // A ticking "now" that updates every second while this screen is visible.
    // Both elapsed and rest-remaining are computed from it, so they stay accurate
    // across navigation/minimize.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val elapsed = startTime?.let { ((now - it) / 1000).toInt().coerceAtLeast(0) } ?: 0
    // Remaining rest seconds, computed from the stored end timestamp (null = no rest)
    val restRemaining: Int? = restEndsAt?.let {
        val rem = ((it - now) / 1000).toInt()
        if (rem > 0) rem else null
    }

    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRestConfig by remember { mutableStateOf(false) }
    var exerciseToRemove by remember { mutableStateOf<Exercise?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    // When rest naturally elapses, clear the "which exercise" marker
    LaunchedEffect(restRemaining) {
        if (restRemaining == null && restEndsAt != null) {
            viewModel.skipRest()
        }
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
        val minuteValues = remember { (0..10).toList() }                    // 0..10
        val secondValues = remember { (0..50 step 10).toList() }            // 0,10,20,30,40,50

        // Seed indices from current restDuration
        val curMin = restDuration / 60
        val curSecStep = ((restDuration % 60) / 10)  // 0..5
        var minIndex by remember(showRestConfig) {
            mutableStateOf(minuteValues.indexOf(curMin.coerceIn(0, 10)).coerceAtLeast(0))
        }
        var secIndex by remember(showRestConfig) {
            mutableStateOf(secStepToIndex(curSecStep, secondValues))
        }

        fun pushDuration() {
            val total = minuteValues[minIndex] * 60 + secondValues[secIndex]
            viewModel.setRestDuration(total)
        }

        AlertDialog(
            onDismissRequest = { showRestConfig = false },
            title = { Text("Rest duration") },
            text = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = minuteValues.map { it.toString() },
                        selectedIndex = minIndex,
                        onSelected = { minIndex = it; pushDuration() },
                        modifier = Modifier.weight(1f)
                    )
                    Text("min", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    WheelPicker(
                        items = secondValues.map { String.format(java.util.Locale.US, "%02d", it) },
                        selectedIndex = secIndex,
                        onSelected = { secIndex = it; pushDuration() },
                        modifier = Modifier.weight(1f)
                    )
                    Text("sec", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showRestConfig = false }) { Text("Done") } }
        )
    }

    exerciseToRemove?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToRemove = null },
            title = { Text("Remove exercise?") },
            text = { Text("Remove ${ex.name} and all its sets from this workout?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(sessionId, ex.id)
                    exerciseToRemove = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRemove = null }) { Text("Cancel") }
            }
        )
    }

    val unitLabel = if (useLbs) "lbs" else "kg"

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Column {
                            Text(sessionName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text(formatElapsed(elapsed),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onMinimize) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize"
                            )
                        }
                    },
                    actions = {
                        // Rest countdown lives in the top bar (not between exercises)
                        if (restRemaining != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Rest ${restRemaining / 60}:${String.format(java.util.Locale.US, "%02d", restRemaining % 60)}",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { viewModel.addRestTime(15) }) { Text("+15") }
                                TextButton(onClick = { viewModel.skipRest() }) { Text("Skip") }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
            ) {
            item {
                TextButton(
                    onClick = { showRestConfig = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Rest: ${restDuration / 60}:${String.format(java.util.Locale.US, "%02d", restDuration % 60)} (tap to change)")
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
                        onDeleteExercise = { exerciseToRemove = exercise }
                    )
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
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    var pendingDelete by remember(log.id) { mutableStateOf(false) }
    var visible by remember(log.id) { mutableStateOf(true) }

    val canComplete = (repsText.toIntOrNull() ?: 0) > 0 && weightText.isNotBlank()

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
    ) {
        if (pendingDelete) {
            // Confirm-delete state: red-tinted row with Delete / Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Delete set ${log.setNumber}?",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = { pendingDelete = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        visible = false          // trigger exit animation
                        onDeleteSet(log)         // remove from DB; row leaves the list
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (log.completed) Color(0xFF7FA563).copy(alpha = 0.15f)
                        else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { pendingDelete = true }
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
                Box(Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { onToggle(log) },
                        enabled = canComplete || log.completed
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Mark complete",
                            tint = if (log.completed) Color(0xFF7FA563)
                            else if (canComplete) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
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
            Text("Rest", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, style = MaterialTheme.typography.labelMedium,
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
    val grouped = filtered.toList().groupBy { it.muscleGroup }

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
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    grouped.forEach { (group, exList) ->
                        item {
                            // Distinct section band: filled, uppercase, full-width
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
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(exList) { ex ->
                            Surface(
                                onClick = { onPick(ex) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    ex.name,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                )
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

private fun secStepToIndex(step: Int, secondValues: List<Int>): Int {
    val target = step * 10
    val i = secondValues.indexOf(target)
    return if (i >= 0) i else 0
}