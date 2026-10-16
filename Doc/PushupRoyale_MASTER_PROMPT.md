# Pushup Royale: Master Prompt & Explain File

**Stack:** Kotlin · Jetpack Compose · CameraX · ML Kit Pose Detection
**Package:** `com.example.pushuproyale`

> **How to use this file**
> 1. Read top to bottom. It runs **big picture → core logic → file-by-file → tiny details**.
> 2. Paste **Section 1** into an AI coding assistant to rebuild or extend the app.
> 3. **Appendix A** holds the complete source, so this one file is enough to recreate the whole project.
>
> **Status:** Upgraded to **v3.0.0** (Major Update per `PushupRoyale_MASTER_FIX_PROMPT.md`) with Full-Body Pose Tracking (shoulders, elbows, wrists, hips, knees, ankles, head/nose), Anti-Cheat torso vertical displacement validation, real-time plank line alignment checks, front-camera placement guidance, full-body Liquid Glass skeleton overlay, 480x640 fast pipeline inference, Reports & Multi-Period Analytics (Daylatch-style), Settings engine, and Prasad Dhodamani developer credits.

---

## Contents

**Part A: The big picture**
1. Master prompt (copy-paste)
2. What the app is
3. Architecture at a glance
4. Tech stack and versions
5. Project structure
6. End-to-end flow

**Part B: Core logic**
7. Rep detection algorithm
8. Gamification system
9. Screen and UI spec

**Part C: File-by-file walkthrough**
10. Every file explained

**Part D: Small details**
11. Threading and lifecycle
12. Permission flow
13. Compose details
14. Known limitations and quirks
15. Tuning cheat sheet
16. Build, run, troubleshoot
17. Manual test checklist
18. Roadmap and extension prompts
19. Branding note
20. Glossary

**Appendix A:** Complete source code

---

# PART A: THE BIG PICTURE

## 1. Master prompt (copy-paste)

Paste everything inside the block into an AI coding assistant.

```text
ROLE
You are a senior Android engineer who writes clean, compile-ready Kotlin.

GOAL
Build a complete Android app called "Pushup Royale": a pushup counter that
watches the user through the FRONT camera, detects the body with on-device AI
pose detection, and counts reps automatically. Wrap it in a battle-royale-style
progression game: XP per rep, 8 ranks, and a "Winner Winner Pushup Dinner" win
when a match reaches the rep goal. Do NOT use the PUBG name, logo, or assets.

STACK (use these exact versions)
- Kotlin 1.9.24, JVM target 17, Gradle Kotlin DSL (.kts), single module ":app"
- Android Gradle Plugin 8.5.2, compileSdk 34, targetSdk 34, minSdk 24
- applicationId and namespace: com.example.pushuproyale
- Jetpack Compose BOM 2024.06.00, Material 3, Compose compiler extension 1.5.14
- androidx.activity:activity-compose 1.9.0
- CameraX 1.3.4: camera-camera2, camera-lifecycle, camera-view
- ML Kit com.google.mlkit:pose-detection:18.0.0-beta4 (bundled model, STREAM_MODE)
- gradle.properties must contain android.useAndroidX=true
- No DI, no database, no network, no INTERNET permission

FILES TO OUTPUT (complete contents, one code block per file, path as heading)
 1. settings.gradle.kts
 2. build.gradle.kts            (root; plugin versions, apply false)
 3. gradle.properties
 4. app/build.gradle.kts
 5. app/src/main/AndroidManifest.xml
 6. app/src/main/res/values/themes.xml
 7. app/src/main/java/com/example/pushuproyale/PushupAnalyzer.kt
 8. app/src/main/java/com/example/pushuproyale/Ranks.kt
 9. app/src/main/java/com/example/pushuproyale/MainActivity.kt

BEHAVIOR SPEC
Rep detection (class PushupAnalyzer implements ImageAnalysis.Analyzer):
- ML Kit PoseDetector in STREAM_MODE. Feed InputImage.fromMediaImage(image, rotationDegrees).
- Use only LEFT/RIGHT SHOULDER, ELBOW, WRIST landmarks.
- A side counts only if all 3 landmarks have inFrameLikelihood >= 0.6.
- Elbow angle = angle at the elbow between elbow->shoulder and elbow->wrist,
  computed with atan2, absolute value, folded into 0..180 degrees.
- Final angle = average of the valid sides; null if no side is valid.
- State machine with hysteresis: angle < 90 marks DOWN. angle > 155 while DOWN
  gives reps++ and clears DOWN. A null angle leaves the state unchanged.
- Expose reset(). Always close the ImageProxy in addOnCompleteListener.
- Report (angle: Int?, reps: Int) to the UI on the main thread.

Gamification (object Ranks):
- 1 rep = 10 XP. Tiers (name to XP): Bronze 0, Silver 500, Gold 1500,
  Platinum 3500, Diamond 7000, Crown 12000, Ace 20000, Conqueror 35000.
- DAILY_GOAL = 30 reps. Finishing a match with reps >= goal adds 1 win.
- Persist xp and wins in SharedPreferences "royale" (keys "xp" and "wins").
- info(xp) returns Triple(rankName, progress 0..1 toward next rank, xpToNext).
  The top rank returns progress 1 and xpToNext 0.

UI (single screen, Compose, dark Material 3, primary color gold 0xFFF2A900):
- If camera permission is missing: centered button "Allow camera to drop in",
  and auto-request the permission once on first composition.
- Otherwise: full-screen CameraX PreviewView (front camera) behind two HUD
  panels with 67% black background (0xAA000000).
- Top HUD: rank name uppercase, gold, black weight, 20sp on the left;
  "🍗 {wins} wins  •  {xp} XP" on the right; gold LinearProgressIndicator;
  "{n} XP to next rank" 11sp light gray, shown only when n > 0.
- Bottom HUD (centered): gold banner "WINNER WINNER PUSHUP DINNER!" only when
  the last finished match won; rep count 96sp black weight white; line
  "Goal 30  •  elbow {angle or --}°" light gray; button "Finish match & bank XP".
- Finish button: save session XP and win, reload xp/wins, set banner =
  (reps >= goal), reset the analyzer, set reps to 0.
- Manifest: CAMERA permission, optional front-camera feature, portrait lock,
  label "Pushup Royale", no custom icon.

QUALITY BAR
- Must sync and build in Android Studio Koala (2024.1.1) or newer with JDK 17.
- No placeholder TODOs. Comment only where logic is non-obvious
  (angle math, thresholds, state machine).

AFTER THE CODE
- List run steps, the tunable constants, and every assumption you made.
```

---

## 2. What the app is

**Pushup Royale** is an Android app that counts your pushups for you. You prop the phone on the floor, do pushups side-on to it, and the app watches your arms through the **front camera** and adds one rep each time you go down and come back up. Reps turn into **XP**, XP climbs a **battle-royale-style rank ladder**, and a match that reaches the rep goal earns a **"Winner Winner Pushup Dinner"** win.

**Features in this version**
- Live front-camera preview.
- On-device AI pose detection (no internet, no frames leave the phone).
- Automatic rep counting from elbow angle.
- Live elbow-angle readout (useful for calibration).
- XP, 8 ranks, rank-progress bar, win counter.
- "Finish match & bank XP" button that saves progress.
- Progress saved on the device between launches.

**Not in this version:** skeleton overlay, sound or haptics, history, streaks, squads, leaderboards, settings screen, form checking. See Section 18.

---

## 3. Architecture at a glance

```text
 Front camera
     │
     ├──► CameraX Preview ─────────────► PreviewView (what you see)
     │
     └──► CameraX ImageAnalysis ───────► PushupAnalyzer.analyze()
                                            │
                                            │  ML Kit PoseDetector (33 landmarks)
                                            │  elbow angle = shoulder–elbow–wrist
                                            │  state machine: DOWN (<90°) → UP (>155°) = +1 rep
                                            ▼
                                 onUpdate(angle, reps)   [main thread]
                                            ▼
                         Compose state (reps, angle) ──► HUD recomposes
                                            │
                              "Finish match & bank XP"
                                            ▼
                      Ranks.addSession() ──► SharedPreferences ("xp", "wins")
```

**Layers**

| Layer | Responsibility | File |
|---|---|---|
| UI | Permission screen, camera view, HUD, button | `MainActivity.kt` |
| Vision / logic | Pose detection, angle math, rep state machine | `PushupAnalyzer.kt` |
| Game / data | XP, ranks, wins, persistence | `Ranks.kt` |
| Build / config | Gradle, manifest, theme | `*.gradle.kts`, `AndroidManifest.xml`, `themes.xml` |

There is **no ViewModel, database, DI framework, or network layer**. It is deliberately small.

---

## 4. Tech stack and versions

| Component | Version | Role | Why chosen |
|---|---|---|---|
| Kotlin | 1.9.24 | Language | Pairs with Compose compiler 1.5.14 |
| Android Gradle Plugin | 8.5.2 | Build | Matches Android Studio Koala; needs Gradle 8.7+ |
| JDK | 17 | Build/compile | Required by AGP 8.x |
| compileSdk / targetSdk | 34 | API level | Android 14 |
| minSdk | 24 | Oldest supported | Android 7.0 (a safe floor; 21 would also work) |
| Compose BOM | 2024.06.00 | Version alignment | One number controls all Compose library versions |
| Material 3 | via BOM | UI components | `Surface`, `Button`, `Text`, `LinearProgressIndicator` |
| Compose compiler ext. | 1.5.14 | Compose compiler | Must match Kotlin 1.9.24 |
| activity-compose | 1.9.0 | `setContent`, permission launcher | Standard Compose entry point |
| CameraX | 1.3.4 | Camera preview and frame analysis | Simple lifecycle-aware camera API |
| ML Kit pose-detection | 18.0.0-beta4 | AI body landmarks | Bundled, on-device, real-time, free |

---

## 5. Project structure

```text
PushupRoyale/
├── settings.gradle.kts            Project name, repositories, module list
├── build.gradle.kts               Root: plugin versions (not applied here)
├── gradle.properties              android.useAndroidX=true
└── app/
    ├── build.gradle.kts           App module: SDK levels, Compose, dependencies
    └── src/main/
        ├── AndroidManifest.xml    Permission, launcher activity, theme, label
        ├── res/values/
        │   └── themes.xml         Window theme (dark Material, no action bar)
        └── java/com/example/pushuproyale/
            ├── MainActivity.kt    Compose UI + camera binding + permission
            ├── PushupAnalyzer.kt  ML Kit pose + elbow angle + rep state machine
            └── Ranks.kt           XP, ranks, wins, SharedPreferences
```

