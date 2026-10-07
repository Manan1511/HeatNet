package com.heatnet.ui.screens

import com.heatnet.data.analysis.MetricComparison
import com.heatnet.data.analysis.Verdict
import com.heatnet.ui.heatmap.formatValue

/** Plain-language lines for the compare screen (PRD section 3: raw numbers only as detail). */
val Verdict.label: String
    get() = when (this) {
        Verdict.IMPROVED -> "Better"
        Verdict.WORSE -> "Worse"
        Verdict.SIMILAR -> "About the same"
        Verdict.NOT_ENOUGH_DATA -> "Not enough data"
    }

fun describe(comparison: MetricComparison): String {
    val before = comparison.before
    val after = comparison.after
    if (comparison.verdict == Verdict.NOT_ENOUGH_DATA || before == null || after == null) {
        return "${comparison.metric.label}: not enough shared room areas with usable data to compare."
    }
    val unit = comparison.metric.unit
    return "${comparison.metric.label}: ${comparison.verdict.label.lowercase()}. " +
        "Average across ${before.count} shared room area${if (before.count == 1) "" else "s"} went from " +
        "${formatValue(before.mean)} $unit to ${formatValue(after.mean)} $unit."
}
