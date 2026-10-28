# HeatNet Demo Script (about 4 minutes)

## Before you start (5 minutes ahead)

- [ ] Phone is on **one** connection (Wi-Fi is easiest). Do not switch it during the demo.
- [ ] App is installed and opens. If you plan to map Wi-Fi, leave precise location permission ungranted so the first Wi-Fi session can show the permission prompt.
- [ ] Phone is charged and screen timeout is long.
- [ ] You know a room to map. A small room with a clear shape works best.
- [ ] Confirm internet access and the Cloudflare test endpoint before the presentation. If a reading is partial or blocked, explain what failed and show an already saved real session if available; do not invent readings.
- [ ] Open the diagram `docs/network-reading.html` in a tab, in case you are asked how a reading works.

## The script

### 1. Intro (20 seconds)

> "HeatNet maps how good the Wi-Fi or mobile data is in each part of a room. I'll draw a room, take a few readings, and show the heatmap."

### 2. Start a session (30 seconds)

1. Tap **Start a new map**.
2. Point out the detected connection.
   > "The app detects Wi-Fi or mobile data and locks each session to that connection. Shared internet metrics can be compared across connection types."
3. If you selected Wi-Fi and permission is missing, tap **Allow precise location**. Android shows its permission dialog; approving it continues to room setup.
   > "Android requires this permission to expose Wi-Fi radio details. HeatNet uses the details for this reading and does not use them to determine where I am in the room."
4. If you selected mobile data, tap **Continue with Mobile data**; the app does not request Wi-Fi location permission.

### 3. Draw the room (30 seconds)

1. Tap four or five corners to make the room shape, then close it.
2. Enter one wall length in metres.
   > "I enter one real wall length so the app knows the scale. Everything is stored in metres."

### 4. Take readings (60 seconds)

1. Tap a spot on the map, then press **Measure**.
2. While it runs, say:
   > "It captures the current network, reads the Wi-Fi signal, then runs download, upload, latency and packet-loss tests. On a slow connection this can take about a minute."
3. A dot appears. Repeat at **at least 3 spots**, ideally 5 or 6, in different parts of the room, one near the router and one far away.
   > "With fewer than 3 readings, no heatmap is drawn."

### 5. Show the heatmap (45 seconds)

1. Point to the colours: bright near a dot, faded far from one.
   > "Green is good, red is bad. The colours between dots are estimates, so they fade. The dots are the real measurements."
2. Switch between metrics, for example signal then latency.
3. Tap a dot to show the detail sheet.
   > "Here are the raw numbers for this one reading."
4. Mention the weakness:
   > "The estimate doesn't know about walls, so it can look smoother than reality."

### 6. Finish and weak spots (30 seconds)

1. Tap **Finish**.
2. Read the weak-spot sentence aloud, or say "no weak spots found" if that's what shows.
   > "The app splits the room into four corners and flags a corner when most of its readings are bad."

### 7. Compare (45 seconds)

If you have two sessions:

1. Go to **Saved sessions** and tap **Compare**.
2. Pick two sessions. The button says how many more to pick.
3. Show the side-by-side maps and the verdicts.
   > "It only compares areas both sessions measured, and it warns if the comparison might be unfair, for example different rooms or legacy simulated data from an older app version."

If you only have one session, say: "With two sessions, this screen compares shared network measurements." Do not fake it.

### 8. Close (15 seconds)

> "So: draw, measure, see where the network is weak, and compare after a change."

## If something goes wrong

| Problem | What to do |
| --- | --- |
| A reading says "blocked" | Say it out loud. It means no network, the wrong connection type, or missing permission. Fix it and measure again. |
| A reading is "partial" | Say that some tests failed and the app keeps what it got. It never invents numbers. |
| No internet or test endpoint is unavailable | Explain that no live network result is available. Reopen an already saved real session if one exists; do not present invented values. |
| The app crashes | Reopen it. Sessions are saved on the phone. Continue from **Saved sessions**. |
| Compare is disabled | You need at least two saved sessions. |

## Things not to claim

- Do not say the app was tested in several real rooms unless it was.
- Do not call the HTTP fallback "packet loss". It's a failed-request rate.
- Do not say the speed test measures Wi-Fi capacity. It measures internet speed to a public server.
- The official app takes live measurements only. Heatmap areas between measured dots are estimates.
