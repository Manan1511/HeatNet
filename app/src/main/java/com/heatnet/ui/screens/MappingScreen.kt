package com.heatnet.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heatnet.data.model.Metric
import com.heatnet.data.model.Reading
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.HeatNetTopBar
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.heatmap.HeatmapConfig
import com.heatnet.ui.heatmap.HeatmapLegend
import com.heatnet.ui.heatmap.HeatmapView
import com.heatnet.ui.heatmap.MetricScale
import com.heatnet.ui.heatmap.displayLabel
import com.heatnet.ui.heatmap.formatValue
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingScreen(container: AppContainer, sessionId: Long, onBack: () -> Unit, onFinish: () -> Unit) {
    val vm = viewModel(key = "map-$sessionId") {
        MappingViewModel(sessionId, container.repository, container.connectionMonitor.state) { container.measurementSource }
    }
    val session = vm.session
    val readings by vm.readings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HeatNetTopBar(
                title = {
                    Column {
                        Text(session?.name ?: "Map", style = MaterialTheme.typography.titleMedium)
                        session?.let {
                            val demoLabel = when {
                                readings.isEmpty() -> ""
                                readings.all { reading -> reading.isDemo } -> " · demo"
                                readings.any { reading -> reading.isDemo } -> " · includes demo"
                                else -> ""
                            }
                            Text(
                                "${it.connectionType.label} · ${readings.size} readings$demoLabel",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = onFinish, enabled = session != null && !vm.measuring) { Text("Finish") }
                },
            )
        },
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (vm.notFound) Text("This session no longer exists.") else CircularProgressIndicator()
            }
            return@Scaffold
        }

        val metrics = Metric.availableFor(session.connectionType)
        val metric = vm.metric.takeIf { it in metrics } ?: metrics.first()
        val scale = MetricScale.forReadings(metric, session.connectionType, readings)
        val config = HeatmapConfig()
        val withValue = readings.count { metric.valueOf(it) != null }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metrics.forEach { m ->
                    FilterChip(selected = m == metric, onClick = { vm.selectMetric(m) }, label = { Text(m.label) })
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                HeatmapView(
                    outline = session.outline,
                    widthMetres = session.widthMetres,
                    heightMetres = session.heightMetres,
                    readings = readings,
                    metric = metric,
                    scale = scale,
                    config = config,
                    selectedPosition = vm.position,
                    highlightedReadingId = vm.selectedReadingId,
                    onTap = vm::onMapTap,
                    onReadingTap = { if (!vm.measuring) vm.selectedReadingId = it.id },
                    modifier = Modifier.fillMaxSize(),
                )
                if (withValue < config.minReadings) {
                    AssistChip(
                        onClick = {},
                        label = {
                            val more = config.minReadings - withValue
                            Text("Take $more more reading${if (more == 1) "" else "s"} to see the heatmap")
                        },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                val stage = vm.stage
                if (stage != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.padding(4.dp))
                        Text("${stage.label}…", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = vm::cancelMeasure) { Text("Cancel") }
                    }
                } else {
                    Text(
                        if (vm.position == null) {
                            "Walk to a spot, then tap where you are standing on the map."
                        } else {
                            "Stay where you are and press Measure. It takes about 10 seconds."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton("Measure", vm::measure, enabled = vm.position != null)
                }
                HeatmapLegend(scale, metric.displayLabel(readings))
            }
        }

        readings.firstOrNull { it.id == vm.selectedReadingId }?.let { reading ->
            ReadingDetailSheet(
                reading = reading,
                isWifi = session.connectionType == ConnectionType.WIFI,
                onDismiss = { vm.selectedReadingId = null },
                onDelete = { vm.deleteReading(reading.id) },
            )
        }

        vm.switchedTo?.let { now ->
            AlertDialog(
                onDismissRequest = vm::dismissSwitchWarning,
                title = { Text("Connection changed") },
                text = {
                    Text(
                        "You've switched to ${now.label.lowercase()}. Readings from here won't match earlier ones. " +
                            "Switch back to ${session.connectionType.label.lowercase()} to keep mapping, or end the session.",
                    )
                },
                confirmButton = { TextButton(onClick = vm::dismissSwitchWarning) { Text("I'll switch back") } },
                dismissButton = {
                    TextButton(onClick = {
                        vm.dismissSwitchWarning()
                        onFinish()
                    }) { Text("End session") }
                },
            )
        }
    }
}

/** Raw numbers for one dot (PRD section 3: numbers only in a detail view), plus delete/redo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingDetailSheet(reading: Reading, isWifi: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Reading", style = MaterialTheme.typography.titleLarge)
            Text(
                when (reading.status) {
                    MeasurementStatus.COMPLETE -> "All tests finished"
                    MeasurementStatus.PARTIAL -> "Some tests failed; missing values are shown as —"
                    MeasurementStatus.BLOCKED -> "Test failed (no connection or it timed out)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            DetailRow("Download", reading.downloadMbps, "Mbps")
            DetailRow("Upload", reading.uploadMbps, "Mbps")
            DetailRow("Latency", reading.latencyMs, "ms")
            DetailRow("Jitter", reading.jitterMs, "ms")
            DetailRow("Packet loss", reading.packetLossPct, "%")
            DetailRow("Packet loss method", reading.packetLossMethod.label)
            DetailRow("Signal strength", reading.signalDbm?.toFloat(), "dBm")
            if (isWifi) {
                DetailRow("Link speed", reading.linkSpeedMbps?.toFloat(), "Mbps")
                DetailRow("Band", reading.wifiBand?.let { "$it GHz" })
                DetailRow("Channel", reading.wifiChannel?.toString())
                DetailRow("Access point (BSSID)", reading.bssid)
            } else {
                DetailRow("Network type", reading.networkType)
            }
            HorizontalDivider()
            DetailRow("Position", "${formatValue(reading.x)} m east, ${formatValue(reading.y)} m south of the top-left")
            DetailRow("Time", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(reading.timestamp)))
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete this reading")
            }
            Text(
                "Placed it in the wrong spot? Delete it, tap the right spot and measure again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reading?") },
            text = { Text("This dot and its numbers will be removed from the map.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: Float?, unit: String) =
    DetailRow(label, value?.let { "${formatValue(it)} $unit" })

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}
