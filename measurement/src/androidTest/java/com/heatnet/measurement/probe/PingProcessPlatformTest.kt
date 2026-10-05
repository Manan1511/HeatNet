package com.heatnet.measurement.probe

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingProcessPlatformTest {
    @Test
    fun androidPingProcessIsBoundedAndParsesAvailableLoopbackSummary() {
        val startedAt = System.nanoTime()
        val execution = AndroidPingProcess().run(
            host = "127.0.0.1",
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
}
