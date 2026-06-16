package org.veilon.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.veilon.gymtracker.ui.TemplatesViewModel
import org.veilon.gymtracker.ui.theme.ScreenTitle

@Composable
fun TemplatesScreen(
    onOpenTemplate: (Long) -> Unit,
    viewModel: TemplatesViewModel = viewModel()
) {
    val templates by viewModel.templates.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var templateToDelete by remember { mutableStateOf<org.veilon.gymtracker.data.WorkoutTemplate?>(null) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Template") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template name (e.g. Push Day)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (templateName.isNotBlank()) {
                        viewModel.createTemplate(templateName.trim())
                        showDialog = false
                        templateName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Delete template?") },
            text = { Text("\"${template.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTemplate(template)
                    templateToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScreenTitle("Templates")
                    Button(
                        onClick = { showDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("NEW", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (templates.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No templates yet. Create one!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(templates) { template ->
                    Card(
                        onClick = { onOpenTemplate(template.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { templateToDelete = template }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}