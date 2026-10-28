# AGP Built-in Kotlin Migration Implementation Plan

> **Status:** Completed and verified on 2026-10-28. The initial review kept these local upgrade edits uncommitted; the user's later request was to finish the remaining work and deliver it.

**Goal:** Move HeatNet's Android modules onto AGP 9.4.1 built-in Kotlin and remove temporary compatibility flags that the migration no longer needs.

**Architecture:** AGP 9.4.1 remains the Android build plugin and supplies its built-in Kotlin integration. Remove the external `kotlin-android` plugin, align the Compose compiler Gradle plugin to AGP's bundled Kotlin 2.2.10, and retain KSP 2.3.12 for Room. Measurement behavior is unchanged; the app manifest also no longer locks the activity to portrait.

**Tech Stack:** Android Gradle Plugin 9.4.1, Gradle 9.8.0, built-in Kotlin/KGP 2.2.10, Kotlin Compose plugin 2.2.10, KSP 2.3.12, Room 2.8.5, JDK 17.

**Spec:** User-approved scope in the current conversation: adopt built-in Kotlin while keeping AGP 9.4.1 and Gradle 9.8.0, and verify KSP.

## Global Constraints

- Keep the Gradle wrapper on 9.8.0 and AGP on 9.4.1.
- Keep Android compile, min, and target SDK at 36, and Java/Kotlin bytecode targets at 17.
- Keep KSP at 2.3.12 and Room at 2.8.5 unless verification exposes a concrete incompatibility.
- Preserve the user's wrapper-script/JAR edits. The PRD began as an untracked project reference and has now been updated with verified status and remaining field work.

## Review Focus

- Built-in Kotlin exposes the `kotlin.compilerOptions` DSL in every Android module; verify through Kotlin compilation.
- Compose compiler plugin version matches the Kotlin compiler used by AGP; verify the app compiles.
- KSP still generates Room sources with built-in Kotlin; verify the data module's KSP and tests.
- Removing legacy AGP flags does not change SDK declarations or required generated resources; verify Gradle configuration and APK assembly.
- Existing code has no `kapt`, custom Kotlin source sets, `resValue`, or `uses-sdk` declarations; confirm by repository search before editing.

---

### Task 1: Migrate Android modules to built-in Kotlin

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `data/build.gradle.kts`
- Modify: `measurement/build.gradle.kts`
- Modify: `gradle.properties`

**Interfaces:**
- Consumes: AGP 9.4.1, Gradle 9.8.0, KSP 2.3.12, Room 2.8.5, Android SDK 36, JVM target 17.
- Produces: Android modules compile Kotlin through AGP built-in Kotlin; Compose uses the matching Kotlin 2.2.10 compiler plugin; Room remains processed by KSP.

- [x] Remove `kotlin-android` plugin declarations from the root and all Android modules, and remove its version-catalog alias.
- [x] Set the Compose compiler plugin version to 2.2.10 to match AGP 9.4's bundled KGP; keep KSP 2.3.12.
- [x] Remove deprecated compatibility flags that are no longer needed or whose features are unused; retain non-deprecated project-specific flags.
- [x] Run `./gradlew help`; configuration succeeds without the deprecated `kotlin-android` plugin/variant API warnings.
- [x] Run `./gradlew build --dry-run`; the task graph configures successfully.
- [x] Run `./gradlew test assembleDebug`; all JVM unit tests pass, Room KSP runs, and a debug APK is assembled.

### Verification results

- `.\gradlew.bat help` and `.\gradlew.bat build --dry-run`: successful.
- `.\gradlew.bat test assembleDebug connectedDebugAndroidTest lintDebug`: current working-tree verification on 2026-10-28 passed 164 JVM tests and 9 instrumentation tests, with 0 failures, 0 errors, and 0 skipped. Room KSP completes and the debug APK is assembled at `app/build/outputs/apk/debug/app-debug.apk`.
- Lint completes for `:measurement`, `:data`, and `:app` with 0 errors, 20 warnings, and one hint. These include newer SDK/dependency suggestions and minor Compose/resource recommendations; the app intentionally remains on API 36.
- The emulator smoke check reached the room setup flow and confirmed the home screen resizes in landscape after removal of the fixed portrait orientation.
- Remaining non-blocking build notices: AGP's dependency-constraint import-performance hint and Gradle deprecations that will need review before Gradle 10. The removed Kotlin Android plugin/variant API warnings no longer appear.
- Independent read-only review found no code defects or blockers.

No new runtime test was needed for the configuration migration; existing module tests and KSP-backed APK assembly provide the regression coverage. Physical-device and three-room field validation remain separate project tasks.
