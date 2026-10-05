package com.heatnet.measurement.probe

import com.heatnet.measurement.model.MeasurementConfig
import com.heatnet.measurement.model.IssueCode
import com.heatnet.measurement.model.PacketLossMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketLossProbeTest {
    @Test
    fun fallbackRequiresReachableHttp() = runBlocking {
        val packetProbe = PacketLossProbe(icmpWithZeroReplies())
        var fallbackCalls = 0

        val result = packetProbe.measureWithFallback(
            MeasurementConfig(),
            httpReachable = false,
            deadlineNanos = deadline(),
        ) {
            fallbackCalls += 1
            httpSuccess()
        }

        assertEquals(PacketLossMethod.UNAVAILABLE, result.method)
        assertNull(result.lossPct)
        assertEquals(0, fallbackCalls)
    }

    @Test
    fun offlineLossIsUnavailable() = runBlocking {
        val packetProbe = PacketLossProbe(icmpUnavailable())

        val result = packetProbe.measureWithFallback(
            MeasurementConfig(),
            httpReachable = false,
            deadlineNanos = deadline(),
        ) { httpSuccess() }

        assertEquals(PacketLossMethod.UNAVAILABLE, result.method)
        assertNull(result.lossPct)
        assertTrue(result.issues.any { it.code == IssueCode.ICMP_UNAVAILABLE })
        assertTrue(result.issues.any { it.code == IssueCode.NO_INTERNET })
    }

    @Test
    fun usesLabeledHttpFailureMethodWhenIcmpHasNoRepliesAndHttpWorkedEarlier() = runBlocking {
        val packetProbe = PacketLossProbe(icmpWithZeroReplies())

        val result = packetProbe.measureWithFallback(
            MeasurementConfig(),
            httpReachable = true,
            deadlineNanos = deadline(),
        ) { httpSuccess() }

        assertEquals(PacketLossMethod.HTTP_PROBE_FAILURES, result.method)
        assertEquals(10f, result.lossPct!!, 0.001f)
        assertEquals(20, result.attemptedProbes)
        assertEquals(18, result.successfulProbes)
    }

    @Test
    fun keepsPartialIcmpLossWhenAtLeastOneReplyArrived() = runBlocking {
        val packetProbe = PacketLossProbe(
            IcmpProbe(FakePingProcess(PingExecution("20 packets transmitted, 18 received", 0, true))),
        )
        var fallbackCalls = 0

        val result = packetProbe.measureWithFallback(
            MeasurementConfig(),
            httpReachable = true,
            deadlineNanos = deadline(),
        ) {
            fallbackCalls += 1
            httpSuccess()
        }

        assertEquals(PacketLossMethod.ICMP, result.method)
        assertEquals(10f, result.lossPct!!, 0.001f)
        assertEquals(0, fallbackCalls)
    }

    private fun icmpWithZeroReplies() = IcmpProbe(
        FakePingProcess(PingExecution("20 packets transmitted, 0 packets received, 100% packet loss", 1, true)),
    )

    private fun icmpUnavailable() = IcmpProbe(
        FakePingProcess(PingExecution("ping not found", exitCode = null, started = false)),
    )

    private fun httpSuccess() = PacketLossMeasurement(
        lossPct = 10f,
        method = PacketLossMethod.HTTP_PROBE_FAILURES,
        attemptedProbes = 20,
        successfulProbes = 18,
    )

    private fun deadline() = System.nanoTime() + 10_000_000_000L

    private class FakePingProcess(private val execution: PingExecution) : PingProcess {
        override fun run(host: String, count: Int, intervalMillis: Int, timeoutMillis: Long) = execution
    }
}
