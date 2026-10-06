package com.heatnet.ui.screens

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus

/** What to do with one finished measurement from Member 1's engine. */
object MeasureRules {
    /**
     * These mean the test never ran on the session's connection, so there is nothing honest to save.
     * Anything else (no internet, timeouts, failed probes) is saved with null values, as the PRD asks.
     */
    private val notSaved = setOf(
        IssueCode.TRANSPORT_MISMATCH,
        IssueCode.UNSUPPORTED_TRANSPORT,
        IssueCode.PERMISSION_DENIED,
    )

    fun shouldSave(result: MeasurementResult): Boolean = result.issues.none { it.code in notSaved }

    /** A plain sentence for the user, or null when everything worked. */
    fun message(result: MeasurementResult, sessionType: ConnectionType): String? {
        val codes = result.issues.map { it.code }.toSet()
        return when {
            IssueCode.PERMISSION_DENIED in codes ->
                "Location permission is off, so Wi-Fi details can't be read. Allow it in settings and try again. Nothing was saved."
            IssueCode.TRANSPORT_MISMATCH in codes ->
                "You're not on ${sessionType.label} right now. Switch back to take a reading. Nothing was saved."
            IssueCode.UNSUPPORTED_TRANSPORT in codes ->
                "This connection isn't Wi-Fi or mobile data. Nothing was saved."
            IssueCode.NO_INTERNET in codes -> "No internet connection. The reading was saved and marked as failed."
            IssueCode.NETWORK_CHANGED in codes -> "The network changed during the test. Saved with some values missing."
            result.status == MeasurementStatus.BLOCKED -> "The test couldn't run. The reading was saved and marked as failed."
            result.status == MeasurementStatus.PARTIAL -> "Some tests didn't finish. Saved with some values missing."
            else -> null
        }
    }
}
