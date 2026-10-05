# HeatNet Member 1: Measurement Engine Design

**Status:** Conversational design and written spec approved; implementation plan ready for review.
**Scope:** Member 1's Android networking and radio-measurement work.

## Purpose and agreed constraints

HeatNet is the app name. This design uses the attached Network Heatmap PRD for product context and Member 1's responsibilities. The user's choices control open decisions: native Kotlin, Android 16 only (API 36), a separate measurement result for teammate integration, Cloudflare download/upload endpoints kept configurable, an HTTP probe fallback whose method stays visible, and the modular Kotlin architecture below.

The module produces the measurements used by room maps. It does not draw maps, collect map coordinates, create sessions, or persist Room records. Member 2 owns the mapping UI; Member 3 owns persistence and compare features. Their existing `Reading` model remains the integration target, with the two explicitly listed contract additions below.

## Module and public boundary

Add a reusable Android library module named `:measurement` to the HeatNet Gradle project. Use Kotlin, coroutines, and OkHttp. The module contains no Compose screens or database. The app can depend on it without depending on measurement implementation details.

Build for API 36 only: `minSdk`, `targetSdk`, and `compileSdk` are all 36, per the user's Android 16 choice.

The public API consists of:

- `ConnectionMonitor`, exposing the current supported transport and internet-validation state as a `StateFlow`. The app captures Wi-Fi or mobile at session start and observes changes to show the PRD's mid-session warning.
- `MeasurementEngine.takeMeasurement(expectedType, onProgress)`, a suspending operation that verifies the expected transport, checks required permission, captures radio data, runs probes, and returns one `MeasurementResult`.
- `MeasurementResult`, a transient measurement record. It carries expected and observed connection types, timestamp, overall status, nullable metric values, packet-loss method, radio fields, and structured issues. It has no session ID or coordinates.

Internally, keep separate components for transport classification, Wi-Fi data, cellular data, HTTP speed and latency probes, ICMP/HTTP loss probes, permission checks, and orchestration. Pure calculations and parsing stay independent of Android framework classes so unit tests can cover them.

The app combines a `MeasurementResult` with its session and tapped coordinates to produce a persisted `Reading`. `MeasurementProgress` stages let the app show work in progress and cancel through coroutine cancellation.

## Shared measurement contract

Use `ConnectionType { WIFI, MOBILE }`. `MeasurementResult` includes:

- `expectedConnectionType`, `observedConnectionType`, `measuredAtEpochMillis`, and `status` (`COMPLETE`, `PARTIAL`, or `BLOCKED`)
- nullable `downloadMbps`, `uploadMbps`, `latencyMs`, `jitterMs`, and `packetLossPct`
- `packetLossMethod` (`ICMP`, `HTTP_PROBE_FAILURES`, or `UNAVAILABLE`)
- nullable `signalDbm`, `linkSpeedMbps`, `wifiBand`, `wifiChannel`, `bssid`, and `networkType`
- a list of typed issues, such as permission denied, unsupported transport, network changed, timeout, or unavailable radio data

For a truthful saved result, the shared `Reading` contract must carry `packetLossMethod`; otherwise the UI cannot distinguish ICMP packet loss from HTTP request failures after save/compare. `wifiBand` must accept `"6"` in addition to `"2.4"` and `"5"`. Preserve these changes when Member 2 and Member 3 integrate. The current checkout contains no `Reading` model yet, so this design records the integration contract without inventing a storage model. Do not edit the supplied PRD as part of this module task; share this contract with the team through the repository.

## Session and measurement flow

1. At session start, the app reads `ConnectionMonitor` and locks the session to Wi-Fi or mobile. An unsupported or unknown active transport cannot start a supported session. Internet validation is informational: still attempt the reading so an offline test can be saved with failed metrics, as the PRD requires.
2. During the session, the UI observes transport changes and shows the PRD warning. A reading request whose active transport does not match the locked type returns `BLOCKED`; the engine never silently measures another transport. The UI lets the user end the session or return to its original connection.
3. At reading start, the engine captures the active Android `Network`, checks the permission required for that session type, and snapshots the available radio fields.
4. HTTP probes use a client bound to that captured `Network`, including network-specific DNS resolution. If the captured network is lost or the default transport changes during a reading, cancel unfinished probes and return a partial result. Do not switch the measurement to another route.
5. Run download and upload in separate phases. Each direction uses two parallel HTTP requests. Cloudflare URLs live in `MeasurementConfig`, not scattered through the probe code.
6. Take ten zero-byte GET requests against the configured download endpoint. `latencyMs` is the arithmetic mean of successful request times; `jitterMs` is their sample standard deviation (`n - 1`) when at least two samples succeed.
7. Attempt twenty ICMP echo probes to the configured Cloudflare host and parse the system `ping` output. If ICMP cannot be executed or produces no replies while HTTP connectivity remains available, take twenty small HTTP probes and store their failure percentage with `HTTP_PROBE_FAILURES`. For either method, calculate failures divided by attempted probes; unstarted probes do not count as loss. The UI must label the fallback as HTTP request failures, not claim it is ICMP packet loss.
8. Return available measurements and issues. A failure in one test does not discard radio data or crash the whole reading.

