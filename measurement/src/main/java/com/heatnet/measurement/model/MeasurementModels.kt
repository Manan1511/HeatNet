package com.heatnet.measurement.model

enum class ConnectionType {
    WIFI,
    MOBILE,
}

enum class MeasurementStatus {
    COMPLETE,
    PARTIAL,
    BLOCKED,
}

enum class PacketLossMethod {
    ICMP,
    HTTP_PROBE_FAILURES,
    UNAVAILABLE,
}

enum class MeasurementStage {
    VERIFYING_NETWORK,
    CAPTURING_RADIO,
    DOWNLOAD,
    UPLOAD,
    LATENCY,
    PACKET_LOSS,
    COMPLETE,
}

data class MeasurementProgress(
    val stage: MeasurementStage,
)

enum class IssueCode {
    PERMISSION_DENIED,
    UNSUPPORTED_TRANSPORT,
    TRANSPORT_MISMATCH,
    NETWORK_CHANGED,
    NO_INTERNET,
    TIMEOUT,
    ENDPOINT_FAILURE,
    ICMP_UNAVAILABLE,
    RADIO_UNAVAILABLE,
    FIELD_REDACTED,
}

data class MeasurementIssue(
    val code: IssueCode,
    val detail: String? = null,
)

data class MeasurementResult(
    val expectedConnectionType: ConnectionType,
    val observedConnectionType: ConnectionType?,
    val measuredAtEpochMillis: Long,
    val status: MeasurementStatus,
    val downloadMbps: Float? = null,
    val uploadMbps: Float? = null,
    val latencyMs: Float? = null,
    val jitterMs: Float? = null,
    val packetLossPct: Float? = null,
    val packetLossMethod: PacketLossMethod = PacketLossMethod.UNAVAILABLE,
    val signalDbm: Int? = null,
    val linkSpeedMbps: Int? = null,
    val wifiBand: String? = null,
    val wifiChannel: Int? = null,
    val bssid: String? = null,
    val networkType: String? = null,
    val isDemo: Boolean = false,
    val issues: List<MeasurementIssue> = emptyList(),
)
