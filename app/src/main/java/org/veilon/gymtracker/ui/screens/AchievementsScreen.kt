package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.gamification.AchievementType
import org.veilon.gymtracker.ui.AchievementRow
import org.veilon.gymtracker.ui.AchievementsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BADGES_PER_ROW = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = viewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val unlockedCount = rows.count { it.unlocked }
    var detailRow by remember { mutableStateOf<AchievementRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ACHIEVEMENTS") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "$unlockedCount / ${rows.size} unlocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val grouped = rows.groupBy { it.def.type }
            AchievementType.values().forEach { type ->
                val group = grouped[type].orEmpty()
                if (group.isNotEmpty()) {
                    item {
                        Text(
                            sectionTitle(type),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Chunked rows instead of a nested grid — avoids fighting
                    // the outer LazyColumn's own scrolling.
                    group.chunked(BADGES_PER_ROW).forEach { chunk ->
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { row ->
                                    AchievementBadge(
                                        row = row,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { detailRow = row }
                                    )
                                }
                                // Pad an incomplete last row so badges don't stretch wide
                                repeat(BADGES_PER_ROW - chunk.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailRow?.let { row -> AchievementDetailDialog(row, onDismiss = { detailRow = null }) }
}

private fun sectionTitle(type: AchievementType): String = when (type) {
    AchievementType.WORKOUT_COUNT -> "WORKOUTS"
    AchievementType.STREAK_WEEKS -> "STREAKS"
    AchievementType.PR_COUNT -> "PERSONAL RECORDS"
    AchievementType.LIFETIME_VOLUME_KG -> "LIFETIME VOLUME"
}

@Composable
private fun AchievementBadge(row: AchievementRow, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (row.unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (row.unlocked) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = row.def.name,
                tint = if (row.unlocked) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            row.def.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (row.unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementDetailDialog(row: AchievementRow, onDismiss: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.def.name) },
        text = {
            val status = if (row.unlocked && row.unlockedDate != null) {
                "Unlocked ${dateFmt.format(Date(row.unlockedDate))}"
            } else {
                "Not yet unlocked"
            }
            Text("${row.def.description}\n\n$status")
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
