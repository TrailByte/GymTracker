package org.veilon.gymtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.veilon.gymtracker.gamification.Celebration

@Composable
fun CelebrationOverlay(celebration: Celebration, onDismiss: () -> Unit) {
    // Reset per-celebration so re-showing (e.g. the next queued one) re-plays
    // the entrance instead of appearing already-visible.
    var visible by remember(celebration) { mutableStateOf(false) }
    LaunchedEffect(celebration) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 250))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    when (celebration) {
                        is Celebration.WorkoutFinished -> {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "WORKOUT COMPLETE",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                celebration.sessionName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            celebration.durationSeconds?.let { secs ->
                                val h = secs / 3600
                                val m = (secs % 3600) / 60
                                val text = if (h > 0) "${h}h ${m}m" else "${m}m"
                                Text(
                                    text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is Celebration.LevelUp -> {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "LEVEL ${celebration.newLevel}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            celebration.newTheme?.let { theme ->
                                Text(
                                    "New theme unlocked: ${theme.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is Celebration.AchievementUnlocked -> {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "ACHIEVEMENT UNLOCKED",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                celebration.achievement.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                celebration.achievement.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
