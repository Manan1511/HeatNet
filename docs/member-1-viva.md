# HeatNet Member 1: Viva Notes

## A short explanation

“I built HeatNet’s Android measurement library in Kotlin. It captures the active Wi-Fi or mobile `Network`, reads the radio fields Android makes available, and measures throughput, HTTP latency, jitter, and connection loss. HTTP requests stay bound to the captured network. Each result reports its status and issues, so partial or unavailable values are not mistaken for real measurements.”

## What each measurement means

### Download and upload throughput

The app sends two HTTP requests concurrently for each direction. It finishes the download phase before starting upload. Each direction has a shared maximum payload budget of 4 MiB, so one reading transfers no more than 8 MiB in total. The overall reading has a ten-second deadline.

Throughput is calculated from bytes actually transferred and elapsed monotonic time:

```text
Mbps = payload bytes × 8 ÷ elapsed seconds ÷ 1,000,000
```

The configured starting endpoints are Cloudflare’s download and upload endpoints. They live in `MeasurementConfig`, so the team can replace them. The HTTP client uses the captured Android `Network` for sockets and DNS, rather than silently switching to another route. A shortened or failed transfer keeps its usable result and marks the reading partial.

This is application payload throughput to a public Cloudflare edge. It is not a measurement of the router’s maximum Wi-Fi capacity. The internet plan, signal, routing, congestion, device, and server load can all affect the result.

### HTTP latency and jitter

HeatNet sends ten zero-byte HTTP GET requests to the configured latency endpoint. It averages the successful round-trip times for `latencyMs`. Failed requests do not become latency samples.

`jitterMs` is the sample standard deviation of the successful latency samples. It describes how spread out this set of HTTP timings was; it is not a separate raw packet-delay measurement. With fewer than two successful samples, jitter is undefined and remains null.

### Connection loss

HeatNet first attempts twenty ICMP echo probes using Android’s `ping` executable. If ICMP cannot run or receives no replies, the app may use HTTP fallback probes only when an earlier HTTP request succeeded.

Both methods calculate a percentage from probes that actually started:

```text
loss percentage = failed probes ÷ attempted probes × 100
```

The result keeps a `packetLossMethod` label:

- `ICMP` means echo replies were measured.
- `HTTP_PROBE_FAILURES` means HTTP request failures were measured as a fallback. This is an app-level reachability signal, not a claim about dropped IP packets.
- `UNAVAILABLE` means neither method produced a usable percentage.

Requests that never started because the deadline expired are excluded from the denominator.

## Radio fields

- **RSSI / signal dBm:** A radio signal measurement reported by Android. Values are usually negative; closer to zero is stronger within the same measurement context. Wi-Fi and cellular readings use different radio technologies, so avoid treating them as directly comparable.
- **Wi-Fi link speed:** Android’s receive PHY link speed in Mbps. It describes the negotiated Wi-Fi radio link, not measured internet download speed.
- **Wi-Fi band and channel:** Derived from the active connection’s frequency. HeatNet preserves bands as `"2.4"`, `"5"`, or `"6"`, with a channel number such as channel 1 at 5955 MHz on 6 GHz.
- **BSSID:** The connected access point identifier when Android exposes it. A masked or unavailable value is returned as null.
- **Mobile network type:** The active data network type, such as LTE or NR. When Android reports LTE with an NR NSA display override, HeatNet labels it `NR-NSA`. Signal strength is associated only with a matching radio technology.

Unavailable and redacted values stay null and carry an issue. The library does not substitute placeholders or choose an unrelated cell’s signal.

## Permissions and measurement flow

The library declares `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `ACCESS_FINE_LOCATION`, and `READ_BASIC_PHONE_STATE`. Wi-Fi radio data is gated on fine-location permission. Mobile measurements do not require the Wi-Fi location gate. `READ_BASIC_PHONE_STATE` supports reading the data network type; the design does not request the broader runtime `READ_PHONE_STATE` permission.

`PermissionGate` reports whether permission is ready; it does not display a system dialog. The app Activity owns the permission prompt. `ConnectionMonitor` is shared app state, and the measurement engine does not stop it when a reading ends or is cancelled.

The engine verifies the requested transport, captures radio data, runs each probe under one deadline, and returns `COMPLETE`, `PARTIAL`, or `BLOCKED`. If a route changes, unfinished work is cancelled and completed values are retained. Caller cancellation is propagated so HTTP calls, the ping process, radio callbacks, and flow collection can clean up.

## Passing data to the rest of HeatNet

`MeasurementResult` is a transient measurement object. It contains the expected and observed transport, timestamp, status, metric and radio values, packet-loss method, and issues. It deliberately contains no room coordinates, session identifier, or database behavior.

When the app later creates its shared `Reading`, it should combine this result with the session and point identifiers owned by the mapping and persistence layers. Preserve `packetLossMethod` so screens and comparisons can distinguish ICMP from HTTP request failures. Preserve the `wifiBand` value `"6"` alongside the existing `"2.4"` and `"5"` values.

## Limits to mention

- A public-edge test varies with internet routing, congestion, server load, and the device’s data plan.
- HTTP fallback reports failed app requests; it does not inspect packets on the network.
- Android may redact Wi-Fi identifiers or lack cellular modem data, even when permission is granted.
- Emulators can verify API wiring and permission merging, but their Wi-Fi and cellular radio values are absent or simulated. Physical Android 16 devices are needed to validate real radio readings.
- This Member 1 module does not draw heatmaps, create sessions, store readings, or attach coordinates. Those belong to the other app layers.

## Useful code locations

- Public result and status contract: `measurement/src/main/java/com/heatnet/measurement/model/MeasurementModels.kt`
- Probe budgets and configurable endpoints: `measurement/src/main/java/com/heatnet/measurement/model/MeasurementConfig.kt`
- Orchestration: `measurement/src/main/java/com/heatnet/measurement/engine/MeasurementEngine.kt`
- Android wiring: `measurement/src/main/java/com/heatnet/measurement/engine/MeasurementDependencies.kt`
- Pure metric calculations: `measurement/src/main/java/com/heatnet/measurement/math/MetricCalculations.kt`
- Permission and radio readers: `measurement/src/main/java/com/heatnet/measurement/permissions/PermissionGate.kt` and `measurement/src/main/java/com/heatnet/measurement/radio/`
- Network-bound HTTP and packet-loss probes: `measurement/src/main/java/com/heatnet/measurement/probe/`
