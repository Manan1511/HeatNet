package com.heatnet.measurement.probe

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingProcessPlatformTest {
    @Test
    fun androidPingProcessIsBoundedAndParsesAvailableLoopbackSummary() {
        val startedAt = System.nanoTime()
        val execution = AndroidPingProcess().run(
            route = IcmpRoute("127.0.0.1", "lo"),
            count = 1,
            intervalMillis = 200,
            timeoutMillis = 2_000,
        )
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L

        assertTrue("Android 16 should expose the ping utility", execution.started)
        assertTrue("ping should finish within its timeout plus cleanup", elapsedMillis < 5_000L)
        if (execution.started && !execution.timedOut) {
            PingSummaryParser.parse(execution.output)?.let { summary ->
                assertEquals(1, summary.transmitted)
                assertTrue(summary.received in 0..1)
            }
        }
    }

    @Test
    fun capturedNetworkPingUsesItsInterfaceAndStopsAtRequestedCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        assertNotNull("API 36 emulator should expose its active network", network)
        val capturedNetwork = requireNotNull(network)
        val route = AndroidIcmpRouteResolver(context).resolve(capturedNetwork, "192.0.2.1")
        assertNotNull("captured network must provide its own interface and resolver", route)

        val startedAt = System.nanoTime()
        val execution = AndroidPingProcess().run(
            route = requireNotNull(route),
            count = 2,
            intervalMillis = 200,
            timeoutMillis = 5_000,
        )
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L

        assertTrue("Android 16 should expose the ping utility", execution.started)
        assertTrue("count-bounded ping should complete inside its process timeout", elapsedMillis < 5_000L)
        assertFalse("the two-probe command should finish before its timeout", execution.timedOut)
        val summary = PingSummaryParser.parse(execution.output)
        assertNotNull("ping should return an attempt summary", summary)
        assertEquals(2, requireNotNull(summary).transmitted)
    }
}
