package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.ExercisePR
import org.veilon.gymtracker.ui.StatsViewModel
import org.veilon.gymtracker.ui.WeekPoint
import org.veilon.gymtracker.ui.formatWeight
import org.veilon.gymtracker.ui.theme.PlateBadge
import org.veilon.gymtracker.ui.theme.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    onOpenExercise: (Long) -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val prs by viewModel.prs.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    val weeklyVolume by viewModel.weeklyVolume.collectAsState()
    val weeklyFrequency by viewModel.weeklyFrequency.collectAsState()
    val muscleGroupVolume by viewModel.muscleGroupVolume.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var muscleFilter by remember { mutableStateOf("All") }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            ScreenTitle("Progress")
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Overview") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Records") })
        }

        when (tab) {
            0 -> OverviewTab(weeklyVolume, weeklyFrequency, muscleGroupVolume, useLbs)
            else -> RecordsTab(
                prs = prs,
                useLbs = useLbs,
                muscleFilter = muscleFilter,
                onFilterChange = { muscleFilter = it },
                onOpenExercise = onOpenExercise
            )
        }
    }
}

@Composable
private fun OverviewTab(
    weeklyVolume: List<WeekPoint>,
    weeklyFrequency: List<WeekPoint>,
    muscleGroupVolume: List<WeekPoint>,
    useLbs: Boolean
) {
    val hasData = weeklyVolume.any { it.value > 0 } || weeklyFrequency.any { it.value > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (!hasData) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No workouts yet — finish one to see your trends here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (muscleGroupVolume.isNotEmpty()) {
            item {
                val unit = if (useLbs) "lbs" else "kg"
                SectionLabel("MUSCLE GROUP VOLUME · LAST 30 DAYS ($unit)")
                Spacer(Modifier.height(8.dp))
                val displayPoints = muscleGroupVolume.map { p ->
                    p.copy(value = if (useLbs) p.value * 2.20462 else p.value)
                }
                Card(Modifier.fillMaxWidth()) {
                    SimpleBarChart(
                        data = displayPoints,
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.tertiary,
                        valueFormatter = { v -> if (v >= 1000) "${(v / 1000).roundToInt()}k" else v.roundToInt().toString() }
                    )
                }
            }
        }
        if (weeklyVolume.any { it.value > 0 }) {
            item {
                val unit = if (useLbs) "lbs" else "kg"
                SectionLabel("WEEKLY VOLUME ($unit)")
                Spacer(Modifier.height(8.dp))
                val displayPoints = weeklyVolume.map { p ->
                    p.copy(value = if (useLbs) p.value * 2.20462 else p.value)
                }
                Card(Modifier.fillMaxWidth()) {
                    SimpleBarChart(
                        data = displayPoints,
                        modifier = Modifier.padding(16.dp),
                        valueFormatter = { v -> if (v >= 1000) "${(v / 1000).roundToInt()}k" else v.roundToInt().toString() }
                    )
                }
            }
        }
        if (weeklyFrequency.any { it.value > 0 }) {
            item {
                SectionLabel("WORKOUTS PER WEEK")
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    SimpleBarChart(
                        data = weeklyFrequency,
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.secondary,
                        valueFormatter = { it.roundToInt().toString() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsTab(
    prs: List<ExercisePR>,
    useLbs: Boolean,
    muscleFilter: String,
    onFilterChange: (String) -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val muscleGroups = remember(prs) { prs.map { it.muscleGroup }.distinct().sorted() }
    val filtered = if (muscleFilter == "All") prs else prs.filter { it.muscleGroup == muscleFilter }
    val grouped = filtered.groupBy { it.muscleGroup }.toSortedMap()

    Column(Modifier.fillMaxSize()) {
        if (muscleGroups.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = muscleFilter == "All",
                        onClick = { onFilterChange("All") },
                        label = { Text("All") }
                    )
                }
                items(muscleGroups) { mg ->
                    FilterChip(
                        selected = muscleFilter == mg,
                        onClick = { onFilterChange(mg) },
                        label = { Text(mg) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (prs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No personal records yet — complete some sets to see them here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            grouped.forEach { (group, list) ->
                item {
                    Text(
                        group.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(list.sortedByDescending { it.maxWeightKg }, key = { it.exerciseId }) { pr ->
                    StatsRecordCard(pr, useLbs, onClick = { onOpenExercise(pr.exerciseId) })
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Simple bar chart from plain layout (no Canvas, no library): each bar is a
 * Box sized by fillMaxHeight(fraction-of-max).
 */
@Composable
private fun SimpleBarChart(
    data: List<WeekPoint>,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Double) -> String = { it.roundToInt().toString() }
) {
    val maxValue = (data.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        valueFormatter(point.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    val heightFraction = (point.value / maxValue).toFloat().coerceIn(0.02f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(heightFraction)
                            .background(barColor, shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEach { point ->
                Text(
                    point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatsRecordCard(pr: ExercisePR, useLbs: Boolean, onClick: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val plateValue = if (useLbs) (pr.maxWeightKg * 2.20462).roundToInt().toString()
                             else pr.maxWeightKg.roundToInt().toString()
            PlateBadge(value = plateValue)
            Column(Modifier.weight(1f)) {
                Text("${pr.exerciseName} (${pr.equipmentType})", fontWeight = FontWeight.SemiBold)
                Text(
                    "Best set: ${pr.maxWeightReps}×${formatWeight(pr.maxWeightKg, useLbs)} · ${dateFmt.format(Date(pr.maxWeightDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Best volume: ${pr.maxVolumeReps}×${formatWeight(pr.maxVolumeWeightKg, useLbs)} · ${dateFmt.format(Date(pr.maxVolumeDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
