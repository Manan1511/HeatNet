package com.heatnet.measurement.probe

data class PingSummary(
    val transmitted: Int,
    val received: Int,
)

object PingSummaryParser {
    private val summaryPattern = Regex(
        pattern = """(\d+)\s+packets?\s+transmitted,\s*(\d+)\s+(?:packets?\s+)?received""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(output: String): PingSummary? {
        val match = summaryPattern.find(output) ?: return null
        val transmitted = match.groupValues[1].toIntOrNull() ?: return null
        val received = match.groupValues[2].toIntOrNull() ?: return null
        if (transmitted <= 0 || received < 0 || received > transmitted) return null
        return PingSummary(transmitted, received)
    }
}
