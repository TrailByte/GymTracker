package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.ExerciseHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

private enum class TimeRange(val label: String, val days: Int?) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    YEAR("1Y", 365),
    ALL("All", null)
}

private fun filterByRange(points: List<Pair<Long, Double>>, range: TimeRange): List<Pair<Long, Double>> {
    val days = range.days ?: return points
    val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
    return points.filter { it.first >= cutoff }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    viewModel: ExerciseHistoryViewModel = viewModel()
) {
    LaunchedEffect(exerciseId) { viewModel.setExerciseId(exerciseId) }

    val name by viewModel.exerciseName.collectAsState()
    val history by viewModel.history.collectAsState()
    val useLbs by viewModel.useLbs.collectAsState()
    var selectedRange by remember { mutableStateOf(TimeRange.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name.uppercase()) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (history.weightPoints.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No completed sets yet for this exercise.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TimeRange.entries.toList()) { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = { selectedRange = range },
                                label = { Text(range.label) }
                            )
                        }
                    }
                }

                val unit = if (useLbs) "lbs" else "kg"
                item {
                    Text(
                        "WEIGHT OVER TIME ($unit)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    val points = filterByRange(history.weightPoints, selectedRange)
                        .map { (d, v) -> d to (if (useLbs) v * 2.20462 else v) }
                    Card(Modifier.fillMaxWidth()) {
                        SimpleLineChart(points = points, modifier = Modifier.padding(16.dp))
                    }
                }
                item {
                    Text(
                        "VOLUME OVER TIME ($unit)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    val points = filterByRange(history.volumePoints, selectedRange)
                        .map { (d, v) -> d to (if (useLbs) v * 2.20462 else v) }
                    Card(Modifier.fillMaxWidth()) {
                        SimpleLineChart(
                            points = points,
                            modifier = Modifier.padding(16.dp),
                            lineColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal DIY line chart — no library, no dependency, just Canvas. Y-axis
 * always starts at 0 (an honest sense of scale, not auto-zoomed to the data's
 * own min/max). Plots (timestamp, value) points on an even time axis and
 * connects them. Handles 0, 1, and many points without crashing.
 */
@Composable
private fun SimpleLineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Double) -> String = { it.roundToInt().toString() }
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("No data in this range", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val minY = 0.0
    val maxYraw = points.maxOf { it.second }
    val maxY = if (maxYraw == minY) minY + 1.0 else maxYraw
    val minX = points.first().first
    val maxXraw = points.last().first
    val maxX = if (maxXraw == minX) minX + 1L else maxXraw

    // Which point is highlighted — starts on the most recent one, moves on tap/drag
    var selectedIndex by remember(points) { mutableStateOf(points.size - 1) }
    var canvasWidthPx by remember { mutableStateOf(1f) }

    fun xFor(t: Long, w: Float) = ((t - minX).toFloat() / (maxX - minX).toFloat()) * w

    fun selectNearest(xPos: Float) {
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        points.forEachIndexed { i, (t, _) ->
            val dist = kotlin.math.abs(xFor(t, canvasWidthPx) - xPos)
            if (dist < bestDist) { bestDist = dist; bestIdx = i }
        }
        selectedIndex = bestIdx
    }

    val selected = points[selectedIndex.coerceIn(0, points.size - 1)]

    Column(modifier) {
        // The "details" for whichever point is selected — defaults to the latest
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dateFmt.format(Date(selected.first)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valueFormatter(selected.second),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = lineColor
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            // Y-axis labels in their own reserved column — never overlaps the line
            Column(
                modifier = Modifier.height(140.dp).width(36.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    valueFormatter(maxY),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    valueFormatter(minY),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            Canvas(
                Modifier
                    .weight(1f)
                    .height(140.dp)
                    .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                    .pointerInput(points) {
                        detectTapGestures { offset -> selectNearest(offset.x) }
                    }
                    .pointerInput(points) {
                        detectDragGestures { change, _ ->
                            selectNearest(change.position.x)
                            change.consume()
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val topPad = 8.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val drawableH = h - topPad - bottomPad

                fun xForDraw(t: Long) = xFor(t, w)
                fun yFor(v: Double) = topPad + drawableH - (((v - minY) / (maxY - minY)).toFloat() * drawableH)

                if (points.size == 1) {
                    val (_, v) = points[0]
                    drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(w / 2f, yFor(v)))
                } else {
                    val path = Path()
                    points.forEachIndexed { i, (t, v) ->
                        val x = xForDraw(t)
                        val y = yFor(v)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path,
                        color = lineColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Vertical guide line under the selected point
                    val selX = xForDraw(selected.first)
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, h),
                        strokeWidth = 2.dp.toPx()
                    )
                    points.forEachIndexed { i, (t, v) ->
                        val isSelected = i == selectedIndex
                        drawCircle(
                            color = lineColor,
                            radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                            center = Offset(xForDraw(t), yFor(v))
                        )
                        if (isSelected) {
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(xForDraw(t), yFor(v))
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                dateFmt.format(Date(minX)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                dateFmt.format(Date(maxX)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
