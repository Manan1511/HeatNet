package com.heatnet.measurement.network

import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransportClassifierTest {
    @Test
    fun rejectsUnsupportedOrAmbiguousTransports() {
        assertEquals(ConnectionType.WIFI, TransportClassifier.classify(setOf(NetworkTransport.WIFI)))
        assertEquals(ConnectionType.MOBILE, TransportClassifier.classify(setOf(NetworkTransport.CELLULAR)))

        val unsupportedOrAmbiguous = listOf(
            emptySet(),
            setOf(NetworkTransport.WIFI, NetworkTransport.CELLULAR),
            setOf(NetworkTransport.VPN),
            setOf(NetworkTransport.ETHERNET),
            setOf(NetworkTransport.OTHER),
            setOf(NetworkTransport.WIFI, NetworkTransport.VPN),
        )
        unsupportedOrAmbiguous.forEach { transports ->
            assertNull("$transports must not be measured as Wi-Fi or mobile", TransportClassifier.classify(transports))
        }
    }
}
