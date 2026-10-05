# HeatNet Member 1 Measurement Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build HeatNet's API 36 Kotlin measurement library and its test host so a Wi-Fi or mobile reading returns bounded network metrics, available radio data, and honest failure details.

**Architecture:** Create a `:measurement` Android library with pure metric calculations, Android connectivity/radio adapters, network-bound OkHttp probes, and a coroutine-based `MeasurementEngine`. Add a minimal `:app` host solely to set target SDK 36 and compile/test the public library contract; it contains no mapping UI or persistence.

**Tech Stack:** Kotlin 2.3.21, Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17, Android API 36, coroutines 1.11.0, OkHttp 5.4.0, JUnit 4, AndroidX Test.

**Spec:** `docs/superpowers/specs/2026-10-06-heatnet-member-1-measurement-design.md`

## Global Constraints

- `minSdk`, `targetSdk`, and `compileSdk` are all 36.
- Add a reusable `:measurement` library; no Compose screens or database in that module.
- `MeasurementResult` is transient and carries no session ID or coordinates.
- Cloudflare endpoints are configurable, initially `https://speed.cloudflare.com/__down?bytes=0`, `/__down`, and `/__up`.
- Keep one immutable `MeasurementConfig` for all readings in a session.
- Run 2 parallel streams per direction, with a 4 MiB per-direction and 8 MiB per-reading payload limit and a 10-second overall deadline.
- Take ten HTTP latency samples and twenty ICMP probes; use twenty HTTP probes as a visibly labeled fallback only when HTTP connectivity is available.
- Wi-Fi requires `ACCESS_FINE_LOCATION`; denial blocks Wi-Fi readings, and redacted fields become null.
- The measurement library reports permission state but never opens a dialog; the consuming Activity requests fine location and shows its rationale.
- Declare `READ_BASIC_PHONE_STATE`; do not request runtime `READ_PHONE_STATE` or privileged precise-phone-state access.
- Bind HTTP and DNS to the captured Android `Network`; never switch a reading to a new default route.
- Preserve completed measurements on partial failure; caller cancellation propagates and cleans up child work.
- Publish the `Reading` integration contract: persist `packetLossMethod` and allow `wifiBand` value `"6"`. There is no `Reading` model in this checkout yet, so do not invent a persistence model; document the handoff for Members 2 and 3.
- Do not send coordinates, session IDs, account identifiers, or cookies to the public test endpoint.
- Verify with API 36 unit/instrumented tests and a physical Android 16 phone when available; include `docs/member-1-viva.md`.

## Review Focus

- VPN, Ethernet, no supported transport, or ambiguous Wi-Fi/mobile capabilities must return `BLOCKED` without probes; pin this with `TransportClassifierTest.rejectsUnsupportedOrAmbiguousTransports` in Task 3.
- Denied Wi-Fi location or Android-redacted `WifiInfo` values must block or become null as specified, while mobile still proceeds; pin this with `PermissionGateTest.wifiDeniedBlocksButMobileDoesNot` and `WifiRadioReaderTest.redactedFieldsBecomeNull` in Task 4.
- Missing modem data or a mismatch between active radio technology and available signal components must yield null plus an issue, not a guessed dBm value; pin this with `CellularRadioReaderTest.unavailableTechnologyKeepsSignalNull` in Task 4.
- ICMP launch failure, malformed output, no replies, or no usable HTTP route must never be reported as false ICMP loss; pin this with `PacketLossProbeTest.fallbackRequiresReachableHttp` and `PacketLossProbeTest.offlineLossIsUnavailable` in Task 6.
- A route change, the byte cap, the ten-second deadline, or caller cancellation must not leak calls/processes or silently use another route; pin this with `ThroughputProbeTest.respectsSharedDirectionBudget` in Task 5 and `MeasurementEngineTest.networkLossReturnsPartialAndCallerCancellationCleansUp` in Task 7.

## File Map

