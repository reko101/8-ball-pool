# Pool Security Lab

An offline Android laboratory application for a university cybersecurity course.

It contains its **own** eight-ball pool simulation, and a set of screens that use that
simulation to teach how client-held game state can be exposed, how tampering is
detected, and why client-side integrity checking is not a trust boundary.

## Scope and boundaries

This application:

* is completely offline and declares **no permissions at all** (no `INTERNET`,
  no `SYSTEM_ALERT_WINDOW`, no accessibility service, no `QUERY_ALL_PACKAGES`);
* never reads, writes, patches, hooks, injects into or overlays any other application;
* contains no injection, no hooking, no APK patching, no instrumentation scripts,
  no anti-cheat bypass, no authentication bypass and no payment bypass;
* uses **invented** addresses on the memory screen that correspond to nothing real;
* uses a simulated coin balance that buys nothing and has no value.

Every value the app analyses belongs to its own process.

## Screens

| Screen | Purpose |
| --- | --- |
| **Game** | Playable offline eight-ball simulation: table, cue ball, 15 numbered balls, six pockets, turns, direction, power, physics, collisions, pocket detection, win/loss, reset. |
| **State** | Live inspector over the app's own `GameState`: name, logical path, type, value, simulated address. |
| **Memory** | A drawing of a hypothetical struct layout (`0x1000 GameState`, `0x1040 PlayerState`, ...). Tap a field for a teaching card: type, example value, simulated address, description, risk, recommended defence. |
| **Tamper** | Seal a baseline, apply a simulated unauthorised modification to this app's own fields, and detect it. Shows original value, current value, detection time and integrity status. Includes a control experiment using the sanctioned mutation path. |
| **Report** | The written security report: manipulation, client vs server authority, virtual currency, conceptual memory tampering, anti-cheat detection, defensive programming. |

## Requirements

* Android Studio Ladybug (2024.2.1) or newer
* JDK 17 (bundled with Android Studio)
* Android Gradle Plugin 8.7.3, Gradle 8.9, Kotlin 2.0.21
* `compileSdk` / `targetSdk` 35 (Android 15), `minSdk` 26

## Build

```
File > Open   ->  select the PoolSecurityLab folder
Tools > SDK Manager -> install "Android 15.0 (API 35)" SDK Platform + Build-Tools 35
Sync Project with Gradle Files
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release APK (R8 enabled, signed with the local debug key for lab use only):
`app/build/outputs/apk/release/app-release.apk`

Command line:

```
./gradlew assembleDebug
./gradlew assembleRelease
```

If the Gradle wrapper JAR is missing, either let Android Studio regenerate it on
first sync, or run `gradle wrapper --gradle-version 8.9` once with a local Gradle.

## Install

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

There is no runtime permission prompt, because the app requests no permissions.

## Test on Android 15

1. Create an AVD with system image **API 35**, or use a physical Android 15 device.
2. Install and launch. Confirm the app opens on the Game screen with no crash.
3. Confirm the layout is not hidden behind the status bar or the gesture bar
   (edge-to-edge insets are applied in `MainActivity`).
4. Enable airplane mode and repeat every screen; behaviour must be identical.
5. Play a shot, then open **State** and confirm the values changed.
6. On **Tamper**: `SEAL BASELINE` -> `Simulate balance overwrite` -> `RUN INTEGRITY CHECK`.
   Expect `WARNING: GAME-STATE INTEGRITY VIOLATION DETECTED` with 1000 -> 999999.
7. Press `RESTORE TRUSTED STATE`, check again, expect `INTEGRITY: OK`.
8. Press `Sanctioned change: award 25 coins`, check again, expect `INTEGRITY: OK`
   even though the balance changed. This is the point of the exercise.
9. Rotate the device and confirm no crash.

## Architecture

```
core/     GameHolder (single shared state), LabApplication
game/     TableSpec, Ball, GameState, PhysicsEngine, PoolGame, PoolTableView, GameFragment
security/ SimulatedAddresses, StateVariable, StateInspector, IntegrityGuard,
          DefenseCatalog, ReportContent, and the four laboratory fragments
```

`GameHolder.game.state` is the single source of truth. The table renderer and every
security screen read the same object, so nothing on the lab screens is staged or faked.
