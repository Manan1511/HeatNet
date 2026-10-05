package com.heatnet.measurement.probe

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkBoundHttpClientFactoryPlatformTest {
    @Test
    fun clientUsesTheCapturedNetworksSocketAndDns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        assertNotNull("API 36 emulator should expose its active default network", network)
        val capturedNetwork = requireNotNull(network)

        val client = NetworkBoundHttpClientFactory().create(capturedNetwork)

        assertEquals(capturedNetwork.socketFactory.javaClass, client.socketFactory.javaClass)
        assertEquals(
            capturedNetwork.getAllByName("localhost").toList(),
            client.dns.lookup("localhost"),
        )
    }
}
