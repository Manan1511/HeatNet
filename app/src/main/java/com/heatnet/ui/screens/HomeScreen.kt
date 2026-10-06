package com.heatnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heatnet.ui.AppContainer

@Composable
fun HomeScreen(container: AppContainer, onNewSession: () -> Unit, onSavedSessions: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("HeatNet", style = MaterialTheme.typography.displaySmall)
            Text(
                "Map how good your Wi-Fi or mobile data is in each part of a room.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) { Text("New session") }
            OutlinedButton(onClick = onSavedSessions, modifier = Modifier.fillMaxWidth()) { Text("Saved sessions") }
            Spacer(Modifier.height(8.dp))
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
    }
}
