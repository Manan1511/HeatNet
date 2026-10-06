package com.heatnet.ui.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/**
 * Colour key for the current metric, from the scale's min (left) to max (right), plus the
 * honesty note from PRD section 7: faded colour is an estimate, only dots are measured.
 */
@Composable
fun HeatmapLegend(scale: MetricScale, title: String, modifier: Modifier = Modifier) {
    val unit = scale.metric.unit
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$title (${if (scale.metric.higherIsBetter) "higher is better" else "lower is better"})",
            style = MaterialTheme.typography.labelLarge,
        )
        val steps = 24
        val colors = List(steps + 1) { i ->
            val value = scale.min + (scale.max - scale.min) * i / steps
            Color(scale.colorOf(value))
        }
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            drawRoundRect(Brush.horizontalGradient(colors), cornerRadius = CornerRadius(4.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${formatValue(scale.min)} $unit", style = MaterialTheme.typography.labelSmall)
            Text("${formatValue(scale.max)} $unit", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            "Dots are real readings. Bright colour is near a reading; faded colour is an estimate. " +
                "The estimate does not know about walls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatValue(value: Float): String =
    if (abs(value) >= 10f) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.1f", value)
