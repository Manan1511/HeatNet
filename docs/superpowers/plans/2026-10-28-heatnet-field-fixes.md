# HeatNet field fixes implementation plan

**Goal:** Make weak-network readings useful, compare Wi-Fi and mobile sessions, improve room drawing, and replace the launcher icon.

**Architecture:** Give sequential probes separate bounded deadlines, retain partial results, and preserve honest ICMP/HTTP labels. Compare shared network metrics and show radio maps separately. Add validated corner dragging and optional grid snapping to the outline editor.

**Tech stack:** Kotlin, Jetpack Compose, Android API 36, Room, OkHttp and JUnit.

- [x] Add regression tests for slow probes and ensure later stages still run; implement independent deadlines and fallback time.
- [x] Complete mixed-network comparison, with separate signal maps and safe shared metrics.
- [x] Add tests and implement drag and optional grid snapping with geometry validation.
- [x] Replace launcher and monochrome icons with a radar/signal design.
- [x] Update documentation, run relevant tests and build the debug APK. Report physical-device verification limits.