- `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradle/wrapper/*`, wrapper scripts, and `.gitignore`: reproducible Android 16 Gradle build and shared pinned dependency versions.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, and `app/src/androidTest/java/com/heatnet/MeasurementManifestIntegrationTest.kt`: minimal application host that consumes `:measurement`, declares target SDK 36, and verifies the merged library contract; no user-facing UI.
- `measurement/build.gradle.kts` and `measurement/src/main/AndroidManifest.xml`: API 36 Android library configuration and merged measurement permissions.
- `measurement/src/main/java/com/heatnet/measurement/model/*`: stable measurement, progress, issue, configuration, and result types.
- `measurement/src/main/java/com/heatnet/measurement/math/*`: deterministic throughput, latency, jitter, loss, and Wi-Fi frequency calculations.
- `measurement/src/main/java/com/heatnet/measurement/network/*`: transport classification and active-network state flow.
- `measurement/src/main/java/com/heatnet/measurement/permissions/*` and `radio/*`: permission reporting and Wi-Fi/cellular snapshots.
- `measurement/src/main/java/com/heatnet/measurement/probe/*`: bound HTTP probes, ICMP process/parsing, and HTTP-loss fallback.
- `measurement/src/main/java/com/heatnet/measurement/engine/*`: orchestration, progress, partial results, route-change handling, and cleanup.
- Matching `measurement/src/test/java/...` and `measurement/src/androidTest/java/...` files: pure, fake-provider, and Android platform tests.
- `docs/member-1-viva.md`: concise explanation of each metric and its limits, plus the `MeasurementResult` to future `Reading` mapping and required shared-contract additions for Members 2 and 3.

## Tasks

### Task 1: Bootstrap the API 36 host and measurement library