**Not included:** Gradle wrapper (`gradlew`, `gradle/wrapper/`), launcher icon, unit tests, ProGuard rules. See Section 16.

---

## 6. End-to-end flow

1. **Launch.** `MainActivity.onCreate` calls `setContent` with a dark Material 3 theme (gold primary) and shows `App()`.
2. **Permission check.** `App()` reads the camera permission. If missing, it shows a button and auto-opens the system dialog once.
3. **State creation.** Once granted, it creates `reps`, `angle`, `xp` (read from storage), `wins` (read from storage), `won`, and one `PushupAnalyzer`.
4. **Camera binding.** `CameraView` builds a `PreviewView`, gets the `ProcessCameraProvider`, and binds **Preview + ImageAnalysis** to the Activity lifecycle using the **front** camera.
5. **Frames flow.** Preview frames go to the screen. Analysis frames go to `PushupAnalyzer.analyze()` on a single background thread.
6. **Pose detection.** The analyzer passes each frame to ML Kit, which asynchronously returns a `Pose` (33 landmarks with x, y, and confidence).
7. **Angle.** It computes the elbow angle for each arm that is confidently visible and averages them.
8. **State machine.** Angle below 90° sets `down`. Angle above 155° while `down` adds one rep and clears `down`.
9. **UI update.** `(angle, reps)` is posted to the main thread, which updates Compose state and redraws the HUD.
10. **Frame released.** The `ImageProxy` is closed so CameraX can deliver the next (latest) frame.
11. **Finish match.** Tapping the button saves `reps × 10` XP and possibly a win, refreshes rank info, shows or hides the banner, and resets the analyzer.
12. **Background / return.** CameraX stops when the Activity stops and restarts when it returns, because it is bound to the lifecycle.

---

# PART B: CORE LOGIC

## 7. Rep detection algorithm (the heart of the app)

### 7.1 Landmarks used
ML Kit returns 33 body landmarks. The app uses only 6:

| Side | Shoulder | Elbow | Wrist |
|---|---|---|---|
| Left | `LEFT_SHOULDER` | `LEFT_ELBOW` | `LEFT_WRIST` |
| Right | `RIGHT_SHOULDER` | `RIGHT_ELBOW` | `RIGHT_WRIST` |

Each landmark has `position.x`, `position.y` (pixels) and `inFrameLikelihood` (0.0 to 1.0).

### 7.2 Confidence filter
For each side, the **minimum** `inFrameLikelihood` of its three landmarks must be **at least 0.6**. Otherwise that side is ignored for the frame. If a side is partly hidden by the body (common when filming side-on), it simply drops out.

### 7.3 Angle math
The elbow angle is the angle at the **elbow** between the line to the shoulder and the line to the wrist:

```text
r = atan2(wrist.y − elbow.y, wrist.x − elbow.x)
  − atan2(shoulder.y − elbow.y, shoulder.x − elbow.x)
angle = |degrees(r)|
if angle > 180 → angle = 360 − angle        // fold into 0..180
```

**Worked examples** (image y grows downward):

| Shoulder | Elbow | Wrist | Result |
|---|---|---|---|
| (0,0) | (0,100) | (100,100) | **90°** (elbow bent like an "L") |
| (0,0) | (0,100) | (0,200) | **180°** (arm fully straight) |

**Why this works regardless of phone angle:** an angle between two lines does not change if the whole picture is rotated or mirrored. So the front camera's mirroring and the phone's rotation do not affect the result.

### 7.3b Combining arms
- Both sides valid: **average** of the two angles.
- One side valid: use that one (typical for side-on filming).
- Neither valid: `null` (the HUD shows `--`).

### 7.4 State machine

| Current `down` | Condition | Action |
|---|---|---|
| any | `angle < 90` | `down = true` |
| `true` | `angle > 155` | `reps++`, `down = false` |
| any | `angle` is `null` | nothing changes |
| any | `90 ≤ angle ≤ 155` | nothing changes (dead zone) |

The gap between **90** and **155** is **hysteresis**. It stops jitter around a single threshold from counting extra reps.

### 7.5 Timeline example (one rep)

| Frame | Elbow angle | `down` after | Result |
|---|---|---|---|
| 1 | 172° | false | Top position |
| 2 | 140° | false | Lowering |
| 3 | 95° | false | Not below 90° yet |
| 4 | 84° | **true** | Bottom reached |
| 5 | 110° | true | Pushing up |
| 6 | 150° | true | Not above 155° yet |
| 7 | 161° | false | **+1 rep** |

### 7.5b Edge behaviors
- **Lost tracking mid-rep:** state is kept. When tracking returns, the rep can still finish.
- **Shallow rep** (never below 90°): not counted.
- **Starting already bent** (below 90°) then extending: counts one rep.
- **No body-position check:** any elbow bend-and-extend cycle counts (see Section 14).

---

## 8. Gamification system

### 8.1 XP
`XP gained = reps × 10`, added when you tap **Finish match & bank XP**. Reps from an unfinished match are **not** saved.

### 8.2 Rank ladder

| Rank | XP needed | Lifetime reps needed |
|---|---|---|
| Bronze | 0 | 0 |
| Silver | 500 | 50 |
| Gold | 1,500 | 150 |
| Platinum | 3,500 | 350 |
| Diamond | 7,000 | 700 |
| Crown | 12,000 | 1,200 |
| Ace | 20,000 | 2,000 |
| Conqueror | 35,000 | 3,500 |

### 8.3 Progress-bar math (`Ranks.info`)
1. Find the highest tier whose threshold is ≤ current XP (`indexOfLast`).
2. If it is the last tier: return `(name, 1.0, 0)`.
3. Otherwise: `progress = (xp − lo) / (hi − lo)` and `toNext = hi − xp`.

Example: 2,400 XP → Gold (lo 1,500, hi 3,500) → progress 0.45 → 1,100 XP to Platinum.

### 8.4 Wins
`DAILY_GOAL = 30`. When a match is finished with `reps >= 30`, `wins` goes up by 1 and the banner **WINNER WINNER PUSHUP DINNER!** appears. (The constant is called "daily" but it is really a **per-match** goal. See Section 14.)

### 8.5 Persistence
- Store: `SharedPreferences` named **`royale`**.
- Keys: **`xp`** (Int), **`wins`** (Int).
- Written with `apply()` (asynchronous, fast).
- Read when the main screen is first composed.
- Cleared by uninstalling or clearing app data.
- No schema versioning or migration.

---

## 9. Screen and UI spec

### 9.1 Permission screen
A centered button: **"Allow camera to drop in"**. The system dialog is also opened automatically once.

### 9.2 Main screen layout

```text
┌────────────────────────────────────────┐
│ GOLD                  🍗 3 wins • 2400 XP │  ← top HUD (67% black)
│ ▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │  ← gold progress bar
│ 1100 XP to next rank                    │
│                                          │
│          (live camera preview)           │
│                                          │
│    WINNER WINNER PUSHUP DINNER!          │  ← only after a won match
│                   24                     │  ← rep counter (96sp)
│        Goal 30  •  elbow 128°            │
│       [ Finish match & bank XP ]         │  ← bottom HUD (67% black)
└────────────────────────────────────────┘
```

### 9.3 Style values

| Element | Value |
|---|---|
| Theme | `darkColorScheme(primary = Gold)` |
| Gold | `Color(0xFFF2A900)` |
| HUD panel color | `Color(0xAA000000)` (ARGB: `AA` ≈ 67% opacity, black) |
| Rank name | 20sp, `FontWeight.Black`, gold, uppercase |
| Wins/XP text | white, default size |
| "XP to next rank" | 11sp, `Color.LightGray` |
| Rep counter | 96sp, `FontWeight.Black`, white |
| Goal/elbow line | `Color.LightGray` |
| Banner | gold, `FontWeight.Black` |
| Padding | top HUD 12dp, bottom HUD 16dp, plus system-bar insets |

---

# PART C: FILE-BY-FILE WALKTHROUGH

## 10. Every file explained

### 10.1 `settings.gradle.kts`
| Piece | Meaning |
|---|---|
| `pluginManagement { repositories {...} }` | Where Gradle looks for **plugins**: Google, Maven Central, Gradle Plugin Portal. |
| `dependencyResolutionManagement { repositories {...} }` | Where **libraries** come from: Google and Maven Central. |
| `rootProject.name = "PushupRoyale"` | Project name. |
| `include(":app")` | Registers the single module. |

### 10.2 `build.gradle.kts` (root)
Declares plugin **versions** with `apply false`: Android application plugin `8.5.2` and Kotlin Android plugin `1.9.24`. `apply false` means "make the version known, but do not apply it to the root project". The `app` module applies them without repeating versions.

### 10.3 `gradle.properties`
One line: `android.useAndroidX=true`. It tells the build to use AndroidX libraries, which CameraX, Compose, and activity-compose all require. Missing it causes a build error.

### 10.4 `app/build.gradle.kts`

| Line / block | Meaning |
|---|---|
| `plugins {...}` | Applies the Android app and Kotlin plugins. |
| `namespace` | Package for the generated `R` class and code. |
| `compileSdk = 34` | API level compiled against (Android 14). |
| `applicationId` | Unique ID on device and Play Store. **Change before publishing.** |
| `minSdk = 24` | Oldest Android supported (7.0). |
| `targetSdk = 34` | Android version whose behavior rules the app opts into. |
| `versionCode` / `versionName` | Integer used to order updates / label users see. |
| `compileOptions` + `jvmTarget` | Java/Kotlin language level 17. Needs JDK 17. |
| `buildFeatures { compose = true }` | Enables Compose. |
| `kotlinCompilerExtensionVersion = "1.5.14"` | Compose compiler. **Must match Kotlin 1.9.24.** |

| Dependency | What it provides here |
|---|---|
| `platform("androidx.compose:compose-bom:2024.06.00")` | Locks all Compose versions together. Other Compose lines then need no version. |
| `material3` | `MaterialTheme`, `Surface`, `Button`, `Text`, `LinearProgressIndicator`. |
| `ui` | Core Compose: `Modifier`, `Color`, `AndroidView`, `dp`/`sp`. |
| `activity-compose:1.9.0` | `ComponentActivity.setContent`, `rememberLauncherForActivityResult`. |
| `camera-camera2:1.3.4` | CameraX backend built on Camera2. Also brings `camera-core`. |
| `camera-lifecycle:1.3.4` | `ProcessCameraProvider` and lifecycle binding. |
| `camera-view:1.3.4` | `PreviewView`. |
| `pose-detection:18.0.0-beta4` | ML Kit pose detector with the model bundled in the APK (works offline, adds APK size). |

