package com.heatnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.heatnet.data.analysis.WeakSpotDetector
import com.heatnet.data.analysis.WeakSpotReport
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.AuroraBackground
import com.heatnet.ui.theme.Brand
import com.heatnet.ui.theme.HeatNetTopBar
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.theme.SecondaryButton
import com.heatnet.ui.theme.StatusPill
import com.heatnet.ui.theme.SurfaceCard

/** Shown after Finish (PRD 5 step 7). Readings are already saved; the text comes from Member 3's F8 detector. */
@Composable
fun SummaryScreen(container: AppContainer, sessionId: Long, onBackToMap: () -> Unit, onDone: () -> Unit) {
    val state by produceState<Pair<Int, WeakSpotReport>?>(null, sessionId) {
        value = container.repository.load(sessionId)?.let { loaded ->
            loaded.readings.size to WeakSpotDetector.analyse(loaded.session, loaded.readings)
        }
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { HeatNetTopBar(title = { Text("Session saved") }) },
        ) { padding ->
            val loaded = state
            if (loaded == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            val (count, report) = loaded
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatusPill("Saved on this phone", pulsing = false)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.displayMedium.copy(brush = Brand.gradient),
                    )
                    Text(
                        if (count == 1) "reading" else "readings",
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text("Weak spots", style = MaterialTheme.typography.titleLarge)
                if (report.spots.isEmpty()) {
                    SurfaceCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(report.summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    report.spots.forEach { spot ->
                        SurfaceCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp),
                                )
                                Text(spot.sentence, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Back to map", onBackToMap, Modifier.weight(1f))
                    PrimaryButton("Done", onDone, Modifier.weight(1f))
                }
            }
        }
    }
}
