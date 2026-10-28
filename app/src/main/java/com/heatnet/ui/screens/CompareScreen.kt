package com.heatnet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.heatnet.data.analysis.Comparison
import com.heatnet.data.analysis.ComparisonResult
import com.heatnet.data.analysis.SessionComparison
import com.heatnet.data.analysis.Verdict
import com.heatnet.data.model.Metric
import com.heatnet.data.model.SessionWithReadings
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.AuroraBackground
import com.heatnet.ui.theme.Brand
import com.heatnet.ui.theme.HeatNetTopBar
import com.heatnet.ui.theme.StatusPill
import com.heatnet.ui.theme.SurfaceCard
import com.heatnet.ui.theme.statusGreen
import com.heatnet.ui.heatmap.HeatmapLegend
import com.heatnet.ui.heatmap.HeatmapView
import com.heatnet.ui.heatmap.MetricScale
import com.heatnet.ui.heatmap.displayLabel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private sealed interface CompareLoad {
    data object Loading : CompareLoad
    data object Missing : CompareLoad
    data class Done(val result: ComparisonResult) : CompareLoad
}

/**
 * F7: two saved sessions side by side. Shared metrics use one colour scale
 * built from both sessions' readings, so the same colour means the same value on each side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(container: AppContainer, beforeId: Long, afterId: Long, onBack: () -> Unit) {
    val load by produceState<CompareLoad>(CompareLoad.Loading, beforeId, afterId) {
        val (before, after) = coroutineScope {
            awaitAll(
                async { container.repository.load(beforeId) },
                async { container.repository.load(afterId) },
            )
        }
        value = if (before == null || after == null) {
            CompareLoad.Missing
        } else {
            CompareLoad.Done(SessionComparison.compare(before, after))
        }
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HeatNetTopBar(
                    title = { Text("Compare sessions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val state = load) {
                    CompareLoad.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    CompareLoad.Missing -> Message("One of these sessions no longer exists.")
                    is CompareLoad.Done -> when (val result = state.result) {
                        is ComparisonResult.Failure -> Message(result.error.message)
                        is ComparisonResult.Success -> CompareContent(result.comparison)
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CompareContent(comparison: Comparison) {
    val type = comparison.before.session.connectionType
    val metrics = comparison.metrics.map { it.metric }
    var metricName by rememberSaveable { mutableStateOf(metrics.first().name) }
    val metric = metrics.firstOrNull { it.name == metricName } ?: metrics.first()
    val allReadings = comparison.before.readings + comparison.after.readings
    val scale = MetricScale.forReadings(metric, type, allReadings)
    val summary = comparison.metrics.first { it.metric == metric }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (comparison.before.session.connectionType != comparison.after.session.connectionType) {
            Text("Wi-Fi and mobile signal strengths use different scales and are shown separately below.")
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { m ->
                FilterChip(
                    selected = m == metric,
                    onClick = { metricName = m.name },
                    label = { Text(m.label) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionMap("Before", comparison.before, metric, scale, Modifier.weight(1f))
            SessionMap("After", comparison.after, metric, scale, Modifier.weight(1f))
        }

        HeatmapLegend(scale, metric.displayLabel(allReadings))

        val verdictColor = when (summary.verdict) {
            Verdict.IMPROVED -> statusGreen()
            Verdict.WORSE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            StatusPill(summary.verdict.label, dotColor = verdictColor)
            Text(describe(summary), style = MaterialTheme.typography.bodyMedium)
        }

        comparison.warnings.forEach { warning ->
            SurfaceCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    "Note: $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        if (comparison.before.session.connectionType != comparison.after.session.connectionType) {
            Text("Signal strength by connection", style = MaterialTheme.typography.titleMedium)
            listOf(comparison.before, comparison.after).forEach { session ->
                val signalScale = MetricScale.forReadings(Metric.SIGNAL, session.session.connectionType, session.readings)
                SessionMap(session.session.connectionType.name, session, Metric.SIGNAL, signalScale, Modifier.fillMaxWidth())
                HeatmapLegend(signalScale, "Signal strength")
            }
        }
    }
}

@Composable
private fun SessionMap(
    title: String,
    data: SessionWithReadings,
    metric: Metric,
    scale: MetricScale,
    modifier: Modifier = Modifier,
) {
    val session = data.session
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(brush = Brand.gradient))
        Text(
            session.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        HeatmapView(
            outline = session.outline,
            widthMetres = session.widthMetres,
            heightMetres = session.heightMetres,
            readings = data.readings,
            metric = metric,
            scale = scale,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .aspectRatio((session.widthMetres / session.heightMetres.coerceAtLeast(0.1f)).coerceIn(0.4f, 2.5f)),
        )
    }
}
