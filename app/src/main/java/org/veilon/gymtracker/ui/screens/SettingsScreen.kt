package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val useLbs by viewModel.useLbs.collectAsState()
    val restSeconds by viewModel.restSeconds.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)

        // Weight unit
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weight unit", fontWeight = FontWeight.SemiBold)
                    Text(if (useLbs) "Pounds (lbs)" else "Kilograms (kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // false = kg (left), true = lbs (right)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("kg")
                    Switch(checked = useLbs, onCheckedChange = { viewModel.setUseLbs(it) })
                    Text("lbs")
                }
            }
        }

        // Default rest duration
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Default rest timer", fontWeight = FontWeight.SemiBold)
                Text("New workouts start with this rest duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { viewModel.setRestSeconds(restSeconds - 15) }) {
                        Text("-15s")
                    }
                    Text(
                        "${restSeconds / 60}:${String.format("%02d", restSeconds % 60)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedButton(onClick = { viewModel.setRestSeconds(restSeconds + 15) }) {
                        Text("+15s")
                    }
                }
            }
        }
    }
}