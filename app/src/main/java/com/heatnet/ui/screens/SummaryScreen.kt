package com.heatnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heatnet.data.analysis.WeakSpotDetector
import com.heatnet.data.analysis.WeakSpotReport
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.HeatNetTopBar
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.theme.SecondaryButton
import com.heatnet.ui.theme.SurfaceCard

/** Shown after Finish (PRD 5 step 7). Readings are already saved; the text comes from Member 3's F8 detector. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(container: AppContainer, sessionId: Long, onBackToMap: () -> Unit, onDone: () -> Unit) {
    val state by produceState<Pair<Int, WeakSpotReport>?>(null, sessionId) {
        value = container.repository.load(sessionId)?.let { loaded ->
            loaded.readings.size to WeakSpotDetector.analyse(loaded.session, loaded.readings)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { HeatNetTopBar(title = { Text("Session saved") }) }) { padding ->
        val loaded = state
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val (count, report) = loaded
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("$count readings saved on this phone.", style = MaterialTheme.typography.bodyLarge)
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weak spots", style = MaterialTheme.typography.titleMedium)
                    if (report.spots.isEmpty()) {
                        Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        report.spots.forEach { Text("• ${it.sentence}", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Back to map", onBackToMap, Modifier.weight(1f))
                PrimaryButton("Done", onDone, Modifier.weight(1f))
            }
        }
    }
}
