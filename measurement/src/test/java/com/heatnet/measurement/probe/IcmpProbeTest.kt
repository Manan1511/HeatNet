package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.PacketLossMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class IcmpProbeTest {
    @Test
    fun reportsIcmpLossUsingTransmittedEchoes() = runBlocking {
        val process = FakePingProcess(PingExecution("20 packets transmitted, 15 received, 25% packet loss", 0, true))

        val result = IcmpProbe(process).measure(MeasurementConfig(), deadline())

        assertEquals(PacketLossMethod.ICMP, result.method)
        assertEquals(20, result.attemptedProbes)
        assertEquals(15, result.successfulProbes)
        assertEquals(25f, result.lossPct!!, 0.001f)
        assertEquals("speed.cloudflare.com", process.host)
        assertEquals(20, process.count)
    }

    @Test
    fun executableFailureReturnsUnavailableInsteadOfInventingLoss() = runBlocking {
        val result = IcmpProbe(FakePingProcess(PingExecution("", exitCode = null, started = false)))
            .measure(MeasurementConfig(), deadline())

        assertEquals(PacketLossMethod.UNAVAILABLE, result.method)
        assertEquals(null, result.lossPct)
        assertTrue(result.issues.any { it.code == IssueCode.ICMP_UNAVAILABLE })
    }

    @Test
    fun zeroRepliesAreAvailableForPacketProbeToReplaceWithHttpFallback() = runBlocking {
        val result = IcmpProbe(
            FakePingProcess(PingExecution("20 packets transmitted, 0 packets received, 100% packet loss", 1, true)),
        ).measure(MeasurementConfig(), deadline())

        assertEquals(PacketLossMethod.ICMP, result.method)
        assertEquals(0, result.successfulProbes)
        assertEquals(100f, result.lossPct!!, 0.001f)
    }

    @Test
    fun cancellationInterruptsAndStopsThePingRunner() = runBlocking {
        val process = BlockingPingProcess()
        val job = launch(Dispatchers.Default) { IcmpProbe(process).measure(MeasurementConfig(), deadline()) }
        assertTrue(process.started.await(2, TimeUnit.SECONDS))

        job.cancelAndJoin()

        assertTrue(process.interrupted.await(2, TimeUnit.SECONDS))
        assertFalse(job.isActive)
    }

    private fun deadline() = System.nanoTime() + 10_000_000_000L

    private class FakePingProcess(private val execution: PingExecution) : PingProcess {
        var host: String? = null
        var count: Int? = null
        override fun run(host: String, count: Int, intervalMillis: Int, timeoutMillis: Long): PingExecution {
            this.host = host
            this.count = count
            return execution
        }
    }

    private class BlockingPingProcess : PingProcess {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)

        override fun run(host: String, count: Int, intervalMillis: Int, timeoutMillis: Long): PingExecution {
            started.countDown()
            try {
                CountDownLatch(1).await()
            } catch (error: InterruptedException) {
                interrupted.countDown()
                throw error
            }
            return PingExecution("", 0, true)
        }
    }
}
