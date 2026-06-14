package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.veilon.gymtracker.ui.theme.ScreenTitle

@Composable
fun LogScreen(
    activeSessionId: Long?,
    onClearActive: () -> Unit,
    onGoToHome: () -> Unit
) {
    if (activeSessionId == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No active workout", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Start a workout from the Home tab.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onGoToHome) { Text("Go to Home") }
            }
        }
    } else {
        WorkoutScreen(
            sessionId = activeSessionId,
            onFinish = onClearActive
        )
    }
}