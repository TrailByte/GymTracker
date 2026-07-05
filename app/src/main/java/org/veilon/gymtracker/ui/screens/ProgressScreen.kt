package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
fun ProgressScreen(viewModel: StatsViewModel = viewModel()) {
    val prs by viewModel.prs.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    val weeklyVolume by viewModel.weeklyVolume.collectAsState()
    val weeklyFrequency by viewModel.weeklyFrequency.collectAsState()

    val grouped = prs.groupBy { it.muscleGroup }
    val hasAnyData = prs.isNotEmpty() || weeklyVolume.any { it.value > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { ScreenTitle("Progress") }

        if (!hasAnyData) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No data yet — complete some sets to see your progress here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Weekly volume trend ---
        if (weeklyVolume.any { it.value > 0 }) {
            item {
                val unit = if (useLbs) "lbs" else "kg"
                Text(
                    "WEEKLY VOLUME ($unit)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                val displayPoints = weeklyVolume.map { p ->
                    val v = if (useLbs) p.value * 2.20462 else p.value
                    p.copy(value = v)
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

        // --- Workout frequency ---
        if (weeklyFrequency.any { it.value > 0 }) {
            item {
                Text(
                    "WORKOUTS PER WEEK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    SimpleBarChart(
                        data = weeklyFrequency,
                        modifier = Modifier.padding(16.dp),
                        barColor = MaterialTheme.colorScheme.secondary,
                        valueFormatter = { v -> v.roundToInt().toString() }
                    )
                }
            }
        }

        // --- Personal records ---
        if (prs.isNotEmpty()) {
            item {
                Text(
                    "PERSONAL RECORDS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            grouped.toSortedMap().forEach { (group, list) ->
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        group.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(list.sortedByDescending { it.maxWeightKg }, key = { it.exerciseId }) { pr ->
                    StatsRecordCard(pr, useLbs)
                }
            }
        }
    }
}

/**
 * A simple bar chart built from plain layout (no Canvas, no charting library):
 * each bar is a Box sized by fillMaxHeight(fraction-of-max), so it's just
 * ordinary Compose measurement doing the work.
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
private fun StatsRecordCard(pr: ExercisePR, useLbs: Boolean) {
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val plateValue = if (useLbs) (pr.maxWeightKg * 2.20462).roundToInt().toString()
                             else pr.maxWeightKg.roundToInt().toString()
            PlateBadge(value = plateValue)
            Column(Modifier.weight(1f)) {
                Text(pr.exerciseName, fontWeight = FontWeight.SemiBold)
                Text(
                    "Best set: ${formatWeight(pr.maxWeightKg, useLbs)} · ${dateFmt.format(Date(pr.maxWeightDate))}",
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