No `isMinifyEnabled`, signing config, or release settings are defined.

### 10.5 `AndroidManifest.xml`

| Element | Meaning |
|---|---|
| `<uses-permission CAMERA>` | Declares camera use. It is a **dangerous** permission, so the app must also ask at runtime (it does). |
| `<uses-feature camera.front required="false">` | Lets stores list the app for phones without a front camera (it just will not work there). |
| `android:label="Pushup Royale"` | Name under the launcher icon. |
| `android:theme="@style/Theme.App"` | Points to `themes.xml`. |
| `<activity .MainActivity>` | The only screen. |
| `exported="true"` | Required for an activity with an intent filter when targeting Android 12+. |
| `screenOrientation="portrait"` | Locks the screen to portrait. |
| `MAIN` + `LAUNCHER` intent filter | Makes this the app's launch entry. |

Not set: icon (default system icon is used), backup rules, RTL support, INTERNET permission.

### 10.6 `res/values/themes.xml`
Defines `Theme.App` with parent `android:Theme.Material.NoActionBar` (framework dark theme, no action bar). Compose draws all real UI, so this only affects the window background and system bars behind and before Compose.

### 10.7 `PushupAnalyzer.kt`

| Member | Type | Purpose |
|---|---|---|
| Constructor `onUpdate` | `(Int?, Int) -> Unit` | Callback with `(elbowAngle, reps)` after every analyzed frame. |
| `detector` | ML Kit `PoseDetector` | Created with `STREAM_MODE`: tuned for video, reuses information between frames for smoother, faster results. |
| `main` | `Handler(Looper.getMainLooper())` | Delivers updates on the main thread. |
| `down` | `Boolean` | "Has the user reached the bottom since the last rep?" |
| `reps` | `Int` | Running rep count for the current match. |
| `reset()` | function | Sets `reps = 0` and `down = false`. Called by the Finish button. |
| `analyze(proxy)` | override | Runs once per camera frame (see below). |
| `elbowAngle(pose)` | private function | Computes the averaged elbow angle (see Section 7). |
| `DOWN`, `UP` | `companion const` | `90.0` and `155.0`. |

**`analyze` step by step**
1. `proxy.image ?: return proxy.close()`: get the raw camera image; if missing, release the frame and stop.
2. `InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)`: wraps the frame and tells ML Kit how many degrees to rotate it to be upright.
3. `detector.process(...)`: starts asynchronous detection.
4. `addOnSuccessListener`: compute angle → run state machine → post `(angle, reps)` to the main thread. If the angle is `null`, the state machine is skipped.
5. `addOnCompleteListener { proxy.close() }`: **always** release the frame, on success or failure. If you forget, the camera stalls after a couple of frames.

`@androidx.annotation.OptIn(ExperimentalGetImage::class)` acknowledges that `ImageProxy.image` is an experimental CameraX API.

`elbowAngle` contains a nested helper `side(shoulderId, elbowId, wristId)` which returns the angle for one arm or `null`. The two results are combined with `listOfNotNull(l, r).takeIf { it.isNotEmpty() }?.average()`: average of valid values, or `null` if there are none.

### 10.8 `Ranks.kt`

| Member | Purpose |
|---|---|
| `tiers` | `List<Pair<String, Int>>`: rank name and the XP threshold. |
| `DAILY_GOAL = 30` | Reps needed for a win. |
| `prefs(c)` | Opens the `royale` `SharedPreferences` (private). |
| `xp(c)`, `wins(c)` | Read saved values (default 0). |
| `addSession(c, reps)` | Adds `reps × 10` to `xp`, and `+1` to `wins` if `reps >= DAILY_GOAL`, then `apply()`. |
| `info(xp)` | Returns `Triple(rank, progress, xpToNext)` (Section 8.3). |

It is a Kotlin `object`, so it is a singleton with no instance to create.

### 10.9 `MainActivity.kt`

**Top-level `Gold`:** `private val Gold = Color(0xFFF2A900)`, reused across the file.

**`class MainActivity : ComponentActivity`:** `onCreate` calls `setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Gold)) { Surface { App() } } }`. This is the whole Activity.

**`@Composable fun App()`**

| Step | What it does |
|---|---|
| `granted` state | Initialized from `ContextCompat.checkSelfPermission(...)`. |
| `launcher` | `rememberLauncherForActivityResult(RequestPermission())`; its result sets `granted`. |
| `LaunchedEffect(Unit)` | Auto-requests permission once when the screen first appears. |
| `if (!granted) {...; return}` | Shows the permission button and stops composing the rest. |
| `reps`, `angle` | `mutableIntStateOf` / `mutableStateOf<Int?>`; updated by the analyzer callback. |
| `xp`, `wins` | Loaded from `Ranks` once; reloaded after Finish. |
| `won` | Controls the banner. |
| `analyzer` | `remember { PushupAnalyzer { a, r -> angle = a; reps = r } }`: one instance for the screen's life. |
| `val (rank, progress, toNext) = Ranks.info(xp)` | Destructures the `Triple` for the top HUD. |
| `Box` | Stacks camera (bottom layer), top HUD, bottom HUD. |

**Finish button handler**
```kotlin
Ranks.addSession(ctx, reps)
xp = Ranks.xp(ctx); wins = Ranks.wins(ctx)
won = reps >= Ranks.DAILY_GOAL
analyzer.reset(); reps = 0
```

**`@Composable fun CameraView(analyzer)`**
1. Gets the lifecycle owner and creates a single-thread `Executor` (`remember`).
2. `AndroidView(factory = ...)` hosts the classic `PreviewView` inside Compose.
3. `ProcessCameraProvider.getInstance(c)` returns a future; the listener runs on the main executor once the camera is ready.
4. Builds `Preview` and connects it to `view.surfaceProvider`.
5. Builds `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` and attaches the analyzer on the executor.
6. `provider.unbindAll()` then `bindToLifecycle(owner, DEFAULT_FRONT_CAMERA, preview, analysis)`.
7. Returns the `PreviewView`.

---

# PART D: SMALL DETAILS

## 11. Threading and lifecycle

| Thread | What runs there |
|---|---|
| **Main** | All Compose UI, ML Kit success/complete listeners, the `Handler.post` updates, the Finish handler. |
| **Analysis executor** (single thread) | Only `analyze()`, which hands the frame to ML Kit and returns. |
| **ML Kit internal threads** | The actual neural-network inference. |

- `reps` and `down` are written only inside the success listener (main thread) and `reset()` (main thread). They are safe **by construction**, with no locks.
- ML Kit task listeners added without an executor already run on the main thread, so the `Handler.post` is technically redundant. It is harmless and makes the thread contract explicit.
- `STRATEGY_KEEP_ONLY_LATEST` drops old frames if analysis is slower than the camera, so lag does not pile up.
- `bindToLifecycle` ties the camera to the Activity: it starts on start and stops on stop automatically. `unbindAll()` before binding prevents duplicate bindings.
- Analysis frames default to a small resolution (around 640×480), which is plenty for pose detection and keeps it fast.
- **Not cleaned up:** the executor and detector are never closed. A tidy fix:

```kotlin
// PushupAnalyzer.kt
fun close() = detector.close()

// MainActivity.kt, inside CameraView
DisposableEffect(Unit) { onDispose { executor.shutdown(); analyzer.close() } }
```

## 12. Permission flow
1. First composition: `checkSelfPermission` → `granted`.
2. `LaunchedEffect(Unit)` fires `launcher.launch(CAMERA)` once if not granted.
3. The user taps Allow → `granted = true` → screen recomposes into the main UI.
4. The user taps Deny → the button screen stays; tapping it re-launches the request.
5. After two denials (or "Don't ask again"), Android stops showing the dialog. The button will do nothing visible. The user must enable the permission in **Settings ▸ Apps ▸ Pushup Royale ▸ Permissions**.

## 13. Compose details
- **State:** `mutableIntStateOf` / `mutableStateOf` + `var x by remember` means that writing to `x` redraws everything that reads it.
- **`remember`** keeps values across recompositions, but **not** across Activity recreation (see Section 14).
- **Early `return`** from a composable is allowed. It is used to skip the main UI when permission is missing.
- **Modifier order matters.** In the top HUD: `fillMaxWidth().background(...).statusBarsPadding().padding(12.dp)`. Background comes first so the dark panel extends under the status bar and behind the padding.
- **`Modifier.align(Alignment.BottomCenter)`** is only available inside `Box`, which is why the bottom HUD is a direct child of the `Box`.
- **`LinearProgressIndicator(progress = { progress })`:** the lambda form (Material 3 1.2+) reads the value lazily and avoids needless recomposition.
- **`AndroidView` factory** runs once. `PreviewView` is not rebuilt on recomposition.
- **`Spacer(Modifier.weight(1f))`** in a `Row` pushes the next item to the far end.
- **`Color(0xAA000000)`:** the first two hex digits are **alpha**, then red-green-blue.
- **`sp` vs `dp`:** `sp` for text (respects user font-size setting), `dp` for layout sizes.
- **`LaunchedEffect(Unit)`** runs once when it enters the composition.
- **Edge-to-edge** is not enabled, so `statusBarsPadding()` / `navigationBarsPadding()` are mostly no-ops. They are included so the HUD is safe if edge-to-edge is turned on later.

## 14. Known limitations and quirks (be aware)

1. **Never compiled or run.** Expect to fix a small issue or two on first sync.
2. **No form validation.** Any elbow bend-and-extend counts, including biceps curls or dips. There is no check that the body is horizontal.
3. **Fixed thresholds** (90° / 155°). They are not adapted to the user, camera distance, or style.
4. **Front camera only.** No switch to the back camera.
5. **`DAILY_GOAL` is not daily.** It is per match. There is no date tracking, so several matches in one day can each earn a win.
6. **Banner persists.** `won` is only updated on Finish, so the "Winner" banner stays visible during the next match until the next Finish tap.
7. **Starting bent counts a rep.** If you begin with elbows below 90° and then extend, one rep is counted.
8. **In-progress reps are lost** if the Activity is recreated (dark-mode switch, language change, process death). Fix: `rememberSaveable`, or a ViewModel.
9. **Executor and detector are not closed** (Section 11).
10. **No skeleton overlay.** You cannot visually confirm tracking; use the elbow readout.
11. **No sound or haptics** on a rep.
12. **No anti-cheat and no history.** Progress is local, editable by clearing data, and only totals are kept.
13. **No launcher icon, no release build setup** (no minify, signing, or ProGuard rules).
14. **ML Kit artifact is a beta version.** Check for a newer release.
15. **Permission dead-end** after "Don't ask again" (Section 12).
16. **Orientation locked** to portrait.