**Files:** Create the root Gradle files and wrapper above; create `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `measurement/build.gradle.kts`, and `measurement/src/main/AndroidManifest.xml`.

**Interfaces:** The app module depends on `project(":measurement")`; the library exports package `com.heatnet.measurement`. In Android Studio's Gradle settings, download/select JDK 17 for this Gradle 8.13 build; the installed bundled JBR is Java 25 and the external terminal currently finds Java 8. Do not commit a machine-specific JDK path or `local.properties`.

- [x] **Step 1: Pin the build toolchain and modules** to AGP `8.13.2`, Gradle wrapper `8.13`, Kotlin Gradle plugin `2.3.21`, and Java/Kotlin target 17. Add `:app` and `:measurement`; set `compileSdk = 36`, `minSdk = 36`, and app `targetSdk = 36`. Put dependency versions in `gradle/libs.versions.toml`; include coroutines `1.11.0`, OkHttp and MockWebServer3 `5.4.0`, JUnit `4.13.2`, and AndroidX Test runner/rules `1.7.0`, core `1.7.0`, and ext-junit `1.3.0`.
- [x] **Step 2: Declare the measurement permissions** in the library manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `ACCESS_FINE_LOCATION`, and `READ_BASIC_PHONE_STATE`. Set `testInstrumentationRunner` to `androidx.test.runner.AndroidJUnitRunner` for the host and library.
- [x] **Step 3: Ignore generated/local files** in `.gitignore`: `.gradle/`, `.idea/`, `.kotlin/`, `local.properties`, and all module `build/` directories.
- [x] **Step 4: Set up the Gradle runtime and wrapper.** In Android Studio, use **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** with JDK 17. This checkout sets the IDE macro `GRADLE_LOCAL_JAVA_HOME` to the local JDK 17 in ignored `.gradle/config.properties`; the SDK path also stays in ignored `local.properties`. With JDK 17, the official Gradle 8.13 binary distribution generated the wrapper. Do not commit the downloaded distribution or machine-specific JDK/SDK paths.
- [x] **Step 5: Build both empty modules** with `.\gradlew.bat :app:assembleDebug :measurement:assembleDebug`.

Run the wrapper's `--version` and module assembly commands from Android Studio's Terminal using **Run highlighted command using the IDE**, or from a regular terminal with `JAVA_HOME` pointing to the downloaded JDK 17.
Expected: Gradle 8.13 reports JDK 17 and both debug artifacts assemble with SDK 36.

- [x] **Step 6: Commit** as `build: bootstrap HeatNet measurement project`.

### Task 2: Define the contract and pure metric calculations

**Files:** Create `measurement/src/main/java/com/heatnet/measurement/model/MeasurementModels.kt`, `MeasurementConfig.kt`, `math/MetricCalculations.kt`, and `math/WifiFrequencyMapper.kt`; test in matching `measurement/src/test/java/...` files.

**Interfaces:** Export `ConnectionType`, `MeasurementStatus`, `PacketLossMethod`, `MeasurementIssue`, `MeasurementProgress`, `MeasurementConfig`, and `MeasurementResult`. `MeasurementProgress` uses stages `VERIFYING_NETWORK`, `CAPTURING_RADIO`, `DOWNLOAD`, `UPLOAD`, `LATENCY`, `PACKET_LOSS`, and `COMPLETE`. `MeasurementResult` has the approved nullable metric/radio fields, `expectedConnectionType`, `observedConnectionType`, `measuredAtEpochMillis`, `status`, `packetLossMethod`, and `issues`. Task 4 defines `RadioSnapshot` as the radio readers' output.

Use `Float?` for throughput, latency, jitter, and loss fields to match the shared `Reading`; use `Int?` for dBm, link speed, and channel, `String?` for band/BSSID/network type, `Long` for the epoch timestamp, and `List<MeasurementIssue>` for issues. `MeasurementConfig` is immutable and defaults to the three approved Cloudflare URLs, `icmpHost = "speed.cloudflare.com"`, two streams, 4 MiB per direction, 10,000 ms overall timeout, ten latency samples, twenty ICMP probes, and twenty HTTP fallback probes. `MeasurementIssue` is `(code: IssueCode, detail: String?)`; codes are `PERMISSION_DENIED`, `UNSUPPORTED_TRANSPORT`, `TRANSPORT_MISMATCH`, `NETWORK_CHANGED`, `NO_INTERNET`, `TIMEOUT`, `ENDPOINT_FAILURE`, `ICMP_UNAVAILABLE`, `RADIO_UNAVAILABLE`, and `FIELD_REDACTED`.

- [x] **Step 1: Write failing tests** named `calculatesPayloadMbps`, `returnsNullForNonPositiveDuration`, `computesMeanAndSampleJitter`, `returnsNullWithoutSuccessfulLatencySamples`, `usesAttemptedProbeDenominator`, and `maps24FiveAndSixGhzChannels`. Assert 1,000,000 bytes over 1 second is 8 Mbps; latencies `[10, 12, 14]` yield mean 12 ms and sample jitter 2 ms; 1 failure of 5 attempts is 20%; zero attempts is null; 2412 MHz maps to band `"2.4"`/channel 1, 5180 to `"5"`/36, and 5955 to `"6"`/1.
- [x] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm those tests fail because the calculation types are not implemented.
- [x] **Step 3: Implement** the data types and pure calculations. Use monotonic nanoseconds for durations; use only successful latency samples; return null for undefined values; map unknown frequencies to null.
- [x] **Step 4: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm the named tests pass.
- [x] **Step 5: Commit** as `feat(measurement): define result contract and metric math`.

### Task 3: Classify and monitor the active network

**Files:** Create `network/ConnectionSnapshot.kt`, `TransportClassifier.kt`, `ConnectionMonitor.kt`, and matching classifier/monitor tests.

**Interfaces:** `ConnectionSnapshot(network: Network?, connectionType: ConnectionType?, hasValidatedInternet: Boolean)`; `ConnectionMonitor.state: StateFlow<ConnectionSnapshot>`, `start()`, and `stop()`. `TransportClassifier.classify(transports: Set<NetworkTransport>): ConnectionType?`, where `NetworkTransport` has `WIFI`, `CELLULAR`, `VPN`, `ETHERNET`, and `OTHER`; the Android adapter maps `NetworkCapabilities` to this pure set.

- [ ] **Step 1: Write failing tests** including `TransportClassifierTest.rejectsUnsupportedOrAmbiguousTransports` for Wi-Fi/mobile classification and unsupported VPN/Ethernet/no transport/both Wi-Fi and cellular, plus validated versus unvalidated state, lost default network, and idempotent callback registration/unregistration.
- [ ] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm classifier/monitor tests fail.
- [ ] **Step 3: Implement** a pure classifier and `ConnectivityManager.registerDefaultNetworkCallback` adapter. Construct the callback with `FLAG_INCLUDE_LOCATION_INFO`, publish the default network and `NET_CAPABILITY_VALIDATED` via `StateFlow`, and ignore stale callbacks from replaced networks.
- [ ] **Step 4: Run** `.\gradlew.bat :measurement:testDebugUnitTest :measurement:connectedDebugAndroidTest`; confirm classification and callback lifecycle tests pass on the API 36 AVD.
- [ ] **Step 5: Commit** as `feat(measurement): monitor active network state`.

### Task 4: Read Wi-Fi and cellular radio data with the approved permissions

**Files:** Create `permissions/PermissionGate.kt`, `radio/WifiRadioReader.kt`, `CellularRadioReader.kt`, `RadioSnapshot.kt`, fakeable platform interfaces, and matching local/instrumented tests.

**Interfaces:** `PermissionGate.check(type): PermissionOutcome`; `WifiRadioReader.read(network): RadioSnapshot`; `CellularRadioReader.read(): RadioSnapshot`. Wi-Fi snapshot carries RSSI, receive PHY link speed, band, channel, and BSSID; mobile snapshot carries dBm and network type.

- [ ] **Step 1: Write failing tests** including `PermissionGateTest.wifiDeniedBlocksButMobileDoesNot`, `WifiRadioReaderTest.redactedFieldsBecomeNull`, and `CellularRadioReaderTest.unavailableTechnologyKeepsSignalNull`. Also cover Wi-Fi-only fine-location gating, masked BSSID and invalid RSSI/link/frequency becoming null, 2.4/5/6 GHz mapping, active mobile data subscription selection, and LTE/NR/NR-NSA labels.
- [ ] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm these tests fail.
- [ ] **Step 3: Implement** permission checks without launching dialogs. Read Wi-Fi transport info from the captured `Network`; turn Android sentinels/redaction into null. Read cellular signal from the active data subscription's `TelephonyManager`, pair dBm only with the active technology, read data type with `READ_BASIC_PHONE_STATE`, and use a short-lived `TelephonyCallback.DisplayInfoListener` to read the NR NSA override when base type is LTE. Bound the callback wait and unregister it in `finally`; return null on absent telephony feature or unavailable modem data.
- [ ] **Step 4: Add API 36 instrumentation checks** for merged permissions and platform adapters, then run `.\gradlew.bat :measurement:testDebugUnitTest :measurement:connectedDebugAndroidTest`.
- [ ] **Step 5: Commit** as `feat(measurement): capture wifi and cellular radio data`.

### Task 5: Implement captured-network latency and throughput probes

**Files:** Create `probe/NetworkBoundHttpClientFactory.kt`, `HttpCallAwait.kt`, `LatencyProbe.kt`, `ThroughputProbe.kt`, probe result types, and matching unit tests using MockWebServer3/fakes.

**Interfaces:** `LatencyProbe.measure(network, config, deadlineNanos): LatencyMeasurement`; `ThroughputProbe.measure(network, direction, byteBudget, deadlineNanos): ThroughputMeasurement`. The network client receives the exact captured `Network` and cannot use the default route.

- [ ] **Step 1: Write failing tests** including `ThroughputProbeTest.respectsSharedDirectionBudget` and `ThroughputProbeTest.budgetTruncationIsPartial`; cover ten zero-byte latency GETs, mean/sample jitter from successes, two concurrent streams in each direction, aggregate direction budget exactly 4 MiB, reading budget no more than 8 MiB, partial body accounting, HTTP failure issues, and request cancellation closing the response.
- [ ] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm the probe tests fail.
- [ ] **Step 3: Implement** an OkHttp client using `Network.getSocketFactory()` and network-specific `Network.getAllByName()` DNS; adapt calls to cancellable suspend operations. Stream and count payload bytes, cap each direction at 4 MiB across both streams, issue exactly ten latency requests, and calculate Mbps from payload bytes and monotonic elapsed time. Keep configured Cloudflare URLs replaceable.
- [ ] **Step 4: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm byte, latency, error, and cancellation tests pass.
- [ ] **Step 5: Commit** as `feat(measurement): measure bounded latency and throughput`.

### Task 6: Implement ICMP parsing and the labeled HTTP loss fallback

**Files:** Create `probe/PingProcess.kt`, `PingSummaryParser.kt`, `IcmpProbe.kt`, `HttpLossProbe.kt`, `PacketLossProbe.kt`, and matching unit tests.

**Interfaces:** `PingProcess.run(host, count, intervalMillis, timeoutMillis): PingExecution`; `PacketLossProbe.measure(network, config, httpReachable, deadlineNanos): PacketLossMeasurement`. The result always identifies `ICMP`, `HTTP_PROBE_FAILURES`, or `UNAVAILABLE`.

- [ ] **Step 1: Write failing tests** including `PacketLossProbeTest.fallbackRequiresReachableHttp` and `PacketLossProbeTest.offlineLossIsUnavailable`; cover standard Android ping summaries, malformed/empty output, executable failure, zero ICMP replies, twenty HTTP attempts with started-attempt denominator, unstarted attempts excluded, and HTTP success/failure outcomes.
- [ ] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm the parser/probe tests fail.
- [ ] **Step 3: Implement** `ping` process execution for twenty echoes to the configured Cloudflare host, parse transmitted/received counts, and stop the process on cancellation/deadline. If ICMP cannot run or receives no replies and an earlier HTTP probe succeeded, make up to twenty small HTTP probes; otherwise return unavailable loss rather than calling total offline failure packet loss.
- [ ] **Step 4: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm ICMP, fallback, offline, and cleanup tests pass.
- [ ] **Step 5: Commit** as `feat(measurement): report ICMP and HTTP probe loss`.

### Task 7: Orchestrate one reading and preserve partial results

**Files:** Create `engine/MeasurementEngine.kt`, `MeasurementDependencies.kt`, and `MeasurementEngineTest.kt` with fake monitor, radio, clock, HTTP, and ping providers.

**Interfaces:** `suspend fun MeasurementEngine.takeMeasurement(expectedType: ConnectionType, onProgress: (MeasurementProgress) -> Unit = {}): MeasurementResult`. The engine consumes the monitor, gate, radio readers, probes, config, wall clock, and monotonic clock from earlier tasks.

- [ ] **Step 1: Write failing tests** including `MeasurementEngineTest.networkLossReturnsPartialAndCallerCancellationCleansUp`. Cover successful result/progress order `VERIFYING_NETWORK → CAPTURING_RADIO → DOWNLOAD → UPLOAD → LATENCY → PACKET_LOSS → COMPLETE`; unsupported or mismatched type returning `BLOCKED` without probes; Wi-Fi denial returning `BLOCKED`; unvalidated/offline internet retaining radio data and null network metrics; single probe failure preserving other metrics; network loss canceling children and returning `PARTIAL`; ten-second deadline; and caller cancellation propagating while cleaning up HTTP calls, ping process, telephony callback, and flow collection without stopping the shared `ConnectionMonitor`.
- [ ] **Step 2: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm orchestration tests fail.
- [ ] **Step 3: Implement** snapshot capture, expected-type validation, permission/radio reads, and probe stages under one monotonic ten-second deadline. Use the captured network for every probe and the immutable config supplied to the engine. If the captured network is lost or replaced by a new default network, cancel unfinished probes and record a partial result; propagate caller cancellation. Return `BLOCKED` for unsupported or mismatched transport, a missing captured network, or denied Wi-Fi location permission; return `COMPLETE` only when all planned probes finish with no issues; otherwise return `PARTIAL`, retaining every completed metric. An unvalidated internet state is informational and must not block probes.
- [ ] **Step 4: Run** `.\gradlew.bat :measurement:testDebugUnitTest` and confirm all orchestration and review-focus tests pass.
- [ ] **Step 5: Commit** as `feat(measurement): orchestrate bounded readings`.

### Task 8: Verify app consumption and write the viva guide

**Files:** Create `app/src/androidTest/java/com/heatnet/MeasurementManifestIntegrationTest.kt` and `docs/member-1-viva.md`; adjust only the new host/library test configuration as required.

- [ ] **Step 1: Write the integration test** `libraryManifestDeclaresMeasurementPermissions` to verify the consuming app sees the merged network, Wi-Fi, location, and basic phone-state permissions, and `appConsumesMeasurementLibrary` to compile against `MeasurementEngine`/`MeasurementResult` with `packetLossMethod = HTTP_PROBE_FAILURES` and `wifiBand = "6"`.
- [ ] **Step 2: Run** `.\gradlew.bat :app:connectedDebugAndroidTest`; expect the tests to pass because Tasks 1, 2, and 7 already wired the consuming app and public API. If they fail, fix manifest merging or public API visibility before proceeding.
- [ ] **Step 3: Write** the viva guide covering throughput and parallel streams, HTTP latency and jitter, ICMP versus HTTP failure percentage, RSSI, Wi-Fi PHY rate versus internet throughput, bands/channels, mobile types, permissions, public-edge variability, the `MeasurementResult` to future `Reading` mapping, and the limits of emulator/physical readings.
- [ ] **Step 4: Run final checks** with `.\gradlew.bat :app:assembleDebug :measurement:testDebugUnitTest :app:connectedDebugAndroidTest`. Start the API 36 AVD first. On a physical Android 16 phone, manually record one Wi-Fi and one mobile reading; note unavailable radio fields instead of treating emulator values as real.
- [ ] **Step 5: Review** `git diff --check`, confirm the PRD is untouched and unstaged, and commit as `docs: add member one measurement viva guide`.

## Toolchain References

- [AGP 8.13 release notes and API 36/JDK 17 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Kotlin Gradle plugin compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Kotlin compiler options DSL](https://kotlinlang.org/docs/gradle-compiler-options.html)
- [Android Gradle JDK selection](https://developer.android.com/build/jdks)
- [Android `Network` socket factory and DNS](https://developer.android.com/reference/android/net/Network)
- [Android callback location information](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback)
- [AndroidX Test dependency versions](https://developer.android.com/jetpack/androidx/releases/test)
- [OkHttp 5.4.0 artifact](https://central.sonatype.com/artifact/com.squareup.okhttp3/okhttp/5.4.0)
