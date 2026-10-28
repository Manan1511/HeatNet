# PRD: HeatNet Network Heatmap App (Android)

App name: **HeatNet**
Course: Computer Networks (BACSE203), VIT Vellore
Team: 3 people
Status: Implemented; Android 16 automated checks pass; physical-device field validation remains. The shipped app records real measurements only and has no simulated-reading mode.
Deadline: Not provided

## Current project status (2026-10-28)

- The app targets Android 16 / API 36, with minimum SDK 36. The team selected Cloudflare's configurable public speed-test endpoints as the initial HTTP targets.
- The AGP built-in Kotlin migration builds successfully with Gradle 9.8.0 and AGP 9.4.1.
- Current working-tree verification (2026-10-28): 164 JVM tests and 9 API 36 emulator instrumentation tests passed with no failures or skips; the debug APK builds, and lint reports zero errors, 20 warnings, and one hint.
- An emulator walkthrough reached the connection picker, room-outline drawing, and room-scale setup. The home screen also resized in landscape. This does not verify real network measurements or radio values.
- Still to do: run the app on the team's physical Android 16 phones and test in at least three real rooms; record which readings are unavailable and tune heatmap/weak-spot thresholds from those results. The report draft is `docs/heatnet-project-report-draft.md` and deliberately has no invented field data.
- The team deadline and prior Android experience were not supplied and remain unknown.

---

## 0. How to use this document with an AI

Paste this whole file at the start of a session, then say what you are working on (for example "I'm Member 2, help me build the heatmap renderer"). The AI should treat sections 3 to 9 as decisions already made, not suggestions. If you want to change one, say so explicitly and update this file afterwards.

**Quick summary for the AI:** A native Kotlin Android app. The user outlines a room, walks around, taps their position on the outline, and the app runs a short network test (speed, latency, ICMP echo loss or labeled HTTP request-failure rate) plus reads available radio data. Each real reading becomes a dot. The app fills the gaps with inverse distance weighting and draws a heatmap that fades out far from any real reading. Sessions are saved locally and two sessions of the same connection type can be compared side by side. The app flags weak spots automatically. The shipped app has no simulated-reading mode.

---

## 1. Problem

People know their Wi-Fi or mobile data is bad in some parts of a room but not where, how bad, or whether a change (moving the router, switching a channel) helped. Existing tools (NetSpot, Ekahau, various Android Wi-Fi analyzers) are either aimed at IT professionals, need floor plan uploads, or only show signal strength rather than the speed, latency and loss a user actually feels.

## 2. Goals and non-goals

### Goals
1. Let any user map network quality across one room in a few minutes.
2. Show **real measurements** (speed, latency, jitter, packet loss, signal) rather than just signal bars.
3. Let the user check whether a change helped, by comparing two maps.
4. Be honest about uncertainty: show clearly which parts of the map are measured and which are estimated.
5. Demonstrate solid Computer Networks understanding (throughput, latency, jitter, packet loss, TCP behaviour, Wi-Fi bands and channels, cellular network types).

### Non-goals (decided, do not build)
- User accounts, login, cloud sync, or any backend of our own
- PDF export (screenshots are enough)
- Router placement suggestions (the app cannot know where the router is or how walls block signal; advice would be a guess)
- Automatic indoor positioning (step counting, sensor tracking). It drifts and cannot be defended
- Floor plan image uploads
- Multi-room or multi-floor maps
- Mixing Wi-Fi and mobile data readings in one map
- An in-app simulated/demo-reading mode; old saved simulated readings remain visibly marked for data compatibility
- iOS or web versions

---

## 3. Users

"General" audience, with a typical user being a student or home user checking a room or hostel for dead spots. No technical knowledge assumed. The UI must use plain words ("Weak signal in the north-east corner") and show raw numbers only in a detail view.

---

## 4. Scope: features

### 4.1 Must have (core)
| ID | Feature | Notes |
|----|---------|-------|
| F1 | Room outline | User draws the room by tapping corners to form a polygon, then enters the length of one wall in metres to set the scale |
| F2 | Connection type detection | App detects Wi-Fi or mobile data at session start and locks the session to it |
| F3 | Take a reading | User taps their position on the outline, presses Measure, a 5 to 10 second test runs, a dot appears |
| F4 | Heatmap | Gaps filled with inverse distance weighting, colour fades with distance from the nearest real reading |
| F5 | Metric switcher | Toggle the heatmap between metrics (see section 6) |
| F6 | Save sessions | Store sessions locally on the phone |

