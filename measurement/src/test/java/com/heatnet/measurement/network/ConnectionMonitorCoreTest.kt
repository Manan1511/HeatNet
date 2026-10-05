package com.heatnet.measurement.network

import com.heatnet.measurement.radio.RadioSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionMonitorCoreTest {
    @Test
    fun publishesValidationAndClearsLostDefaultNetwork() {
        val registrar = FakeNetworkCallbackRegistrar()
        val snapshots = mutableListOf<NetworkObservation<String>>()
        val monitor = DefaultNetworkMonitorCore(registrar, snapshots::add)

        monitor.start()
        val callback = registrar.callback!!
        callback.onAvailable("wifi-1")
        assertFalse(snapshots.last().hasTransportObservation)
        callback.onCapabilitiesChanged("wifi-1", setOf(NetworkTransport.WIFI), hasValidatedInternet = true)

        assertEquals("wifi-1", snapshots.last().network)
        assertEquals(setOf(NetworkTransport.WIFI), snapshots.last().transports)
        assertTrue(snapshots.last().hasValidatedInternet)
        assertTrue(snapshots.last().hasTransportObservation)

        callback.onCapabilitiesChanged("wifi-1", setOf(NetworkTransport.WIFI), hasValidatedInternet = false)
        assertFalse(snapshots.last().hasValidatedInternet)

        callback.onLost("wifi-1")
        assertNull(snapshots.last().network)
        assertTrue(snapshots.last().transports.isEmpty())
        assertFalse(snapshots.last().hasValidatedInternet)
    }

    @Test
    fun retainsWifiRadioSnapshotFromThePermissionAwareCapabilitiesCallback() {
        val registrar = FakeNetworkCallbackRegistrar()
        val snapshots = mutableListOf<NetworkObservation<String>>()
        val monitor = DefaultNetworkMonitorCore(registrar, snapshots::add)
        val radio = RadioSnapshot(signalDbm = -48, linkSpeedMbps = 600, wifiBand = "5", wifiChannel = 36)

        monitor.start()
        registrar.callback!!.onAvailable("wifi-1")
        registrar.callback!!.onCapabilitiesChanged(
            "wifi-1",
            setOf(NetworkTransport.WIFI),
            hasValidatedInternet = true,
            wifiRadioSnapshot = radio,
        )

        assertEquals(radio, snapshots.last().wifiRadioSnapshot)
    }

    @Test
    fun ignoresEventsForReplacedDefaultNetworks() {
        val registrar = FakeNetworkCallbackRegistrar()
        val snapshots = mutableListOf<NetworkObservation<String>>()
        val monitor = DefaultNetworkMonitorCore(registrar, snapshots::add)

        monitor.start()
        val callback = registrar.callback!!
        callback.onAvailable("old-network")
        callback.onCapabilitiesChanged("old-network", setOf(NetworkTransport.WIFI), hasValidatedInternet = true)
        callback.onAvailable("new-network")
        callback.onCapabilitiesChanged("old-network", setOf(NetworkTransport.CELLULAR), hasValidatedInternet = true)
        callback.onLost("old-network")

        assertEquals("new-network", snapshots.last().network)
        assertTrue(snapshots.last().transports.isEmpty())
        assertFalse(snapshots.last().hasValidatedInternet)
    }

    @Test
    fun startAndStopAreIdempotent() {
        val registrar = FakeNetworkCallbackRegistrar()
        val monitor = DefaultNetworkMonitorCore<String>(registrar) { }

        monitor.start()
        monitor.start()
        assertEquals(1, registrar.registerCount)

        monitor.stop()
        monitor.stop()
        assertEquals(1, registrar.unregisterCount)

        monitor.start()
        monitor.stop()
        assertEquals(2, registrar.registerCount)
        assertEquals(2, registrar.unregisterCount)
    }

    private class FakeNetworkCallbackRegistrar : NetworkCallbackRegistrar<String> {
        var registerCount = 0
            private set
        var unregisterCount = 0
            private set
        var callback: NetworkEventsCallback<String>? = null
            private set

        override fun register(callback: NetworkEventsCallback<String>) {
            registerCount += 1
            this.callback = callback
        }

        override fun unregister(callback: NetworkEventsCallback<String>) {
            unregisterCount += 1
            if (this.callback === callback) this.callback = null
        }
    }
}