## 15. Tuning cheat sheet

| I want to... | Change | Where |
|---|---|---|
| Count shallower pushups | Raise `DOWN` to `100`–`110` | `PushupAnalyzer.kt` companion |
| Require less lockout at the top | Lower `UP` to `145` | `PushupAnalyzer.kt` companion |
| Stop double counting | Widen the gap (lower `DOWN`, raise `UP`) or add a minimum time between reps (e.g. 400 ms) | `PushupAnalyzer.kt` |
| Reduce shaky detection | Raise the `0.6f` confidence to `0.7f`, or smooth the angle (e.g. exponential moving average, α ≈ 0.4) | `PushupAnalyzer.kt` |
| More accurate (slower) pose model | Swap to `com.google.mlkit:pose-detection-accurate` and use `AccuratePoseDetectorOptions` | `app/build.gradle.kts`, analyzer |
| Change the win goal | `DAILY_GOAL` | `Ranks.kt` |
| Change XP per rep | `reps * 10` in `addSession` | `Ranks.kt` |
| Change rank names or thresholds | `tiers` list | `Ranks.kt` |
| Use the back camera | `DEFAULT_FRONT_CAMERA` → `DEFAULT_BACK_CAMERA` | `MainActivity.kt` |
| Allow landscape | Remove `screenOrientation` | `AndroidManifest.xml` |
| Change the accent color | `Gold` | `MainActivity.kt` |
| Rename the app | `android:label` | `AndroidManifest.xml` |
| New package id | `namespace` and `applicationId` (and move the source folder and `package` lines) | Gradle + sources |

## 16. Build, run, troubleshoot

### Run steps
1. Install **Android Studio Koala (2024.1.1) or newer** (it bundles JDK 17).
2. Unzip `PushupRoyale.zip`, then **File ▸ Open** the `PushupRoyale` folder.
3. Let **Gradle sync** finish (needs internet the first time to download libraries).
4. On a real phone: **Settings ▸ About phone ▸ tap Build number 7 times**, then **Developer options ▸ USB debugging**. Plug in, select the device, press **Run ▶**.
5. Allow the camera. Prop the phone on the floor and do pushups **side-on** so your arm is fully in view.
6. Command-line debug APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Sync says Gradle wrapper missing | Zip has no `gradlew` or `gradle/wrapper` | Let Android Studio download the suggested Gradle, or install Gradle 8.7+ and run `gradle wrapper --gradle-version 8.7` in the project root. |
| "Unsupported class file / JDK" error | Wrong JDK | Use JDK 17 (Settings ▸ Build Tools ▸ Gradle ▸ Gradle JDK). |
| Compose compiler / Kotlin version mismatch | Versions changed | Keep Kotlin `1.9.24` with compiler extension `1.5.14`. |
| `UnsafeOptInUsageError` lint error | Missing opt-in | Keep `@androidx.annotation.OptIn(ExperimentalGetImage::class)` on `analyze`. |
| Black camera preview | Permission denied, emulator with no virtual camera, or another app using the camera | Grant permission; use a real phone; close other camera apps. |
| Elbow shows `--` | Arms not confidently visible | Step back, add light, film side-on, keep the whole arm in frame. |
| Reps not counting | Elbow never gets below 90° | Watch the readout, then raise `DOWN` (Section 15). |
| Counts extra reps | Angle jitter | Widen thresholds or smooth the angle. |
| Permission button does nothing | "Don't ask again" was chosen | Enable camera in system Settings. |
| Need to reset progress | | `adb shell pm clear com.example.pushuproyale` or clear app data. |

**Wrapper properties** (`gradle/wrapper/gradle-wrapper.properties`) if you create the wrapper by hand:
```text
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## 17. Manual test checklist

- [ ] Fresh install shows the camera permission dialog.
- [ ] Deny → button remains; Allow → live preview appears.
- [ ] Arms straight in frame → elbow readout around 160°–180°.
- [ ] One slow, full pushup → counter goes up by exactly **1**.
- [ ] A half pushup (elbow stays above 90°) → no count.
- [ ] Leave the frame → readout shows `--`; returning does not break counting.
- [ ] Tap **Finish** → XP rises by reps × 10; counter resets to 0.
- [ ] Finish with 30+ reps → banner appears and wins increases by 1.
- [ ] Kill and relaunch → XP and wins persist; reps start at 0.
- [ ] Background and foreground → camera resumes.
- [ ] Rank boundary: finish enough reps to cross 500 XP → rank changes to Silver, bar resets.

## 18. Roadmap and extension prompts

Paste any of these after pasting the master prompt, or along with the source from Appendix A.

| Feature | Prompt to give the AI |
|---|---|
| Skeleton overlay | "Draw the detected shoulder, elbow, and wrist landmarks and connecting lines over the PreviewView using a Compose Canvas, correctly scaled and mirrored for the front camera." |
| Haptics + sound | "Vibrate briefly and play a short tone each time a rep is counted. Add a mute toggle." |
| Real daily goal + streak | "Store the last win date. Make the goal truly daily, add a day streak counter, and show it in the top HUD." |
| History | "Add Room. Save every finished match (date, reps, XP) and add a History screen with a weekly bar chart." |
| Form checking | "Add a body-line check using shoulder–hip–ankle angle (about 160°–180°). Only count a rep when the body is straight, and show a warning when it is not." |
| Calibration | "Add a 3-rep calibration step that measures the user's top and bottom elbow angles and sets DOWN and UP automatically." |
| Settings | "Add a settings screen for goal, thresholds, camera choice, and sound, stored in DataStore." |
| Squads | "Add Firebase Auth and Firestore. Let users join a squad by code and show a squad leaderboard of weekly reps." |
| Reminders | "Use WorkManager to send a daily reminder notification at a user-chosen time." |
| State safety | "Move match state into a ViewModel and use rememberSaveable so reps survive configuration changes." |

## 19. Branding note
The name **Pushup Royale** and the "Pushup Dinner" wording are a playful nod to battle-royale games. The rank ladder (Bronze … Conqueror, including Crown and Ace) mirrors a well-known mobile battle-royale ladder. That is fine for a personal project. **If you plan to publish**, do not use the PUBG name, logos, or assets, and consider renaming the ranks and the "Winner Winner" phrasing to avoid confusion. This is not legal advice.

## 20. Glossary

| Term | Meaning |
|---|---|
| **AGP** | Android Gradle Plugin: the build tool plugin for Android. |
| **BOM** | "Bill of Materials": one version number that aligns many libraries. |
| **CameraX** | Jetpack library that simplifies camera preview and frame analysis. |
| **Compose** | Android's modern declarative UI toolkit written in Kotlin. |
| **Hysteresis** | Using two different thresholds (90° and 155°) to prevent flicker. |
| **HUD** | Heads-up display: the info panels over the camera. |
| **ImageProxy** | A single camera frame handed to the analyzer; must be closed. |
| **Landmark** | One tracked body point (for example, the left elbow). |
| **Lifecycle** | Android's start/stop/destroy states for an Activity. |
| **ML Kit** | Google's on-device machine-learning SDK. |
| **Pose detection** | AI that finds body joints in an image (33 points here). |
| **Recomposition** | Compose redrawing only the parts affected by changed state. |
| **SharedPreferences** | Simple key-value storage on the device. |
| **STREAM_MODE** | ML Kit mode optimized for continuous video frames. |
| **XP** | Experience points: 10 per rep here. |

---

# APPENDIX A: COMPLETE SOURCE CODE

Copied verbatim from the project. Create each file at the path shown in its heading.

### `settings.gradle.kts`

```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "PushupRoyale"
include(":app")
```

### `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

### `gradle.properties`

```properties
android.useAndroidX=true
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

### `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.pushuproyale"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pushuproyale"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Compose BOM — locks all compose library versions
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    // Activity + Lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit Pose Detection (bundled, offline)
    implementation("com.google.mlkit:pose-detection:18.0.0-beta4")
}
```

### `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-feature android:name="android.hardware.camera.front" android:required="false" />
    <application
        android:label="Pushup Royale"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.App">
        <activity android:name=".MainActivity" android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### `app/src/main/res/values/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:windowTranslucentStatus">false</item>
        <item name="android:windowTranslucentNavigation">false</item>
    </style>
</resources>
```

### `app/src/main/java/com/example/pushuproyale/PushupAnalyzer.kt`

```kotlin
package com.example.pushuproyale

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan2

/* ── Data models ─────────────────────────────────────────── */

data class Point2D(val x: Float, val y: Float)

data class ArmBones(
    val shoulder: Point2D,
    val elbow: Point2D,
    val wrist: Point2D,
    val isLeft: Boolean,
    val angle: Double
)

data class PushupFeedback(
    val angle: Int?,
    val reps: Int,
    val isDown: Boolean,
    val arms: List<ArmBones>,
    val imageWidth: Int,
    val imageHeight: Int,
    val isRepJustCompleted: Boolean,
    val state: PushupState
)

enum class PushupState {
    WAITING_FOR_TOP,
    READY_TOP,
    DOWN
}

/* ── Analyzer ────────────────────────────────────────────── */

/**
 * Counts pushup reps via ML Kit pose detection.
 *
 * State machine: WAITING_FOR_TOP → READY_TOP → DOWN → READY_TOP (rep counted)
 *
 * The start-gate (WAITING_FOR_TOP) prevents false reps when the user starts
 * with bent arms.  EMA smoothing (α = 0.4) reduces angle jitter.  A 400 ms
 * cooldown between reps prevents double-counting from a single motion.
 */
