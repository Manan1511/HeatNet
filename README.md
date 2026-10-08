# HeatNet

HeatNet is a native Android app for measuring Wi-Fi or mobile-data quality at points around a room and visualizing the results as a heatmap. It targets Android 16 (API 36) and stores sessions on the device.

## What it does

- Create a Wi-Fi or mobile-data session, draw a room outline, and set its scale using a measured wall.
- Take network readings at room locations and view signal and internet-performance heatmaps.
- Review weak spots, revisit saved sessions, and compare two sessions using shared room areas.
- Use the home-screen **Demo mode** switch to explore the mapping flow with simulated readings. Demo readings are marked as simulated in saved data and comparison warnings.

Heatmap colors between measured points are inverse-distance-weighted estimates. The interpolation does not model walls, doors, or furniture; the app fades estimated areas to distinguish them from nearby measurements.

## Requirements

- Android Studio with Android SDK Platform 36 installed.
- Android 16 / API 36 device or emulator. The app's `minSdk` is 36.
- Gradle JDK 17. The Android modules compile against Java/JVM 17.

## Open and run

1. Open this repository's root folder in Android Studio and allow Gradle sync to finish.
2. In **Tools → SDK Manager**, install Android SDK Platform 36 if it is missing.
3. Start an Android 16 / API 36 emulator from **Tools → Device Manager**, or connect a compatible device with USB debugging enabled.
4. Select the `app` run configuration and launch it, or install the debug build from PowerShell:

   ```powershell
   .\gradlew.bat :app:installDebug
   ```

The Gradle wrapper is included, so use `gradlew.bat` on Windows rather than installing Gradle separately.

## Build and checks

Run these from the repository root in PowerShell:

```powershell
# Build the debug APK
.\gradlew.bat :app:assembleDebug

# Run local unit tests
.\gradlew.bat :measurement:testDebugUnitTest :data:testDebugUnitTest :app:testDebugUnitTest

# Run Android instrumentation tests (requires a running API 36 device/emulator)
.\gradlew.bat :measurement:connectedDebugAndroidTest :app:connectedDebugAndroidTest

# Run Android lint
.\gradlew.bat :measurement:lintDebug :data:lintDebug :app:lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

| Module | Responsibility |
| --- | --- |
| `:app` | Jetpack Compose screens, room drawing and scaling, map interaction, heatmap rendering, and app wiring. |
| `:measurement` | Android network and Wi-Fi radio access, permission checks, and latency, throughput, ICMP, and HTTP fallback probes. |
| `:data` | Room database, session/readings repository, data mapping, weak-spot analysis, and session comparison. |

The network-reading path is shown in the [HeatNet network reading diagram](docs/network-reading.html). Its editable diagram description is in [`docs/network-reading.json`](docs/network-reading.json).

## How measurements work

Each reading captures the active Android `Network` and whether it uses Wi-Fi or mobile data. The app then runs the probes over that captured network so a change in the device's default route does not silently mix transports within a reading.

Wi-Fi radio details are read through a fresh Android Wi-Fi callback. Android requires precise location permission to expose these fields; HeatNet requests that permission to read Wi-Fi information and does not use it to locate the device. If permission or radio data is unavailable, those fields may be missing.

Internet measurements use replaceable defaults in `measurement/src/main/java/com/heatnet/measurement/model/MeasurementConfig.kt`:

| Measurement | Default method / target |
| --- | --- |
| Latency | HTTPS request to Cloudflare's `speed.cloudflare.com` test endpoint |
| Download and upload | HTTPS byte transfers to Cloudflare's public speed-test endpoints |
| Packet loss | ICMP echo probes to `speed.cloudflare.com` when available |
| Fallback | HTTP request-failure rate when ICMP is unavailable or returns no replies and HTTP connectivity was established |

HTTP fallback is an application-level probe-failure rate, not a count of raw network packets lost. The app labels the measurement method accordingly. Throughput reflects end-to-end internet goodput, which depends on the test endpoint and the wider network; it is not a direct measurement of Wi-Fi link capacity. The default measurement deadline is 10 seconds, with a 4 MiB transfer budget per direction.

Wi-Fi sessions need location permission for Android's Wi-Fi radio details. The app also declares network access/state permissions and `READ_BASIC_PHONE_STATE` for cellular network information. Sessions and readings are stored locally with Room; the project has no account or cloud-sync backend.