## Test budget and server behavior

Use `https://speed.cloudflare.com/__down?bytes=0` for latency, `https://speed.cloudflare.com/__down` for download, and `https://speed.cloudflare.com/__up` for upload as the initial configurable HTTP endpoints. Keep the same endpoint and settings for each point in a session. Limit transferred test data to 8 MiB per reading total, split evenly between download and upload, and stop the full measurement at ten seconds. Measure elapsed time with a monotonic clock and calculate direction-specific application-payload throughput as `8 * bytes / elapsedSeconds / 1,000,000` Mbps across the parallel streams. Report partial status when a limit or timeout cuts a probe short; do not count work that never ran as packet loss.

The result describes the path to a public Cloudflare edge, not raw local Wi-Fi throughput. Internet routing, server load, and the user's internet plan can affect it. Send no map coordinates, session IDs, account identifiers, or cookies to the test endpoint. The endpoint still receives normal network requests from the device.

## Radio data and permissions

### Wi-Fi

Read the current `WifiInfo` from the active network's transport information. Record RSSI in dBm, receive link speed in Mbps, frequency-derived band and channel, and BSSID when available. Interpret the receive link speed as the Wi-Fi receive PHY rate, not measured internet download speed. Recognize 2.4, 5, and 6 GHz; preserve unknown values as null.

Declare `ACCESS_WIFI_STATE` and request `ACCESS_FINE_LOCATION` at the point the user starts a Wi-Fi session. Pass `FLAG_INCLUDE_LOCATION_INFO` when registering the network callback. If fine location is denied, block Wi-Fi readings as the PRD specifies and return the permission issue to the UI. If permission is granted but Android still redacts an individual field, return that field as null with an issue; never store a placeholder BSSID as real data. Do not scan for nearby networks or manage Wi-Fi connections.

### Mobile

Use `TelephonyManager` for the active data subscription's signal-strength snapshot and data network type. Match dBm to the active radio technology when Android reports it; return null instead of choosing an unrelated or unavailable cell measurement. Use the Android 5G display override to distinguish NR NSA when the data network type alone reports LTE. Declare `READ_BASIC_PHONE_STATE` for the data network type; it is a non-dangerous permission. Do not request the broader runtime `READ_PHONE_STATE` or privileged precise-phone-state access for this scope. If telephony is unavailable or the modem reports an unavailable value, return null and an issue while retaining the network test results.

Declare `INTERNET` and `ACCESS_NETWORK_STATE` for all measurements. The measurement library reports required and granted permissions and the location rationale. The app's Activity owns the system permission dialog.

## Failure and cancellation behavior

- No internet: retain available radio data; leave failed network metrics null and return their failure issues. Do not crash.
- Permission denied: block a Wi-Fi reading; do not block mobile readings because of Wi-Fi location permission.
- Transport mismatch or unsupported VPN/Ethernet/unknown transport: return `BLOCKED` with a clear reason and perform no test on a different transport.
- Network drop, endpoint error, ICMP restrictions, or timeout: preserve completed metrics and mark the result `PARTIAL` with the applicable issue and method.
- Missing radio field or unavailable telephony hardware: return null for that field.
- Caller cancellation: propagate coroutine cancellation, stop child work and the ping process, and unregister callbacks in `finally` blocks.

## Verification and deliverables

Add focused unit tests for throughput and unit conversion, latency mean and jitter, loss percentages, transport classification, ping-output parsing, permission outcomes, and partial-result orchestration using fake network and radio providers. Add Android tests for runtime permission behavior and platform adapters.

Build and run tests against API 36. Use the emulator for project/build and permission-flow checks; verify actual Wi-Fi and cellular readings on a physical Android 16 phone because emulator radio values may be absent or simulated. Include a short `docs/member-1-viva.md` that explains throughput, parallel connections, latency, jitter, both loss methods, RSSI, Wi-Fi PHY rate versus internet speed, band/channel, cellular technology, and known limits.

## Sources

- [Android Wi-Fi permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
- [Android `WifiInfo`](https://developer.android.com/reference/android/net/wifi/WifiInfo)
- [Android `Network`](https://developer.android.com/reference/android/net/Network)
- [Android `NetworkCallback`](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback)
- [Android `TelephonyManager`](https://developer.android.com/reference/android/telephony/TelephonyManager)
- [Android `READ_BASIC_PHONE_STATE`](https://developer.android.com/reference/android/Manifest.permission#READ_BASIC_PHONE_STATE)
- [Cloudflare Speedtest endpoints and methodology](https://github.com/cloudflare/speedtest/blob/main/README.md)
