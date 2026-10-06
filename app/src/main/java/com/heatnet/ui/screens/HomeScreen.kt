package com.heatnet.ui.screens

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
import androidx.compose.ui.unit.dp
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.HeatHero
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.theme.SecondaryButton
import com.heatnet.ui.theme.SurfaceCard

@Composable
fun HomeScreen(container: AppContainer, onNewSession: () -> Unit, onSavedSessions: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            HeatHero()
            Spacer(Modifier.height(8.dp))
            Text("HeatNet", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "See exactly where your Wi-Fi or mobile data is strong, and where it drops out.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Start a new map", onNewSession)
            SecondaryButton("Saved sessions", onSavedSessions, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
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
        }
    }
}
