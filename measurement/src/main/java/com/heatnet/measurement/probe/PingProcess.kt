package com.heatnet.measurement.probe

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal data class PingExecution(
    val output: String,
    val exitCode: Int?,
    val started: Boolean,
    val timedOut: Boolean = false,
)

internal interface PingProcess {
    fun run(route: IcmpRoute, count: Int, intervalMillis: Int, timeoutMillis: Long): PingExecution
}

internal class AndroidPingProcess : PingProcess {
    override fun run(route: IcmpRoute, count: Int, intervalMillis: Int, timeoutMillis: Long): PingExecution {
        if (count <= 0 || timeoutMillis <= 0L) {
            return PingExecution("", exitCode = null, started = false)
        }

        val process = try {
            ProcessBuilder(*buildPingCommand(route, count, intervalMillis).toTypedArray())
                .redirectErrorStream(true)
                .start()
        } catch (_: IOException) {
            return PingExecution("", exitCode = null, started = false)
        } catch (_: SecurityException) {
            return PingExecution("", exitCode = null, started = false)
        }

        val output = CompletableFuture.supplyAsync {
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }
        return try {
            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) process.destroyForcibly()
            if (!finished) process.waitFor(PROCESS_STOP_WAIT_MILLIS, TimeUnit.MILLISECONDS)
            val text = runCatching { output.get(OUTPUT_DRAIN_WAIT_MILLIS, TimeUnit.MILLISECONDS) }.getOrDefault("")
            PingExecution(
                output = text,
                exitCode = if (process.isAlive) null else process.exitValue(),
                started = true,
                timedOut = !finished,
            )
        } catch (interrupted: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            throw interrupted
        } finally {
            if (process.isAlive) process.destroyForcibly()
            runCatching { process.inputStream.close() }
            runCatching { process.outputStream.close() }
            runCatching { process.errorStream.close() }
        }
    }
}

internal fun buildPingCommand(
    route: IcmpRoute,
    count: Int,
    intervalMillis: Int,
): List<String> {
    val intervalSeconds = String.format(
        java.util.Locale.US,
        "%.3f",
        intervalMillis.coerceAtLeast(100) / 1_000.0,
    )
    return listOf(
        "ping",
        "-I", route.interfaceName,
        "-c", count.toString(),
        "-i", intervalSeconds,
        "-W", "1",
        route.hostAddress,
    )
}

private const val PROCESS_STOP_WAIT_MILLIS = 250L
private const val OUTPUT_DRAIN_WAIT_MILLIS = 500L