### 4.2 Should have (build in this order)
| ID | Feature | Notes |
|----|---------|-------|
| F7 | Compare two sessions | Side-by-side maps, same connection type only. **Highest priority after the core** |
| F8 | Weak-spot flags | Marks areas below thresholds and writes one plain sentence per problem area. **First thing to drop if time runs out** |

### 4.3 Could have (only if everything above is done and tested)
- Suggested grid of spots to visit, so the user knows where to take readings
- Averaging two tests per spot to reduce noise

---

## 5. User flow

1. Open app, tap **New session**.
2. App detects connection type (Wi-Fi or mobile data) and shows it. Session is locked to that type. Continuing with Wi-Fi requests precise location permission for Wi-Fi radio details; approval proceeds to room setup. Mobile sessions do not request this permission.
3. User draws the room outline by tapping corners, closes the shape, enters one wall length.
4. User walks to a spot, taps that spot on the outline, presses **Measure**.
5. App runs the test (5 to 10 seconds), places a coloured dot, updates the heatmap.
6. Repeat for roughly 10 to 15 spots.
7. User taps **Finish**. Session is saved. Weak-spot summary is shown (F8).
8. Later: user opens two saved sessions of the same type and taps **Compare**.

**Edge cases to handle:**
- Connection type changes mid-session: show a warning ("You've switched to mobile data. Readings from here won't match earlier ones.") and let the user end the session or return to the original connection.
- No internet during a test: save the reading with failed flags rather than crashing; mark it clearly.
- Wi-Fi location permission denied: explain that Android requires it for Wi-Fi radio details, provide an app-settings path after denial, and block Wi-Fi sessions until precise permission is granted. Mobile sessions remain available without it.
- Fewer than 3 readings: do not draw a heatmap, ask for more points.

---

## 6. Measurements: what each reading records

Every reading is one **Reading** record (data model in section 8).

### 6.1 Network test (both connection types)
| Metric | How it is measured |
|--------|--------------------|
| Download speed (Mbps) | Download up to 4 MiB from the configured public HTTPS endpoint using two parallel HTTP streams; time the combined payload |
| Upload speed (Mbps) | Upload up to 4 MiB total to the configured public HTTPS endpoint using two parallel HTTP streams |
| Latency (ms) | Time up to 10 zero-byte HTTPS requests and record the average of successful requests |
| Jitter (ms) | Sample standard deviation of successful latency request timings |
| Packet loss (%) | Try up to 20 ICMP echo probes and count missing replies. If ICMP cannot run or gets no replies, use up to 20 HTTP probes only if an earlier HTTP request succeeded; label the result as an application-level HTTP request-failure rate, not packet loss |

One reading has a shared ten-second deadline. Two streams per direction can use more of a fast link than one stream, but the result is still public-endpoint internet goodput, not Wi-Fi capacity.
Known limitation: a public server means results depend on the internet beyond the user's Wi-Fi. Use the same server every time and keep tests short.

### 6.2 Wi-Fi sessions: radio data
- Signal strength (RSSI, dBm)
- Link speed (Mbps)
- Frequency band (2.4, 5, or 6 GHz) and channel
- BSSID of the connected access point

### 6.3 Mobile data sessions: radio data
- Signal strength (dBm)
- Network type (for example 4G LTE or 5G)

**Why radio data is recorded next to the speed test:** on Wi-Fi, the internet plan can cap the speed test result, making every corner look the same. RSSI and link speed change with location, so the map still shows variation.

### 6.4 Heatmap metrics (switcher, F5)
Wi-Fi: download, upload, latency, packet loss, signal strength, link speed.
Mobile data: download, upload, latency, packet loss, signal strength.

---

## 7. Heatmap logic

- **Interpolation:** inverse distance weighting (IDW). The value at any point is a weighted average of nearby readings, closer readings weighted more. Power parameter starts at 2 and is tunable.
- **Fading:** colour opacity depends on the distance to the nearest real reading. Bright means "measured near here", washed out means "estimated". The distance at which it fully fades is a tunable constant, relative to room size.
- **Known weakness (state this honestly in the report):** IDW does not know about walls or furniture. Signal drops sharply through obstacles, but IDW blends smoothly across them, so the map can look cleaner than reality.
- **Colour scale:** consistent per metric, higher is better for speed and signal, lower is better for latency, jitter and loss. Include a legend.
- Real readings are always drawn as visible dots on top of the heatmap.

