package com.heatnet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.AuroraBackground
import com.heatnet.ui.theme.Brand
import com.heatnet.ui.theme.HeatHero
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.theme.SecondaryButton
import com.heatnet.ui.theme.StatusPill
import com.heatnet.ui.theme.SurfaceCard

@Composable
fun HomeScreen(container: AppContainer, onNewSession: () -> Unit, onSavedSessions: () -> Unit) {
    AuroraBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                StatusPill("Network heatmaps", pulsing = true)
                Text(
                    "HeatNet",
                    style = MaterialTheme.typography.displayMedium.copy(brush = Brand.gradient),
                )
                Text(
                    "See exactly where your Wi-Fi or mobile data is strong, and where it drops out.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HeatHero()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepTile("1", "Draw", "your room", Modifier.weight(1f))
                    StepTile("2", "Measure", "each spot", Modifier.weight(1f))
                    StepTile("3", "See", "the map", Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Start a new map", onNewSession)
                SecondaryButton("Saved sessions", onSavedSessions, Modifier.fillMaxWidth())
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Demo mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Use made-up readings instead of real network tests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = container.useFakeMeasurements,
                            onCheckedChange = { container.useFakeMeasurements = it },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StepTile(number: String, title: String, caption: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(number, style = MaterialTheme.typography.headlineSmall.copy(brush = Brand.gradient))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
