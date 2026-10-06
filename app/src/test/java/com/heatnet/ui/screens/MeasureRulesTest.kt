package com.heatnet.ui.screens

import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.MeasurementIssue
import com.heatnet.measurement.model.MeasurementResult
import com.heatnet.measurement.model.MeasurementStatus
import com.heatnet.ui.measure.FakeMeasurementSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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

    @Test
    fun fakeSourceIsBetterNearTheRouter() = runBlocking {
        val fake = FakeMeasurementSource(routerX = 0f, routerY = 0f, stageDelayMillis = 0, random = Random(1))
        val near = fake.measure(ConnectionType.WIFI, 0.5f, 0.5f) {}
        val far = fake.measure(ConnectionType.WIFI, 12f, 9f) {}
        assertEquals(MeasurementStatus.COMPLETE, near.status)
        assertTrue(near.signalDbm!! > far.signalDbm!!)
        assertTrue(near.downloadMbps!! > far.downloadMbps!!)
        assertNull(fake.measure(ConnectionType.MOBILE, 1f, 1f) {}.linkSpeedMbps)
    }
}