---

## 8. Data model

```kotlin
data class Session(
    val id: Long,
    val name: String,
    val createdAt: Long,           // epoch millis
    val connectionType: ConnectionType,   // WIFI or MOBILE
    val outline: List<Point>,      // room polygon corners, in metres
    val widthMetres: Float,        // bounding box, derived from outline
    val heightMetres: Float
)

enum class ConnectionType { WIFI, MOBILE }
enum class MeasurementStatus { COMPLETE, PARTIAL, BLOCKED }
enum class PacketLossMethod { ICMP, HTTP_PROBE_FAILURES, UNAVAILABLE }

data class Reading(
    val id: Long,
    val sessionId: Long,
    val x: Float,                  // metres, inside outline
    val y: Float,
    val timestamp: Long,
    val status: MeasurementStatus,
    val downloadMbps: Float?,      // null if the test failed
    val uploadMbps: Float?,
    val latencyMs: Float?,
    val jitterMs: Float?,
    val packetLossPct: Float?,
    val packetLossMethod: PacketLossMethod,
    val signalDbm: Int?,           // Wi-Fi RSSI or cellular signal
    val linkSpeedMbps: Int?,       // Wi-Fi only
    val wifiBand: String?,         // "2.4", "5", or "6", Wi-Fi only
    val wifiChannel: Int?,         // Wi-Fi only
    val bssid: String?,            // Wi-Fi only
    val networkType: String?,      // mobile only, e.g. "LTE", "NR"
    val isDemo: Boolean = false    // compatibility marker for simulated readings saved by old versions; new readings are false
)
```

All coordinates are in **metres** inside the outline, not pixels. The UI converts to pixels for drawing. This keeps compare mode working across different screen sizes.

---

## 9. Tech stack

- **Language:** Kotlin, native Android
- **UI:** Jetpack Compose, Canvas for the outline and heatmap
- **Storage:** Room (local SQLite)
- **Async:** Kotlin coroutines
- **HTTP tests:** OkHttp
- **Radio data:** location-aware `ConnectivityManager.NetworkCallback` and `WifiInfo` for active Wi-Fi; `TelephonyManager` for cellular
- **Packet loss:** system `ping` via process execution, with an explicitly labeled HTTP request-failure fallback
- **Permissions:** precise location for Wi-Fi metadata (requested when continuing into a Wi-Fi session); internet/network access and `READ_BASIC_PHONE_STATE` are install-time permissions
- **SDK:** `minSdk`, `targetSdk`, and `compileSdk` are 36 (Android 16)

Reason for native Kotlin over React Native: reading signal data needs native APIs, and a bridge adds work for no benefit.

---

## 10. Weak-spot rules (F8)

Starting thresholds, to be tuned after real-room testing:

| Metric | Flag as weak when |
|--------|-------------------|
| Wi-Fi signal | worse than about -75 dBm |
| Latency | above about 100 ms |
| Packet loss | above about 2 % |
| Download speed | below about 25 % of the best reading in the session |
| Mobile signal | threshold to be set after testing on the team's phones |

Output: one plain sentence per weak area (for example "Weak signal in the north-east corner"), based on which quadrant of the room the weak readings fall in.

---

## 11. Work split (3 people)

The three parts connect through the data model in section 8. **Agree on that data model on day one and do not change it without telling the others.** That is what lets everyone work in parallel.

### Member 1: Measurement engine (the networking part)
Owns everything that produces a `Reading`.
- Connection type detection and session locking (F2), including the mid-session change warning
- Download and upload tests with parallel connections
- Latency, jitter, and packet loss tests (ping parsing, HTTP fallback)
- Reading Wi-Fi data (RSSI, link speed, band, channel, BSSID) and cellular data (dBm, network type)
- Permission handling
- Returns one transient `MeasurementResult` from `suspend fun takeMeasurement(sessionType)`; the data layer adds session and tapped-position fields to create a saved `Reading`
- Written explanation of how each metric is measured (this is the viva-critical part)