class PushupAnalyzer(
    private val onUpdate: (PushupFeedback) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val main = Handler(Looper.getMainLooper())

    @Volatile private var state = PushupState.WAITING_FOR_TOP
    @Volatile private var reps = 0
    private var lastRepTimeMs = 0L
    private var smoothedAngle: Double? = null

    var downThreshold: Double = DOWN
    var upThreshold: Double = UP

    fun reset() {
        reps = 0
        state = PushupState.WAITING_FOR_TOP
        smoothedAngle = null
        lastRepTimeMs = 0L
    }

    fun close() {
        try { detector.close() } catch (_: Exception) { /* best effort */ }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val img = proxy.image
        if (img == null) {
            proxy.close()
            return
        }

        val rot = proxy.imageInfo.rotationDegrees
        val rotated = rot == 90 || rot == 270
        val w = if (rotated) img.height else img.width
        val h = if (rotated) img.width else img.height

        detector.process(InputImage.fromMediaImage(img, rot))
            .addOnSuccessListener { pose ->
                val arms = extractArms(pose)
                val raw = arms.map { it.angle }.takeIf { it.isNotEmpty() }?.average()

                var justFinished = false
                val finalAngle = if (raw != null) {
                    // EMA smoothing
                    val s = smoothedAngle?.let { prev ->
                        prev * (1.0 - EMA_ALPHA) + raw * EMA_ALPHA
                    } ?: raw
                    smoothedAngle = s

                    val now = SystemClock.uptimeMillis()
                    when (state) {
                        PushupState.WAITING_FOR_TOP -> {
                            if (s >= upThreshold) state = PushupState.READY_TOP
                        }
                        PushupState.READY_TOP -> {
                            if (s <= downThreshold) state = PushupState.DOWN
                        }
                        PushupState.DOWN -> {
                            if (s >= upThreshold && now - lastRepTimeMs >= MIN_REP_INTERVAL_MS) {
                                reps++
                                lastRepTimeMs = now
                                justFinished = true
                                state = PushupState.READY_TOP
                            }
                        }
                    }
                    s
                } else {
                    smoothedAngle = null
                    null
                }

                val fb = PushupFeedback(
                    angle = finalAngle?.toInt(),
                    reps = reps,
                    isDown = state == PushupState.DOWN,
                    arms = arms,
                    imageWidth = w,
                    imageHeight = h,
                    isRepJustCompleted = justFinished,
                    state = state
                )
                main.post { onUpdate(fb) }
            }
            .addOnCompleteListener { proxy.close() }
    }

    /* ── Landmark extraction ────────────────────────────── */

    private fun extractArms(pose: Pose): List<ArmBones> {
        val out = mutableListOf<ArmBones>()

        fun arm(sId: Int, eId: Int, wId: Int, left: Boolean) {
            val s = pose.getPoseLandmark(sId) ?: return
            val e = pose.getPoseLandmark(eId) ?: return
            val w = pose.getPoseLandmark(wId) ?: return
            if (minOf(s.inFrameLikelihood, e.inFrameLikelihood, w.inFrameLikelihood)
                < MIN_CONFIDENCE) return

            val r = atan2(
                (w.position.y - e.position.y).toDouble(),
                (w.position.x - e.position.x).toDouble()
            ) - atan2(
                (s.position.y - e.position.y).toDouble(),
                (s.position.x - e.position.x).toDouble()
            )
            val deg = abs(Math.toDegrees(r))
            val angle = if (deg > 180) 360 - deg else deg

            out += ArmBones(
                shoulder = Point2D(s.position.x, s.position.y),
                elbow    = Point2D(e.position.x, e.position.y),
                wrist    = Point2D(w.position.x, w.position.y),
                isLeft   = left,
                angle    = angle
            )
        }

        arm(PoseLandmark.LEFT_SHOULDER,  PoseLandmark.LEFT_ELBOW,  PoseLandmark.LEFT_WRIST,  true)
        arm(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, false)
        return out
    }

    companion object {
        const val DOWN = 90.0
        const val UP   = 150.0
        const val MIN_REP_INTERVAL_MS = 400L
        const val MIN_CONFIDENCE = 0.6f
        private const val EMA_ALPHA = 0.4
    }
}
```

### `app/src/main/java/com/example/pushuproyale/Ranks.kt`

```kotlin
package com.example.pushuproyale

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Battle-royale progression with daily streak tracking and multi-period analytics.
 *
 * 1 rep = 10 XP. 8 ranks from Bronze → Conqueror.
 * History is persisted locally in JSON: "YYYY-MM-DD" -> reps.
 */
object Ranks {

    data class Tier(val name: String, val xp: Int, val emoji: String)

    val tiers = listOf(
        Tier("Bronze",    0,      "🥉"),
        Tier("Silver",    500,    "🥈"),
        Tier("Gold",      1_500,  "🥇"),
        Tier("Platinum",  3_500,  "💎"),
        Tier("Diamond",   7_000,  "💠"),
        Tier("Crown",     12_000, "👑"),
        Tier("Ace",       20_000, "🃏"),
        Tier("Conqueror", 35_000, "⚔️")
    )

    const val DEFAULT_GOAL = 30
    const val DEFAULT_DOWN_ANGLE = 90f
    const val DEFAULT_UP_ANGLE = 150f

    /* ── SharedPreferences helpers ───────────────────────── */

    private fun prefs(c: Context) =
        c.getSharedPreferences("royale", Context.MODE_PRIVATE)

    fun xp(c: Context): Int   = prefs(c).getInt("xp", 0)
    fun wins(c: Context): Int = prefs(c).getInt("wins", 0)

    fun dailyGoal(c: Context): Int = prefs(c).getInt("daily_goal", DEFAULT_GOAL)
    fun setDailyGoal(c: Context, goal: Int) {
        prefs(c).edit().putInt("daily_goal", goal.coerceIn(5, 500)).apply()
    }

    fun downAngle(c: Context): Float = prefs(c).getFloat("down_angle", DEFAULT_DOWN_ANGLE)
    fun setDownAngle(c: Context, angle: Float) {
        prefs(c).edit().putFloat("down_angle", angle.coerceIn(60f, 120f)).apply()
    }

    fun upAngle(c: Context): Float = prefs(c).getFloat("up_angle", DEFAULT_UP_ANGLE)
    fun setUpAngle(c: Context, angle: Float) {
        prefs(c).edit().putFloat("up_angle", angle.coerceIn(130f, 175f)).apply()
    }

    fun soundEnabled(c: Context): Boolean = prefs(c).getBoolean("sound_enabled", true)
    fun setSoundEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun hapticsEnabled(c: Context): Boolean = prefs(c).getBoolean("haptics_enabled", true)
    fun setHapticsEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun defaultLensFacing(c: Context): Int = prefs(c).getInt("default_lens", 0) // 0 = front, 1 = back
    fun setDefaultLensFacing(c: Context, lens: Int) {
        prefs(c).edit().putInt("default_lens", lens).apply()
    }

    fun totalSessions(c: Context): Int = prefs(c).getInt("total_sessions", 0)
    fun bestStreak(c: Context): Int = prefs(c).getInt("best_streak", 0)

    fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayStr(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    /** Active streak in days. Resets to 0 if user skipped a day. */
    fun streak(c: Context): Int {
        val p = prefs(c)
        val last = p.getString("last_date", null) ?: return 0
        return if (last == todayStr() || last == yesterdayStr()) {
            p.getInt("streak", 0)
        } else 0
    }

    /** Total reps banked today across all sessions. */
    fun todayReps(c: Context): Int {
        val p = prefs(c)
        val last = p.getString("last_date", null) ?: return 0
        return if (last == todayStr()) p.getInt("today_reps", 0) else 0
    }

    /** Bank a finished match. Updates XP, wins, streak, and today's reps. */
    fun addSession(c: Context, reps: Int) {
        if (reps <= 0) return
        val p = prefs(c)
        val today = todayStr()
        val yest  = yesterdayStr()
        val last  = p.getString("last_date", null)
        val curStreak   = p.getInt("streak", 0)
        val curToday    = if (last == today) p.getInt("today_reps", 0) else 0

        val newStreak = when (last) {
            today -> maxOf(curStreak, 1)       // same day — keep
            yest  -> curStreak + 1             // consecutive — bump
            else  -> 1                         // gap — restart
        }
        val best = maxOf(p.getInt("best_streak", 0), newStreak)
        val curSessions = p.getInt("total_sessions", 0) + 1
        val goal = dailyGoal(c)

        // Update history JSON
        val histJson = try {
            JSONObject(p.getString("history_json", "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val existingDayReps = histJson.optInt(today, 0)
        histJson.put(today, existingDayReps + reps)

        p.edit()
            .putInt("xp",             xp(c) + reps * 10)
            .putInt("wins",           wins(c) + if (reps >= goal) 1 else 0)
            .putInt("streak",         newStreak)
            .putInt("best_streak",    best)
            .putInt("total_sessions", curSessions)
            .putString("last_date",   today)
            .putInt("today_reps",     curToday + reps)
            .putString("history_json", histJson.toString())
            .apply()
    }

    /* ── Multi-Period Analytics ───────────────────────────── */

    data class DayData(val label: String, val dateStr: String, val reps: Int, val isToday: Boolean)

    data class MonthReport(
        val monthName: String,
        val totalReps: Int,
        val targetReps: Int,
        val activeDays: Int,
        val restDays: Int,
        val daysInMonth: Int,
        val dailyProgress: List<DayData>
    )

    data class YearReport(
        val year: Int,
        val totalReps: Int,
        val totalSessions: Int,
        val monthlyTotals: List<Pair<String, Int>>
    )

    fun getHistoryMap(c: Context): Map<String, Int> {
        val jsonStr = prefs(c).getString("history_json", "{}") ?: "{}"
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optInt(key, 0)
            }
        } catch (_: Exception) {}
        return map
    }

    /** Last 7 days progress for weekly bar chart. */
    fun getWeekReport(c: Context): List<DayData> {
        val history = getHistoryMap(c)
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfLabel = SimpleDateFormat("EEE", Locale.US)
        val today = todayStr()

        val list = mutableListOf<DayData>()
        // 7 days ending today
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = sdfDate.format(dateCal.time)
            val label = sdfLabel.format(dateCal.time).take(3)
            val reps = history[dateStr] ?: 0
            list.add(DayData(label, dateStr, reps, dateStr == today))
        }
        return list
    }

    /** Month statistics for donut chart and progress. */
    fun getMonthReport(c: Context): MonthReport {
        val history = getHistoryMap(c)
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = todayStr()

        val dailyList = mutableListOf<DayData>()
        var total = 0
        var active = 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (d in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, d)
            }
            val dateStr = sdf.format(dayCal.time)
            val reps = history[dateStr] ?: 0
            if (reps > 0) active++
            total += reps
            dailyList.add(DayData("$d", dateStr, reps, dateStr == today))
        }

        val goal = dailyGoal(c)
        val target = goal * daysInMonth
        val rest = (daysInMonth - active).coerceAtLeast(0)

