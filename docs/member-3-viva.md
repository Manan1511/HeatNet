# HeatNet Member 3: Viva Notes (Data, Compare and Analysis)

## A short explanation

"I built HeatNet's data layer. It stores sessions and readings locally with Room, lets users reopen, rename and delete sessions, finds weak spots in a room, and compares two sessions to show whether a change helped. Comparison only uses room areas that both sessions measured, and it warns when the comparison may be unfair."

## What I built

| Piece | What it does | Where |
| --- | --- | --- |
| Local storage | Saves sessions (outline, size, connection type) and readings (position, metrics, loss method, radio fields, status, demo flag) in Room. No server or account. | `data/.../db/` |
| Repository | One entry point for the app: create session, add and delete readings, load, rename, delete. | `SessionRepository.kt` |
| Session list | Saved sessions, with rename, delete and compare picking. | `app/.../SessionListScreen.kt` |
| Weak-spot detection (F8) | Flags problem corners of the room and writes one sentence per corner. | `WeakSpotDetector.kt` |
| Session comparison (F7) | Before and after numbers per metric, a verdict, and warnings. | `SessionComparison.kt`, `CompareScreen.kt` |

## How weak-spot detection works

1. It needs at least 3 readings. With fewer it asks for more.
2. It cuts the room's bounding box into four quadrants around its centre (north-west, north-east, south-west, south-east).
3. A quadrant is flagged for a problem when at least half of its readings that have that metric show the problem.
4. Starting thresholds (from the PRD, to be tuned after real-room testing):

| Problem | Flagged when |
| --- | --- |
| Weak signal (Wi-Fi) | RSSI below -75 dBm |
| Weak signal (mobile) | Below -100 dBm (an assumption, not yet tuned on our phones) |
| High latency | Above 100 ms |
| Packet loss | Above 2%, **ICMP only** |
| Slow download | Below 25% of the best download in that session |

5. The output is one sentence per flagged quadrant, for example "Weak signal and high latency in the north-east corner".
6. Failed tests (null values) are ignored for that metric only. HTTP-failure rates are ignored for packet loss, because they are not real packet loss.

## How comparison works

- Only sessions on the same connection type can be compared. Two Wi-Fi sessions, or two mobile sessions.
- Both sessions need at least one reading. The same session cannot be compared with itself.
- Each room is divided into a 4×4 grid. Only cells that **both** sessions sampled are compared. This keeps the comparison fair when readings were taken in different places.
- For each metric it gives the before mean, the after mean and the difference.
- The verdict is IMPROVED, WORSE, SIMILAR or NOT_ENOUGH_DATA. A difference within 5% counts as SIMILAR (noise). Direction respects whether higher or lower is better for that metric.
- Both maps use one shared colour range, so a small change does not look big.
- Warnings appear when:
  - the room sizes differ by more than 20%,
  - the sessions used different access points (BSSID),
  - the sessions have no shared sampled areas,
  - any reading is simulated demo data,
  - HTTP request-failure rates are present (they are not compared as ICMP loss).

## Likely questions

**1. What did you personally build?**
Storage with Room, the repository, the session list with rename and delete, weak-spot detection and session comparison.

**2. Why store positions in metres?**
Pixels depend on screen size. Metres keep saved sessions and comparisons valid on any device.

**3. Why local storage and no backend?**
The PRD rules out accounts, login and cloud sync. It keeps the project simple and keeps data on the phone.

**4. How does weak-spot detection decide a corner is bad?**
At least half of that corner's readings (those with the metric) cross a threshold.

**5. Why quadrants?**
It gives the user a plain-language answer ("north-east corner") without needing a floor plan.

**6. Why is packet loss ICMP-only in your analysis?**
The HTTP fallback counts failed web requests, which is an application-level signal, not dropped packets. Mixing them would be misleading.

**7. How do you know your thresholds are right?**
We do not yet. They are the PRD's starting values. The plan is to tune them after real-room testing, and the mobile-signal threshold is an assumption.

**8. Why only compare shared areas?**
If one session sampled the kitchen and the other the bedroom, comparing their averages says nothing about a change. Shared grid cells make it a like-for-like comparison.

**9. Why a 5% "similar" band?**
Network measurements are noisy. Small differences should not be reported as improvements.

**10. Why must both sessions have the same connection type?**
Wi-Fi and mobile data use different technologies and signal scales, so their numbers are not comparable.

**11. What is `isDemo`?**
A flag marking simulated readings from Demo mode, so they are never mistaken for field measurements. Comparison warns when any are included.

**12. What happens if a test failed at a spot?**
The value is null and is ignored for that metric. The rest of the reading still counts.

**13. What if the user deletes a session?**
The session and its readings are removed from the phone, after a confirmation.

**14. What are the limitations of your analysis?**
- Thresholds are untuned starting values.
- Quadrants are coarse: they use the room's bounding box centre.
- Readings are tapped by hand, so positions can be wrong.
- The 4×4 grid is a fixed choice.

**15. What would you improve?**
Tune the thresholds after real-room testing, use finer or adaptive zones, show per-spot detail, and take two tests per spot to reduce noise.

## Honest status

- **Done:** storage, session list, rename and delete, comparison, weak-spot detection, unit tests for the analysis.
- **Not done yet:** real-room testing in at least three rooms, threshold tuning from that testing, and tuning of the heatmap constants. Say this plainly if asked.
