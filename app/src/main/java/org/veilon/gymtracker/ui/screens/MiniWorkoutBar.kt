package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MiniWorkoutBar(
    sessionName: String,
    startTimeMillis: Long,
    restEndsAtMillis: Long?,
    onExpand: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val elapsed = ((now - startTimeMillis) / 1000).coerceAtLeast(0)
    val h = elapsed / 3600
    val m = (elapsed % 3600) / 60
    val s = elapsed % 60
    val timeText = if (h > 0)
        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    else
        String.format(java.util.Locale.US, "%d:%02d", m, s)

    // Remaining rest, computed from the shared end timestamp
    val restRemaining: Int? = restEndsAtMillis?.let {
        val rem = ((it - now) / 1000).toInt()
        if (rem > 0) rem else null
    }

    Surface(
        onClick = onExpand,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("IN PROGRESS", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(sessionName, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Show rest countdown when resting, otherwise the elapsed time
                if (restRemaining != null) {
                    Text(
                        "Rest ${restRemaining / 60}:${String.format(java.util.Locale.US, "%02d", restRemaining % 60)}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(timeText, fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