        return MonthReport(
            monthName = monthName,
            totalReps = total,
            targetReps = target,
            activeDays = active,
            restDays = rest,
            daysInMonth = daysInMonth,
            dailyProgress = dailyList
        )
    }

    /** 12-month report for the current year. */
    fun getYearReport(c: Context): YearReport {
        val history = getHistoryMap(c)
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val totals = MutableList(12) { 0 }

        var yearlyTotal = 0
        history.forEach { (dateStr, reps) ->
            if (dateStr.startsWith("$curYear-")) {
                val parts = dateStr.split("-")
                if (parts.size >= 2) {
                    val m = parts[1].toIntOrNull() ?: 1
                    if (m in 1..12) {
                        totals[m - 1] += reps
                        yearlyTotal += reps
                    }
                }
            }
        }

        return YearReport(
            year = curYear,
            totalReps = yearlyTotal,
            totalSessions = totalSessions(c),
            monthlyTotals = monthNames.zip(totals)
        )
    }

    fun clearAllData(c: Context) {
        prefs(c).edit().clear().apply()
    }

    /* ── Rank info ───────────────────────────────────────── */

    data class RankInfo(
        val name: String,
        val emoji: String,
        val progress: Float,
        val xpToNext: Int,
        val tierIndex: Int
    )

    fun info(xp: Int): RankInfo {
        val i = tiers.indexOfLast { xp >= it.xp }
        val tier = tiers[i]
        if (i == tiers.lastIndex) {
            return RankInfo(tier.name, tier.emoji, 1f, 0, i)
        }
        val lo = tier.xp
        val hi = tiers[i + 1].xp
        return RankInfo(
            name     = tier.name,
            emoji    = tier.emoji,
            progress = (xp - lo).toFloat() / (hi - lo),
            xpToNext = hi - xp,
            tierIndex = i
        )
    }
}
```

### `app/src/main/java/com/example/pushuproyale/MainActivity.kt`

```kotlin
package com.example.pushuproyale

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/* ─────────────────────────────────────────────────────────────
   LIQUID GLASS DESIGN TOKENS
   ───────────────────────────────────────────────────────────── */

private val Gold           = Color(0xFFF2A900)
private val GoldLight      = Color(0xFFFFD54F)
private val GoldDim        = Color(0x99F2A900)

private val Mint           = Color(0xFF00E676)
private val MintGlow       = Color(0x5500E676)
private val CyanAccent     = Color(0xFF00E5FF)
private val Coral          = Color(0xFFFF6B6B)

private val GlassFill      = Color(0x28FFFFFF)   // 16% white
private val GlassBorder    = Color(0x38FFFFFF)   // 22% white
private val GlassHighlight = Color(0x20FFFFFF)   // subtle specular highlight
private val GlassDark      = Color(0x58000000)   // dark glass variant

private val TextPrimary    = Color(0xFFF5F5F7)   // Apple SF-style near-white
private val TextSecondary  = Color(0xB3F5F5F7)   // 70%
private val TextTertiary   = Color(0x80F5F5F7)   // 50%

enum class AppScreen {
    WORKOUT,
    REPORTS,
    SETTINGS
}

enum class ReportPeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR
}

