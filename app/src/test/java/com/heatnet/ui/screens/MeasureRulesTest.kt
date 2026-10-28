package com.heatnet.ui.screens

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureRulesTest {
    private fun result(status: MeasurementStatus, vararg codes: IssueCode) = MeasurementResult(
        expectedConnectionType = ConnectionType.WIFI,
        observedConnectionType = ConnectionType.WIFI,
        measuredAtEpochMillis = 0,
        status = status,
        issues = codes.map { MeasurementIssue(it) },
    )

    @Test
    fun completeReadingIsSavedWithoutMessage() {
        val r = result(MeasurementStatus.COMPLETE)
        assertTrue(MeasureRules.shouldSave(r))
        assertNull(MeasureRules.message(r, ConnectionType.WIFI))
    }

    @Test
    fun noInternetIsSavedAsFailed() {
        val r = result(MeasurementStatus.BLOCKED, IssueCode.NO_INTERNET)
        assertTrue(MeasureRules.shouldSave(r))
        assertNotNull(MeasureRules.message(r, ConnectionType.WIFI))
    }

    @Test
    fun wrongConnectionOrMissingPermissionIsNotSaved() {
        assertFalse(MeasureRules.shouldSave(result(MeasurementStatus.BLOCKED, IssueCode.TRANSPORT_MISMATCH)))
        assertFalse(MeasureRules.shouldSave(result(MeasurementStatus.BLOCKED, IssueCode.PERMISSION_DENIED)))
        assertFalse(MeasureRules.shouldSave(result(MeasurementStatus.BLOCKED, IssueCode.UNSUPPORTED_TRANSPORT)))
    }

}
