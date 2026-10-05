package com.heatnet.measurement.radio

import com.heatnet.measurement.model.IssueCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellularRadioReaderTest {
    @Test
    fun unavailableTechnologyKeepsSignalNull() {
        val source = FakeCellularRadioSource(
            activeSubscriptionId = 12,
            data = CellularRadioData(
                dataTechnology = CellularTechnology.LTE,
                signalSamples = listOf(CellularSignalSample(CellularTechnology.NR, -86)),
            ),
        )

        val snapshot = CellularRadioReader.fromSource(source).read()

        assertNull(snapshot.signalDbm)
        assertEquals("LTE", snapshot.networkType)
        assertTrue(snapshot.issues.any { it.code == IssueCode.RADIO_UNAVAILABLE })
    }

    @Test
    fun readsOnlyTheActiveDataSubscription() {
        val source = FakeCellularRadioSource(
            activeSubscriptionId = 7,
            data = CellularRadioData(
                dataTechnology = CellularTechnology.LTE,
                signalSamples = listOf(CellularSignalSample(CellularTechnology.LTE, -92)),
            ),
        )

        val snapshot = CellularRadioReader.fromSource(source).read()

        assertEquals(listOf(7), source.requestedSubscriptions)
        assertEquals(-92, snapshot.signalDbm)
    }

    @Test
    fun labelsLteNrAndNrNsaWithoutGuessingSignalTechnology() {
        val source = FakeCellularRadioSource(
            activeSubscriptionId = 1,
            data = CellularRadioData(
                dataTechnology = CellularTechnology.LTE,
                displayOverride = CellularDisplayOverride.NR_NSA,
                signalSamples = listOf(CellularSignalSample(CellularTechnology.LTE, -88)),
            ),
        )
        val reader = CellularRadioReader.fromSource(source)

        assertEquals("NR-NSA", reader.read().networkType)
        assertEquals(-88, reader.read().signalDbm)

        source.data = CellularRadioData(
            dataTechnology = CellularTechnology.NR,
            signalSamples = listOf(CellularSignalSample(CellularTechnology.NR, -79)),
        )
        val nr = reader.read()
        assertEquals("NR", nr.networkType)
        assertEquals(-79, nr.signalDbm)

        source.data = CellularRadioData(
            dataTechnology = CellularTechnology.LTE,
            signalSamples = listOf(CellularSignalSample(CellularTechnology.LTE, -95)),
        )
        assertEquals("LTE", reader.read().networkType)
    }

    @Test
    fun labelsNrAdvancedSeparatelyFromLteAndNrNsa() {
        val advanced = CellularDisplayOverride.entries.firstOrNull { it.name == "NR_ADVANCED" }
        assertTrue("the display override model must preserve NR Advanced", advanced != null)
        val source = FakeCellularRadioSource(
            activeSubscriptionId = 1,
            data = CellularRadioData(
                dataTechnology = CellularTechnology.LTE,
                displayOverride = advanced ?: CellularDisplayOverride.NR_NSA,
                signalSamples = listOf(CellularSignalSample(CellularTechnology.LTE, -88)),
            ),
        )

        val snapshot = CellularRadioReader.fromSource(source).read()

        assertEquals("NR-ADVANCED", snapshot.networkType)
        assertEquals(-88, snapshot.signalDbm)
    }

    @Test
    fun missingActiveSubscriptionReturnsUnavailableRadio() {
        val snapshot = CellularRadioReader.fromSource(
            FakeCellularRadioSource(activeSubscriptionId = null, data = null),
        ).read()

        assertNull(snapshot.signalDbm)
        assertNull(snapshot.networkType)
        assertTrue(snapshot.issues.any { it.code == IssueCode.RADIO_UNAVAILABLE })
    }

    private class FakeCellularRadioSource(
        private val activeSubscriptionId: Int?,
        var data: CellularRadioData?,
    ) : CellularRadioSource {
        val requestedSubscriptions = mutableListOf<Int>()

        override fun activeDataSubscriptionId(): Int? = activeSubscriptionId

        override fun readForSubscription(subscriptionId: Int): CellularRadioData? {
            requestedSubscriptions += subscriptionId
            return data
        }
    }
}
