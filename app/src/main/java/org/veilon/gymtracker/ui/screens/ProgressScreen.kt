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
fun ProgressScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ScreenTitle("Progress")
            Spacer(Modifier.height(8.dp))
            Text("Charts coming soon",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}