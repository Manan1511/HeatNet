# HeatNet: Network Quality Heatmaps for Indoor Spaces

> **Draft status (2026-10-28):** This report describes the implemented app and verified software behavior. The team has not supplied measurements from the required three real rooms, so empirical results and conclusions about real-world accuracy are intentionally left open.

## Abstract

HeatNet is an Android application that helps users compare network quality at different positions inside a room. A user draws a room outline, sets its scale, and takes Wi-Fi or mobile-data readings at selected points. The app records network performance and available radio information, saves readings locally, and visualizes spatial variation as heatmaps. Inverse distance weighting (IDW) estimates values between measured points, while a distance fade indicates where the map is based on sparse measurements.

The project demonstrates network-layer and application-layer measurements, Android network binding, radio metadata, local persistence, and the limits of interpolating sparse data. The software builds and its automated Android checks pass. Field measurements on physical Android 16 phones remain necessary before making claims about performance in real rooms.

## 1. Problem and objectives

Indoor signal bars do not show how network quality varies across a room, and signal strength alone does not describe how responsive or fast internet services feel. HeatNet maps measurements at manually selected room positions so a user can compare download and upload throughput, latency, packet loss, and available signal information.

The app keeps Wi-Fi and mobile-data sessions separate. This avoids mixing results from different access networks in one map. Measurements are stored on-device; the project has no account system or cloud database.

## 2. System architecture

The app is organized into three Android modules:

- **`:app`** provides the Compose interface, room drawing and scale entry, reading placement, heatmap screens, and app wiring.
- **`:measurement`** captures the active Android network and radio fields, runs HTTP and ICMP probes, and returns a typed result with status and issues.
- **`:data`** stores sessions and readings in a local Room database and supports session comparison and weak-spot analysis.

The mapping layer adds room coordinates and session identifiers after the measurement engine returns. Each reading is persisted locally. Heatmap rendering uses measured points as input and labels the interpolated surface as an estimate.

## 3. Network measurement methods

### Throughput

HeatNet measures application payload delivered by HTTPS requests to configurable Cloudflare speed-test endpoints. It uses two parallel streams per direction and caps the payload at 4 MiB for download and 4 MiB for upload. One reading has a ten-second overall deadline.

The reported throughput is calculated as:

```text
Mbps = payload bytes × 8 ÷ elapsed seconds ÷ 1,000,000
```

This is end-to-end goodput to a public server. It depends on the device, access link, internet plan, routing, congestion, and server load. It does not measure the maximum capacity of the Wi-Fi radio link.

### Latency and jitter

The app sends ten zero-byte HTTPS requests to the configured latency endpoint. It reports the mean round-trip time of successful requests. Jitter is the sample standard deviation of those successful timings. Failed requests are not treated as latency samples; jitter is left unavailable when there are fewer than two successful samples.

These are application request timings, so they include transport and server response effects. They are not a pure measurement of propagation delay.

### Packet loss and fallback

HeatNet attempts up to twenty ICMP echo probes to `speed.cloudflare.com`. When ICMP is unavailable or receives no replies, the app can use HTTP request probes if an earlier HTTP request succeeded. The result includes a method label:

- **ICMP** reports echo-probe loss.
- **HTTP_PROBE_FAILURES** reports the fraction of attempted HTTP requests that failed. This is an application-level failure rate, not a claim that the same percentage of IP packets was dropped.
- **UNAVAILABLE** means neither method produced a usable percentage.

Only probes that started are included in the denominator. A deadline that prevents a probe from starting does not count as a failed probe.

### Radio information

For Wi-Fi, Android may provide RSSI, receive PHY link speed, frequency-derived band and channel, and BSSID. Fine-location permission is required by Android to expose Wi-Fi information; HeatNet uses this permission for Wi-Fi metadata and not to determine room position. For mobile data, the app reports the available cellular signal and network type. Android can redact fields or the device may not expose modem details, so unavailable values remain null rather than being guessed.

Wi-Fi PHY link speed is distinct from measured internet throughput. Signal dBm values from Wi-Fi and cellular radio systems should not be treated as directly comparable measurements.

## 4. Heatmap method and limitations

The user marks measurement positions manually on a drawn and scaled room outline. IDW estimates values at locations between readings by assigning greater weight to closer measurements. The method is deterministic and useful for showing spatial trends, but it does not model walls, doors, furniture, reflections, or interference. The distance fade helps distinguish estimates from measured points, but it cannot remove this uncertainty.

Results also depend on manual placement accuracy, the selected public endpoint, Android radio reporting, and whether the network permits ICMP. HeatNet should be interpreted as a room-scale comparison aid, not a calibrated survey instrument.

## 5. Implementation and software verification

The project targets Android 16 (API 36), uses Java/Kotlin bytecode target 17, and builds with Gradle 9.8.0 and Android Gradle Plugin 9.4.1 using AGP's built-in Kotlin support. Measurement tests cover metric math, transport classification, radio and permission behavior, bounded probes, packet-loss fallback, cancellation, and partial results. Instrumentation tests check Android integration and the consuming app's merged permission contract.

The debug app was installed and opened on a Pixel 7 API 36 emulator. The visible UI smoke check covered the home screen, connection selection, room outline drawing, and room-scale entry. The home screen was also observed in landscape after allowing Android 16 to resize the activity. Emulator results verify app wiring and navigation only; they do not verify physical Wi-Fi or cellular measurements.

The current working-tree run passed **164 JVM tests and 9 API 36 emulator instrumentation tests** (173 total; zero failures, errors, or skips), built the debug APK, and completed lint with **zero errors, 20 warnings, and one hint** across the three modules. Warnings include API/dependency suggestions and Compose/resource recommendations. The project intentionally remains on the requested API 36 target. Gradle also reports an AGP project-import performance hint and deprecated features that will need review before Gradle 10.

## 6. Field-test results (pending)

The PRD calls for real-room testing in at least three different rooms. No physical-device readings were available while preparing this draft. Complete this table after testing; do not replace blanks with emulator values.

| Room | Phone / Android version | Connection | Measurement points | Notes on walls / layout |
| --- | --- | --- | --- | --- |
| Room 1 |  |  |  |  |
| Room 2 |  |  |  |  |
| Room 3 |  |  |  |  |

For each room, save the session and record which metrics were unavailable, whether ICMP or HTTP fallback was used, and whether the weak areas corresponded with the visible layout. Use the same endpoint and phone for comparisons where possible.

## 7. Conclusion

HeatNet combines room mapping with network measurements and local session history. Its networking layer separates HTTP application performance from ICMP reachability and preserves method labels and unavailable values. Its heatmaps make sparse readings easier to compare while acknowledging that IDW cannot represent walls or other physical obstructions. The software checks and emulator walkthrough support the implementation; real-room testing is still required to evaluate how well the maps correspond to users' actual indoor experience.

## References within the project

- `README.md` — build and run instructions, module responsibilities, and measurement defaults.
- `docs/member-1-viva.md` — detailed network measurement and Android permission notes.
- `docs/demo-script.md` — app walkthrough for the presentation.
- `docs/network-reading.html` — sequence diagram for a network reading.
