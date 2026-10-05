package com.heatnet.measurement.model

data class MeasurementConfig(
    val latencyUrl: String = "https://speed.cloudflare.com/__down?bytes=0",
    val downloadUrl: String = "https://speed.cloudflare.com/__down",
    val uploadUrl: String = "https://speed.cloudflare.com/__up",
    val icmpHost: String = "speed.cloudflare.com",
    val parallelStreams: Int = 2,
    val maxBytesPerDirection: Long = 4L * 1024L * 1024L,
    val overallTimeoutMillis: Long = 10_000L,
    val latencySampleCount: Int = 10,
    val icmpProbeCount: Int = 20,
    val httpFallbackProbeCount: Int = 20,
)