### Member 2: Map and UI
Owns everything the user sees while mapping.
- Room outline drawing, scale entry, validation (F1)
- Tap-to-place position and the Measure button flow (F3)
- IDW heatmap rendering with distance fade (F4)
- Metric switcher and legend (F5)
- Dots overlay, basic navigation between screens
- Used fake `Reading` data during development so it did not wait for Member 1; no fake-reading source ships in the app

### Member 3: Data, compare, and quality
Owns storage, the extra features, and testing.
- Room database for sessions and readings (F6)
- Session list screen, save and load, delete
- Compare view with two maps side by side (F7)
- Weak-spot detection and summary text (F8)
- Real-room testing on at least 3 different rooms, tuning the thresholds and IDW constants
- Final report, demo script, viva preparation

### Shared by everyone
- Section 8 data model agreed on day one
- Weekly 20-minute sync to merge and test together
- Everyone must be able to answer questions about the full app in the viva, not just their own part
- Use Git with one branch per person and merge into main at each sync

---

## 12. Suggested build order

Fill in real dates once the deadline is known. Phases are in order, not necessarily equal in length.

| Phase | What | Who |
|-------|------|-----|
| 1 | Set up the project, agree on the data model, create the repo, everyone's Android Studio working on a real phone | All |
| 2 | Core pieces built separately: measurement engine, outline and dots tested with development-only fake data, Room database | M1, M2, M3 |
| 3 | Integration: real readings flowing into the map and saved sessions | All |
| 4 | Heatmap quality: IDW tuning, fading, legend, metric switcher | M2, M3 |
| 5 | Compare mode (F7) | M3, with M2 for rendering |
| 6 | Weak-spot flags (F8) if time allows | M3 |
| 7 | Real-room testing, bug fixing, report, demo rehearsal | All |

**Rule:** if the schedule slips, cut F8, then the "could have" list. Never cut the measurement quality or the honesty about estimates, because they are the networking part of the project.

---

## 13. Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Nobody on the team has built an Android app before | High: slows every phase | Start with a tiny "hello readings" app in phase 1, learn permissions and Compose early |
| Public test server results fluctuate | Medium: noisy map | Same server every time, short tests, optional double reading per spot |
| Android restricts Wi-Fi and cellular data access by version | High: features may not work on some phones | Test on real phones early, check permissions per Android version |
| ICMP ping blocked on some networks | Low | HTTP fallback for packet loss |
| IDW ignores walls, map looks too clean | Medium: misleading | Distance fade plus a visible "estimated" note |
| Manual tapping gives wrong positions | Medium | Show readings as visible dots, let the user delete or redo a dot |
| Phones differ in how they report signal | Medium | Test on all team phones, state the limitation in the report |
| Scope creep | High | Section 2 non-goals are fixed unless the whole team agrees |

---

## 14. Success criteria

The project is done when:
1. A user can map a room of normal size in under 10 minutes.
2. A session shows a heatmap for every metric in section 6.4.
3. Two sessions of the same type can be compared side by side.
4. The app works on every phone the team owns.
5. Every team member can explain, without notes, how each metric is measured and what IDW does and does not do.
6. The report states the limitations honestly (public server dependence, IDW and walls, manual positioning).

---

## 15. Questions the professor is likely to ask (viva prep)

1. How exactly do you measure download speed? Why parallel connections?
2. What is the difference between latency and jitter? Why report both?
3. How do you measure packet loss, and what if ICMP is blocked?
4. Why is your speed test affected by your internet plan, and how do you handle it?
5. What is RSSI, what do the dBm values mean, and why is -60 better than -80?
6. What is the difference between 2.4 GHz and 5 GHz in range and speed, and what is a channel?
7. Why can't you mix Wi-Fi and mobile data readings in one map?
8. What does IDW do, and where does it give wrong answers?
9. Why not use GPS for position?
10. What would you change if you had more time?

---

## 16. Open questions

- Final deadline: Not provided
- Any Android experience in the team: Not provided
- Min Android version, based on the phones the team owns: Android 16 / API 36 was selected; the app's `minSdk` is 36
- App name: HeatNet
- Public test server: Cloudflare's public HTTPS speed-test endpoints are the initial configurable defaults; validation from the VIT network remains pending