/* ─────────────────────────────────────────────────────────────
   MAIN ACTIVITY
   ───────────────────────────────────────────────────────────── */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold)) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF090A0F)) {
                    PushupRoyaleApp()
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   ROOT COMPOSABLE
   ───────────────────────────────────────────────────────────── */

@Composable
private fun PushupRoyaleApp() {
    val ctx = LocalContext.current

    // Navigation State
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.WORKOUT) }

    // Camera Permission
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        PermissionScreen { launcher.launch(Manifest.permission.CAMERA) }
        return
    }

    // Workout Settings from SharedPreferences
    var dailyGoal   by rememberSaveable { mutableIntStateOf(Ranks.dailyGoal(ctx)) }
    var downAngle   by rememberSaveable { mutableFloatStateOf(Ranks.downAngle(ctx)) }
    var upAngle     by rememberSaveable { mutableFloatStateOf(Ranks.upAngle(ctx)) }
    var soundOn     by rememberSaveable { mutableStateOf(Ranks.soundEnabled(ctx)) }
    var hapticsOn   by rememberSaveable { mutableStateOf(Ranks.hapticsEnabled(ctx)) }
    var lensFacing  by rememberSaveable {
        mutableIntStateOf(
            if (Ranks.defaultLensFacing(ctx) == 1) CameraSelector.LENS_FACING_BACK
            else CameraSelector.LENS_FACING_FRONT
        )
    }

    // Live Workout State
    var reps        by rememberSaveable { mutableIntStateOf(0) }
    var angle       by remember { mutableStateOf<Int?>(null) }
    var isDown      by remember { mutableStateOf(false) }
    var poseState   by remember { mutableStateOf(PushupState.WAITING_FOR_TOP) }
    var arms        by remember { mutableStateOf<List<ArmBones>>(emptyList()) }
    var srcDims     by remember { mutableStateOf(0 to 0) }
    var wonBanner   by rememberSaveable { mutableStateOf(false) }
    var repPop      by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()

    // Persisted Stats
    var xp          by remember { mutableIntStateOf(Ranks.xp(ctx)) }
    var wins        by remember { mutableIntStateOf(Ranks.wins(ctx)) }
    var streak      by remember { mutableIntStateOf(Ranks.streak(ctx)) }
    var todayReps   by remember { mutableIntStateOf(Ranks.todayReps(ctx)) }

    // Audio & Haptics
    val tone = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (_: Exception) { null }
    }
    DisposableEffect(Unit) { onDispose { try { tone?.release() } catch (_: Exception) {} } }

    val vib = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Pushup Analyzer instance
    val analyzer = remember {
        PushupAnalyzer { fb ->
            angle     = fb.angle
            reps      = fb.reps
            isDown    = fb.isDown
            poseState = fb.state
            arms      = fb.arms
            srcDims   = fb.imageWidth to fb.imageHeight

            if (fb.isRepJustCompleted) {
                scope.launch {
                    repPop = true
                    delay(200)
                    repPop = false
                }
                if (soundOn) {
                    try { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 100) } catch (_: Exception) {}
                }
                if (hapticsOn) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib?.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION") vib?.vibrate(55)
                        }
                    } catch (_: Exception) {}
                }
            }
        }.apply {
            this.downThreshold = downAngle.toDouble()
            this.upThreshold = upAngle.toDouble()
        }
    }

    // Keep analyzer thresholds synced with settings
    LaunchedEffect(downAngle, upAngle) {
        analyzer.downThreshold = downAngle.toDouble()
        analyzer.upThreshold = upAngle.toDouble()
    }

    val rankInfo = Ranks.info(xp)
    val isFront  = lensFacing == CameraSelector.LENS_FACING_FRONT

    // Root Container
    Box(Modifier.fillMaxSize()) {

        when (currentScreen) {
            AppScreen.WORKOUT -> {
                // Layer 1 — Camera View
                CameraView(analyzer, lensFacing)

                // Layer 2 — Skeleton overlay
                SkeletonOverlay(arms, srcDims.first, srcDims.second, isFront, isDown)

                // Layer 3 — Ambient glow in down state
                if (isDown) {
                    DownGlowOverlay()
                }

                // Layer 4 — Top glass HUD
                TopGlassHud(
                    rankInfo   = rankInfo,
                    xp         = xp,
                    wins       = wins,
                    streak     = streak,
                    isFront    = isFront,
                    soundOn    = soundOn,
                    hapticsOn  = hapticsOn,
                    onFlipCam  = {
                        lensFacing = if (isFront) CameraSelector.LENS_FACING_BACK
                        else CameraSelector.LENS_FACING_FRONT
                    },
                    onToggleSound   = {
                        soundOn = !soundOn
                        Ranks.setSoundEnabled(ctx, soundOn)
                    },
                    onToggleHaptics = {
                        hapticsOn = !hapticsOn
                        Ranks.setHapticsEnabled(ctx, hapticsOn)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Layer 5 — Bottom glass HUD
                BottomGlassHud(
                    reps        = reps,
                    angle       = angle,
                    isDown      = isDown,
                    poseState   = poseState,
                    armsVisible = arms.isNotEmpty(),
                    wonBanner   = wonBanner,
                    dailyGoal   = dailyGoal,
                    todayTotal  = todayReps + reps,
                    repPop      = repPop,
                    onFinish    = {
                        Ranks.addSession(ctx, reps)
                        xp        = Ranks.xp(ctx)
                        wins      = Ranks.wins(ctx)
                        streak    = Ranks.streak(ctx)
                        todayReps = Ranks.todayReps(ctx)
                        wonBanner = reps >= dailyGoal
                        analyzer.reset()
                        reps = 0
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp) // space for bottom navigation bar
                        .padding(horizontal = 16.dp)
                )
            }

            AppScreen.REPORTS -> {
                ReportsScreen(
                    ctx       = ctx,
                    todayReps = todayReps + reps,
                    dailyGoal = dailyGoal,
                    streak    = streak,
                    xp        = xp,
                    wins      = wins,
                    modifier  = Modifier.fillMaxSize().padding(bottom = 76.dp)
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    ctx              = ctx,
                    dailyGoal        = dailyGoal,
                    onGoalChange     = {
                        dailyGoal = it
                        Ranks.setDailyGoal(ctx, it)
                    },
                    downAngle        = downAngle,
                    onDownAngleChange= {
                        downAngle = it
                        Ranks.setDownAngle(ctx, it)
                    },
                    upAngle          = upAngle,
                    onUpAngleChange  = {
                        upAngle = it
                        Ranks.setUpAngle(ctx, it)
                    },
                    soundOn          = soundOn,
                    onSoundChange    = {
                        soundOn = it
                        Ranks.setSoundEnabled(ctx, it)
                    },
                    hapticsOn        = hapticsOn,
                    onHapticsChange  = {
                        hapticsOn = it
                        Ranks.setHapticsEnabled(ctx, it)
                    },
                    isFront          = isFront,
                    onCameraChange   = { front ->
                        lensFacing = if (front) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        Ranks.setDefaultLensFacing(ctx, if (front) 0 else 1)
                    },
                    onResetData      = {
                        Ranks.clearAllData(ctx)
                        xp        = 0
                        wins      = 0
                        streak    = 0
                        todayReps = 0
                        reps      = 0
                        dailyGoal = Ranks.DEFAULT_GOAL
                        downAngle = Ranks.DEFAULT_DOWN_ANGLE
                        upAngle   = Ranks.DEFAULT_UP_ANGLE
                        analyzer.reset()
                    },
                    modifier         = Modifier.fillMaxSize().padding(bottom = 76.dp)
                )
            }
        }

        // Floating Liquid Glass Navigation Bar
        LiquidGlassNavBar(
            currentScreen = currentScreen,
            onScreenSelect = { currentScreen = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

/* ─────────────────────────────────────────────────────────────
   FLOATING LIQUID GLASS NAVIGATION BAR
   ───────────────────────────────────────────────────────────── */

@Composable
private fun LiquidGlassNavBar(
    currentScreen: AppScreen,
    onScreenSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x35FFFFFF),
                        Color(0x60000000)
                    )
                )
            )
            .border(
                0.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0x45FFFFFF),
                        Color(0x10FFFFFF)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "Workout",
                emoji = "🏋️",
                selected = currentScreen == AppScreen.WORKOUT,
                onClick = { onScreenSelect(AppScreen.WORKOUT) }
            )
            NavTabItem(
                label = "Reports",
                emoji = "📊",
                selected = currentScreen == AppScreen.REPORTS,
                onClick = { onScreenSelect(AppScreen.REPORTS) }
            )
            NavTabItem(
                label = "Settings",
                emoji = "⚙️",
                selected = currentScreen == AppScreen.SETTINGS,
                onClick = { onScreenSelect(AppScreen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.35f), GoldLight.copy(alpha = 0.20f)))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    color = GoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   REPORTS & ANALYTICS SCREEN  (Daylatch Style Charts)
   ───────────────────────────────────────────────────────────── */

@Composable
private fun ReportsScreen(
    ctx: Context,
    todayReps: Int,
    dailyGoal: Int,
    streak: Int,
    xp: Int,
    wins: Int,
    modifier: Modifier = Modifier
) {
    var period by rememberSaveable { mutableStateOf(ReportPeriod.TODAY) }
    val weekData   = remember(period, todayReps) { Ranks.getWeekReport(ctx) }
    val monthData  = remember(period, todayReps) { Ranks.getMonthReport(ctx) }
    val yearData   = remember(period, todayReps) { Ranks.getYearReport(ctx) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "REPORTS & INSIGHTS",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Pushup Performance",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x20FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🔥 $streak d streak", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))

            // Period Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x18FFFFFF))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReportPeriod.values().forEach { p ->
                    val isSelected = p == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Gold else Color.Transparent)
                            .clickable { period = p }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // VIEW BASED ON PERIOD
        when (period) {
            ReportPeriod.TODAY -> {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TODAY'S COMPLETION", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(16.dp))

                            // Circular Goal Donut
                            val progress = (todayReps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier.size(170.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val stroke = 18.dp.toPx()
                                    // Track
                                    drawArc(
                                        color = Color(0x25FFFFFF),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                    // Progress
                                    drawArc(
                                        brush = Brush.sweepGradient(listOf(Gold, Mint, Gold)),
                                        startAngle = -90f,
                                        sweepAngle = progress * 360f,
                                        useCenter = false,
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$todayReps", fontSize = 42.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                    Text("/ $dailyGoal reps", fontSize = 13.sp, color = TextSecondary)
                                    Text("${(progress * 100).toInt()}%", fontSize = 14.sp, color = if (progress >= 1f) Mint else Gold, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (todayReps >= dailyGoal) "🎉 Daily target achieved! Winner dinner secured!"
                                else "${dailyGoal - todayReps} reps remaining to bank daily win.",
                                color = if (todayReps >= dailyGoal) Mint else TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    // 2x2 Stats Grid
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("⚡ Energy Burned", "${(todayReps * 0.35).toInt()} kcal", GoldLight, Modifier.weight(1f))
                        MetricCard("✨ XP Gained", "+${todayReps * 10} XP", CyanAccent, Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("🍗 Lifetime Wins", "$wins wins", Mint, Modifier.weight(1f))
                        MetricCard("🏆 All-Time XP", "$xp XP", Gold, Modifier.weight(1f))
                    }
                }
            }

            ReportPeriod.WEEK -> {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LAST 7 DAYS", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                val weekTotal = weekData.sumOf { it.reps }
                                Text("Total: $weekTotal reps", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(16.dp))

                            // Daylatch-style Weekly Bar Chart
                            WeeklyRepsBarChart(
                                days = weekData,
                                goal = dailyGoal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                item {
                    val weekTotal = weekData.sumOf { it.reps }
                    val activeDaysCount = weekData.count { it.reps > 0 }
                    val avgDaily = weekTotal / 7

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("📅 Active Days", "$activeDaysCount / 7", Mint, Modifier.weight(1f))
                        MetricCard("📈 Daily Average", "$avgDaily reps/day", CyanAccent, Modifier.weight(1f))
                    }
                }
            }

            ReportPeriod.MONTH -> {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(monthData.monthName.uppercase(), color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // Daylatch-style Donut Chart
                                MonthlyRepsDonutChart(
                                    completed = monthData.totalReps,
                                    target = monthData.targetReps,
                                    modifier = Modifier.size(136.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChartLegend(Mint, "Completed", monthData.totalReps)
                                    val remaining = (monthData.targetReps - monthData.totalReps).coerceAtLeast(0)
                                    ChartLegend(Gold, "Remaining", remaining)
                                    ChartLegend(CyanAccent, "Active Days", monthData.activeDays)
                                    ChartLegend(Color.Gray, "Rest Days", monthData.restDays)
                                }
                            }
                        }
                    }
                }

                item {
                    val consistency = if (monthData.daysInMonth > 0) {
                        (monthData.activeDays.toFloat() / monthData.daysInMonth.toFloat() * 100).toInt()
                    } else 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("🎯 Month Consistency", "$consistency%", Mint, Modifier.weight(1f))
                        MetricCard("📦 Total Banked", "${monthData.totalReps} reps", Gold, Modifier.weight(1f))
                    }
                }
            }

            ReportPeriod.YEAR -> {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("YEAR ${yearData.year}", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text("${yearData.totalReps} total reps", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))

                            // 12-Month Bar Chart
                            YearlyRepsBarChart(
                                monthlyTotals = yearData.monthlyTotals,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("🥊 Total Matches", "${yearData.totalSessions}", CyanAccent, Modifier.weight(1f))
                        MetricCard("🔥 Best Streak", "${Ranks.bestStreak(ctx)} days", GoldLight, Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

/* ── Daylatch Chart Components ──────────────────────────────── */

@Composable
private fun ChartLegend(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label: ",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            "$value",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonthlyRepsDonutChart(completed: Int, target: Int, modifier: Modifier = Modifier) {
    val total = target.coerceAtLeast(1)
    val comp = completed.coerceAtLeast(0)
    val rem = (target - completed).coerceAtLeast(0)

    Canvas(modifier) {
        val stroke = size.minDimension * 0.20f
        val sweepComp = (comp.toFloat() / total.toFloat() * 360f).coerceIn(0f, 360f)
        val sweepRem = (rem.toFloat() / total.toFloat() * 360f).coerceIn(0f, 360f)

        // Completed arc
        if (sweepComp > 0f) {
            drawArc(
                color = Mint,
                startAngle = -90f,
                sweepAngle = sweepComp - 2f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        // Remaining arc
        if (sweepRem > 0f) {
            drawArc(
                color = Gold.copy(alpha = 0.5f),
                startAngle = -90f + sweepComp,
                sweepAngle = sweepRem - 2f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun WeeklyRepsBarChart(days: List<Ranks.DayData>, goal: Int, modifier: Modifier = Modifier) {
    val maxVal = maxOf(days.maxOfOrNull { it.reps } ?: 0, goal, 10).toFloat()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { item ->
            val fraction = (item.reps.toFloat() / maxVal).coerceIn(0.04f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Value text on top
                Text(
                    if (item.reps > 0) "${item.reps}" else "-",
                    color = if (item.isToday) GoldLight else TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(18.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x18FFFFFF))
                    )
                    // Filled Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(
                                if (item.isToday) Brush.verticalGradient(listOf(Gold, GoldLight))
                                else if (item.reps >= goal) Brush.verticalGradient(listOf(Mint, CyanAccent))
                                else Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x35FFFFFF)))
                            )
                    )
                }

                Spacer(Modifier.height(6.dp))
                // Day Label
                Text(
                    item.label,
                    color = if (item.isToday) GoldLight else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun YearlyRepsBarChart(monthlyTotals: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val maxVal = maxOf(monthlyTotals.maxOfOrNull { it.second } ?: 0, 50).toFloat()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        monthlyTotals.forEach { (monthLabel, count) ->
            val fraction = (count.toFloat() / maxVal).coerceIn(0.04f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(10.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x14FFFFFF))
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .background(Brush.verticalGradient(listOf(Gold, CyanAccent)))
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    monthLabel.take(1),
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   SETTINGS & CREDITS SCREEN  (Matching Daylatch Style)
   ───────────────────────────────────────────────────────────── */

@Composable
private fun SettingsScreen(
    ctx: Context,
    dailyGoal: Int,
    onGoalChange: (Int) -> Unit,
    downAngle: Float,
    onDownAngleChange: (Float) -> Unit,
    upAngle: Float,
    onUpAngleChange: (Float) -> Unit,
    soundOn: Boolean,
    onSoundChange: (Boolean) -> Unit,
    hapticsOn: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    isFront: Boolean,
    onCameraChange: (Boolean) -> Unit,
    onResetData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        GlassPanel(
            modifier = Modifier
                .padding(32.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ Reset All Stats?", color = Coral, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("This will permanently clear all your XP, wins, streak, and workout history.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showResetDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30FFFFFF))
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            onResetData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        Text("Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("SETTINGS & PREFERENCES", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Pushup Engine", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
        }

        // WORKOUT SETTINGS CARD
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("WORKOUT TARGETS", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                    // Daily Goal Selector
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Pushup Goal", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Earn a Winner Dinner upon completion", color = TextTertiary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundStepButton("-") { onGoalChange((dailyGoal - 5).coerceAtLeast(5)) }
                            Spacer(Modifier.width(10.dp))
                            Text("$dailyGoal", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            RoundStepButton("+") { onGoalChange((dailyGoal + 5).coerceAtMost(500)) }
                        }
                    }

                    // Depth Threshold (Down Angle)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Elbow Depth Angle", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Elbow must bend below this (Default: 90°)", color = TextTertiary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundStepButton("-") { onDownAngleChange((downAngle - 5f).coerceAtLeast(60f)) }
                            Spacer(Modifier.width(10.dp))
                            Text("${downAngle.toInt()}°", color = Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            RoundStepButton("+") { onDownAngleChange((downAngle + 5f).coerceAtMost(115f)) }
                        }
                    }

                    // Lockout Threshold (Up Angle)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Elbow Lockout Angle", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Elbow must extend above this (Default: 150°)", color = TextTertiary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundStepButton("-") { onUpAngleChange((upAngle - 5f).coerceAtLeast(130f)) }
                            Spacer(Modifier.width(10.dp))
                            Text("${upAngle.toInt()}°", color = GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            RoundStepButton("+") { onUpAngleChange((upAngle + 5f).coerceAtMost(175f)) }
                        }
                    }
                }
            }
        }

        // FEEDBACK & HARDWARE CARD
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("HARDWARE & FEEDBACK", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                    // Audio Beep
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Rep Audio Beep", color = TextPrimary, fontSize = 15.sp)
                        Switch(
                            checked = soundOn,
                            onCheckedChange = onSoundChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = Gold.copy(alpha = 0.5f))
                        )
                    }

                    // Haptics
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tactile Haptic Vibration", color = TextPrimary, fontSize = 15.sp)
                        Switch(
                            checked = hapticsOn,
                            onCheckedChange = onHapticsChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = Mint, checkedTrackColor = Mint.copy(alpha = 0.5f))
                        )
                    }

                    // Default Camera
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Default Camera", color = TextPrimary, fontSize = 15.sp)
                        GlassPill(
                            text = if (isFront) "📷 Front Facing" else "📷 Back Camera",
                            onClick = { onCameraChange(!isFront) }
                        )
                    }
                }
            }
        }

        // DEVELOPER & CREDITS CARD (Prasad Dhodamani details, matching Daylatch style!)
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("ABOUT & CREDITS", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                            contentDescription = "Pushup Royale Logo",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pushup Royale", fontWeight = FontWeight.Black, fontSize = 21.sp, color = TextPrimary)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Gold.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("v2.0.0", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "Built by Prasad Dhodamani",
                                color = GoldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "\"Math over magic, disk over cloud, reps over excuses.\"",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(Modifier.height(16.dp))

                    val features = listOf(
                        "🔒 100% Offline-first — zero frames leave your device",
                        "🤖 On-Device AI Pose Detection (Google ML Kit)",
                        "💎 Apple-inspired Liquid Glass Design",
                        "🍗 Battle Royale Rank Ladder (Bronze → Conqueror)",
                        "📊 Multi-Period Analytics (Today, Week, Month, Year)",
                        "⚡ Zero Cloud Dependency — 100% Private"
                    )
                    features.forEach { feat ->
                        Text(feat, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Pushup Royale is designed & developed by Prasad Dhodamani as part of the companion fitness and habit optimization suite alongside Daylatch.",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // DATA MANAGEMENT
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("DATA & PRIVACY", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("All workout sessions, reps, XP, and streaks are stored strictly on your local device.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Coral.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Text("Reset All Workout Data", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RoundStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x25FFFFFF))
            .border(0.5.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/* ─────────────────────────────────────────────────────────────
   PERMISSION SCREEN  (Liquid Glass Style)
   ───────────────────────────────────────────────────────────── */

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulse by inf.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                contentDescription = "Pushup Royale Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, Gold.copy(alpha = pulse * 0.7f), RoundedCornerShape(28.dp))
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Camera Access Required",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pushup Royale needs your camera to\ntrack your form and count reps with on-device AI.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(32.dp))

            GlassButton(
                text = "Allow Camera Access",
                onClick = onRequest,
                accent = Gold
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   TOP GLASS HUD (WORKOUT)
   ───────────────────────────────────────────────────────────── */

@Composable
private fun TopGlassHud(
    rankInfo: Ranks.RankInfo,
    xp: Int,
    wins: Int,
    streak: Int,
    isFront: Boolean,
    soundOn: Boolean,
    hapticsOn: Boolean,
    onFlipCam: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // Row 1 — Rank + Stats
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(rankInfo.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    rankInfo.name.uppercase(),
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill("🔥", "$streak")
                    StatPill("🍗", "$wins")
                    StatPill("✨", "$xp")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 2 — Progress bar
            val animProg by animateFloatAsState(
                targetValue = rankInfo.progress,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "prog"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x20FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animProg)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Gold, GoldLight))
                        )
                )
            }

            if (rankInfo.xpToNext > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${rankInfo.xpToNext} XP to next rank",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Row 3 — Quick controls
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPill(
                    text = if (isFront) "⟲ Back" else "⟲ Front",
                    onClick = onFlipCam
                )
                Spacer(Modifier.width(8.dp))
                GlassPill(
                    text = if (soundOn) "🔊" else "🔇",
                    onClick = onToggleSound
                )
                Spacer(Modifier.width(8.dp))
                GlassPill(
                    text = if (hapticsOn) "📳" else "📴",
                    onClick = onToggleHaptics
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   BOTTOM GLASS HUD (WORKOUT)
   ───────────────────────────────────────────────────────────── */

@Composable
private fun BottomGlassHud(
    reps: Int,
    angle: Int?,
    isDown: Boolean,
    poseState: PushupState,
    armsVisible: Boolean,
    wonBanner: Boolean,
    dailyGoal: Int,
    todayTotal: Int,
    repPop: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repScale by animateFloatAsState(
        targetValue = if (repPop) 1.15f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "repScale"
    )

    GlassPanel(modifier = modifier) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = wonBanner || reps >= dailyGoal,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut()
            ) {
                GlassBadge(
                    text = "🍗 WINNER WINNER PUSHUP DINNER!",
                    color = Gold
                )
                Spacer(Modifier.height(8.dp))
            }

            // Rep counter with pop animation
            Text(
                "$reps",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    isDown -> Mint
                    reps >= dailyGoal -> Gold
                    else -> TextPrimary
                },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = repScale
                        scaleY = repScale
                    }
                    .drawBehind {
                        if (isDown) {
                            drawCircle(
                                color = MintGlow,
                                radius = size.minDimension * 0.7f
                            )
                        }
                    },
                letterSpacing = (-2).sp
            )

            // State indicator
            val stateText: String
            val stateColor: Color
            when {
                !armsVisible -> {
                    stateText = "Position yourself side-on to the camera"
                    stateColor = TextTertiary
                }
                poseState == PushupState.WAITING_FOR_TOP -> {
                    stateText = "Straighten arms to begin  •  ${angle ?: "--"}°"
                    stateColor = GoldDim
                }
                isDown -> {
                    stateText = "⬆  Push up!  •  ${angle ?: "--"}°"
                    stateColor = Mint
                }
                else -> {
                    stateText = "⬇  Bend elbows below 90°  •  ${angle ?: "--"}°"
                    stateColor = TextSecondary
                }
            }

            Text(
                stateText,
                color = stateColor,
                fontSize = 13.sp,
                fontWeight = if (isDown) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Goal progress
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Goal $dailyGoal", color = TextTertiary, fontSize = 11.sp)
                Text("  •  ", color = TextTertiary, fontSize = 11.sp)
                Text("Today: $todayTotal reps", color = TextTertiary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            GlassButton(
                text = "Finish Match & Bank XP",
                onClick = onFinish,
                accent = Gold
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   GLASS UI PRIMITIVES
   ───────────────────────────────────────────────────────────── */

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(GlassFill, GlassDark)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(GlassBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, GlassHighlight, Color.Transparent)
                    )
                )
        )
        content()
    }
}

@Composable
private fun StatPill(emoji: String, value: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x20FFFFFF))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "$emoji $value",
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GlassPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x18FFFFFF))
            .border(0.5.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassButton(
    text: String,
    onClick: () -> Unit,
    accent: Color
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent, accent.copy(alpha = 0.8f))
                )
            )
            .border(
                0.5.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.Black,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun GlassBadge(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/* ─────────────────────────────────────────────────────────────
   DOWN-STATE AMBIENT GLOW
   ───────────────────────────────────────────────────────────── */

@Composable
private fun DownGlowOverlay() {
    val inf = rememberInfiniteTransition(label = "glow")
    val alpha by inf.animateFloat(
        initialValue = 0.08f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * 0.9f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Transparent, Mint.copy(alpha = alpha)),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }
}

/* ─────────────────────────────────────────────────────────────
   SKELETON OVERLAY
   ───────────────────────────────────────────────────────────── */

@Composable
private fun SkeletonOverlay(
    arms: List<ArmBones>,
    sourceW: Int,
    sourceH: Int,
    isFront: Boolean,
    isDown: Boolean,
    modifier: Modifier = Modifier
) {
    if (arms.isEmpty() || sourceW <= 0 || sourceH <= 0) return

    Canvas(modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        val scale = maxOf(cw / sourceW, ch / sourceH)
        val ox = (cw - sourceW * scale) / 2f
        val oy = (ch - sourceH * scale) / 2f

        fun map(p: Point2D): Offset {
            val px = ox + p.x * scale
            val py = oy + p.y * scale
            return Offset(if (isFront) cw - px else px, py)
        }

        val bone  = if (isDown) Mint else Gold
        val glow  = bone.copy(alpha = 0.30f)
        val joint = TextPrimary

        for (arm in arms) {
            val s = map(arm.shoulder)
            val e = map(arm.elbow)
            val w = map(arm.wrist)

            // Glow layer
            drawLine(glow, s, e, 18f, StrokeCap.Round)
            drawLine(glow, e, w, 18f, StrokeCap.Round)

            // Core bone line
            drawLine(bone, s, e, 5f, StrokeCap.Round)
            drawLine(bone, e, w, 5f, StrokeCap.Round)

            // Joint nodes
            drawCircle(bone, 10f, s)
            drawCircle(joint, 5f, s)
            drawCircle(bone, 10f, w)
            drawCircle(joint, 5f, w)

            // Emphasized elbow node
            val er = if (isDown) 18f else 14f
            drawCircle(glow, er + 10f, e)
            drawCircle(bone, er, e)
            drawCircle(joint, 7f, e)
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   CAMERA VIEW
   ───────────────────────────────────────────────────────────── */

@Composable
private fun CameraView(
    analyzer: PushupAnalyzer,
    lensFacing: Int,
    modifier: Modifier = Modifier
) {
    val owner    = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try { executor.shutdown() } catch (_: Exception) {}
            try { analyzer.close() } catch (_: Exception) {}
        }
    }

    DisposableEffect(lensFacing) {
        onDispose {
            try { provider?.unbindAll() } catch (_: Exception) {}
        }
    }

    key(lensFacing) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { c ->
                val view = PreviewView(c).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val future = ProcessCameraProvider.getInstance(c)
                future.addListener({
                    try {
                        val prov = future.get()
                        provider = prov
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(executor, analyzer) }

                        prov.unbindAll()
                        prov.bindToLifecycle(
                            owner,
                            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(c))
                view
            }
        )
    }
}
```

---
*End of file.*
