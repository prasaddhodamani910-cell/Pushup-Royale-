# Pushup Royale --- Master Improvement & Bug-Fix Prompt

**Project:** Pushup Royale\
**Repository:**
https://github.com/prasaddhodamani910-cell/Pushup-Royale-\
**Target:** Native Android app built with Kotlin, Jetpack Compose,
CameraX, and ML Kit Pose Detection\
**Device used for manual camera check:** Samsung Galaxy A17
(user-reported)\
**Goal:** Improve reliability, full-body pose tracking, rep-count
accuracy, responsiveness, lifecycle safety, and release readiness
without damaging existing features.

------------------------------------------------------------------------

## Instructions for the coding agent

You are working on an existing Android application, not starting a new
project. **Inspect the current repository and its actual code before
editing.** Treat this document as a detailed engineering task list, not
permission to rewrite the app blindly.

### Non-negotiable rules

1.  Preserve the existing Pushup Royale identity, visual design,
    navigation, workout flow, ranks, XP, streaks, reports, settings,
    sound, haptics, and local-first behavior unless a change is required
    to fix a verified defect.
2.  Do not replace working features with mockups, placeholders,
    hardcoded demo data, or a web app.
3.  Do not claim a bug is fixed, a test passed, or a build succeeded
    unless you actually performed the relevant check.
4.  Before changing code, inspect the project structure, README, Gradle
    files, Android manifest, pose analyzer, camera screen, overlay
    coordinate mapping, persistence, and existing tests.
5.  Make small, coherent changes. Avoid unnecessary dependencies and
    avoid broad rewrites.
6.  Keep processing on-device where possible. Do not upload camera
    frames, pose landmarks, or workout data to a server. Do not add
    internet permission or analytics without explicit project-owner
    approval.
7.  Preserve the ability to use the app offline after installation.
8.  Keep the UI professional and consistent with the current design.
    Respect status-bar and navigation-bar insets; avoid overlapping
    content.
9.  If requirements conflict with the existing implementation, explain
    the conflict and choose the safest minimal change.
10. At the end, report the exact files changed, the reason for each
    change, commands run, test results, build result, and remaining
    limitations.

------------------------------------------------------------------------

## Priority 0 --- Protect the current working project

-   Check the current Git status and branch before editing. Do not
    overwrite unrelated user changes.
-   Read the current source and Gradle configuration; do not assume file
    paths or class names match this prompt exactly.
-   Identify the Android Gradle Plugin, Gradle wrapper, Kotlin,
    Java/JDK, compile SDK, target SDK, min SDK, CameraX, and ML Kit
    versions.
-   Record the existing app behavior and current build status before
    making changes, if feasible.
-   Make changes in small steps so failures can be isolated and
    reverted.
-   Do not delete existing documentation, resources, screens, or
    features just to simplify the implementation.

## Priority 1 --- Fix camera and analyzer lifecycle

**Observed source-review concern:** `CameraView` appears to close a
`PushupAnalyzer` in `DisposableEffect(Unit)`, while the analyzer is
remembered in a parent composable. If the workout screen is removed and
later reopened, the same closed analyzer instance may be reused.

### Required work

1.  Trace the lifetime of `PushupAnalyzer`, its ML Kit detector, the
    camera provider, the image-analysis use case, and the executor.
2.  Ensure the analyzer is never reused after its underlying detector
    has been closed.
3.  Choose one clear ownership model:
    -   create and close the analyzer with the camera/workout screen
        lifecycle; or
    -   retain it at a stable owner level and close it only when that
        owner is destroyed.
4.  Unbind CameraX use cases before releasing resources.
5.  Ensure the `ImageProxy` is closed on every processing path,
    including exceptions, rejected frames, detector errors, and early
    returns.
6.  Prevent multiple simultaneous ML Kit inference operations if the
    analyzer is designed for single-flight processing.
7.  Handle camera permission denial, permanent denial, camera
    unavailable errors, and camera switching gracefully.
8.  Ensure front/back camera switching does not leave the previous
    camera bound or the analyzer in an invalid state.
9.  Avoid leaking executors, detector instances, callbacks, activities,
    or composables.
10. Test repeated sequences: open workout → leave workout → return →
    switch camera several times → background app → resume → finish
    session.

### Acceptance criteria

-   No crash or frozen tracking after repeatedly leaving and reopening
    the workout screen.
-   Camera resources are released when appropriate.
-   No double binding or permanently closed detector reuse.
-   The UI shows a useful recoverable error if camera initialization
    fails.

## Priority 2 --- Make pose validation strict and safe

**Observed source-review concern:** the current body-alignment logic
appears to treat a missing plank angle as straight, equivalent to:

``` kotlin
val isPlankStraight = plankAngle == null ||
    plankAngle >= PLANK_MIN_STRAIGHT_ANGLE
```

This can allow an unmeasured body alignment to pass a form check.

### Required work

1.  Audit every required landmark: shoulders, elbows, wrists, hips,
    knees, ankles, and any head landmarks used for guidance.
2.  Distinguish between:
    -   landmark missing;
    -   landmark present but low-confidence;
    -   body partly outside the frame;
    -   a valid but imperfect pose.
3.  Never treat missing measurements as proof of good form.
4.  Use explicit tracking states and guidance for incomplete body
    visibility or unreliable measurements.
5.  Apply confidence thresholds consistently and verify their behavior
    under poor lighting, motion blur, occlusion, and different body
    positions.
6.  Do not require every landmark in every situation if the selected
    exercise mode can safely operate with a smaller, justified set.
    Document the minimum required set.
7.  If a measurement becomes unreliable during a rep, pause or reject
    that rep safely rather than manufacturing a valid measurement.
8.  Reset or reinitialize temporal state when tracking is lost long
    enough that the previous pose sequence is no longer trustworthy.
9.  Avoid noisy guidance flickering on every frame; use sensible
    temporal smoothing or hysteresis without making rep detection
    sluggish.

### Acceptance criteria

-   A missing plank angle never automatically counts as a straight body.
-   Incomplete or low-confidence tracking does not create false rep
    confirmations.
-   Guidance clearly explains what the user should adjust.
-   Brief landmark jitter does not cause unstable state changes.

## Priority 3 --- Correct full-body tracking and camera positioning

### Product requirement

The user should be able to place the phone in front of them and have the
app detect the body needed for a valid push-up. The user must not have
to show only their hands or use a camera angle that makes the movement
impossible to evaluate.

### Required work

1.  Inspect the camera preview transform, image rotation, image
    dimensions, front-camera mirroring, crop behavior, and skeleton
    overlay coordinate mapping.
2.  Verify that pose landmarks and the displayed skeleton align
    accurately in the preview in both front- and back-camera modes.
3.  Inspect `PreviewView.ScaleType.FILL_CENTER` and the image-analysis
    resolution/crop relationship. Do not assume preview coordinates and
    analysis-image coordinates are identical.
4.  Use a clear setup state that tells the user when to move farther
    back, adjust the phone angle, improve lighting, or bring missing
    body parts into frame.
5.  Determine which camera orientation is genuinely suitable for push-up
    movement measurement. Do not promise reliable front-facing detection
    if the geometry does not support it.
6.  Test whether the chosen view can see shoulders, elbows, wrists,
    hips, knees, and ankles with sufficient confidence.
7.  If a side or oblique view is technically required for reliable
    depth/extension measurement, explain this honestly in the UI and
    provide setup guidance instead of pretending that a front view can
    measure everything.
8.  Avoid detecting only arms as a complete usable body. Require the
    minimum body landmarks for the selected detection mode.
9.  Prevent skeleton points from being drawn in incorrect positions
    because of mirroring, rotation, cropping, or mismatched source
    dimensions.
10. Keep the whole body visible where practical, without making the UI
    or camera preview unusably small.

### Acceptance criteria

-   The setup guidance is based on actual detected landmarks, not
    arbitrary timers.
-   The skeleton overlay aligns with the user on the tested device and
    camera modes.
-   The app does not enter the ready state when essential landmarks are
    outside the frame.
-   The app tells the user when the selected angle cannot reliably
    support the required checks.

## Priority 4 --- Improve rep detection accuracy and prevent cheating

**Observed source-review concern:** the current anti-cheat calculation
uses shoulder-to-wrist vertical image distance as a proxy for torso
displacement. That can be view-dependent, especially when movement is
largely toward/away from the camera. This metric must be validated
rather than assumed reliable.

### Required work

1.  Map and document the complete state machine: `WAITING_FOR_READY`,
    `TOP`, `DESCENDING`, `BOTTOM`, `ASCENDING`, and any other states
    currently present.
2.  Define exactly what evidence is needed to confirm a rep:
    -   valid starting/top position;
    -   meaningful descent;
    -   adequate depth;
    -   return to a valid top position;
    -   sufficiently reliable body tracking through the movement.
3.  Reject arm-only waving, elbow bending while not performing a
    push-up, partial reps, and repeated counts from a single movement.
4.  Do not count if essential landmarks disappear during the critical
    phase, unless a documented safe fallback can prove the rep valid.
5.  Review the use of `MIN_TORSO_DISPLACEMENT_RATIO`, angle thresholds,
    `MIN_REP_INTERVAL_MS`, and EMA smoothing. Treat current constants as
    tunable hypotheses, not universally correct values.
6.  Validate the anti-cheat metric for each supported camera view. Do
    not use a single image-space metric as proof of true torso movement
    unless tests support it.
7.  Prefer a combination of valid joint angles, body geometry, landmark
    confidence, temporal sequence, and view-aware movement evidence.
8.  Avoid overly strict rules that reject normal, valid reps. The goal
    is accurate counting, not making the app unusable.
9.  Make threshold settings internally consistent. Prevent impossible or
    contradictory configurations between down and up thresholds.
10. Reset per-rep measurements when a rep is rejected, tracking is lost,
    the workout is reset, or the camera mode changes.
11. Add diagnostic logging behind a development/debug option if useful.
    Do not log images or sensitive camera data.
12. Keep a minimum rep interval as a duplicate-count safeguard, but do
    not rely on it as the main anti-cheat mechanism.

### Acceptance criteria

Test at least these cases:

-   Correct full push-up.
-   Partial descent and return.
-   Elbows bend without a real push-up.
-   Arms move while torso stays essentially still.
-   Body sags or hips rise excessively.
-   One arm or one side becomes occluded.
-   Essential landmarks leave the frame.
-   Camera view is unsuitable.
-   User pauses at the bottom.
-   User performs consecutive valid reps at different realistic speeds.
-   User stops halfway and restarts.
-   Tracking is lost and then recovered.

For each case, record expected versus actual rep count and whether the
app gave appropriate guidance. Do not claim a target accuracy percentage
without a measured test set.

## Priority 5 --- Improve responsiveness without sacrificing accuracy

### Required work

1.  Measure actual ML Kit inference time on the target Android device
    before tuning performance.
2.  Keep `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` or an equivalent
    non-backlogging strategy unless measured evidence justifies a
    change.
3.  Ensure each `ImageProxy` is closed exactly once and promptly.
4.  Avoid doing heavy work on the main thread.
5.  Avoid posting unnecessary UI updates for every tiny landmark change
    if they cause excessive recomposition.
6.  Profile the analyzer's frame rate, inference duration, main-thread
    work, memory usage, and missed frames.
7.  Review the current 480 × 640 target resolution. Choose a resolution
    that balances landmark quality and latency on the actual phone
    rather than assuming higher or lower is always better.
8.  Tune smoothing only after measuring the trade-off between jitter and
    delayed state transitions.
9.  Keep feedback timely and stable. Do not add arbitrary delays to make
    the counter look animated.
10. Handle inference exceptions without permanently stopping the
    analyzer.

### Acceptance criteria

-   Report measured average and high-percentile inference time if
    tooling allows.
-   Compare detection accuracy before and after performance changes.
-   Do not claim a fixed FPS or latency target without measurements.
-   No sustained memory growth, growing frame backlog, or UI freeze
    during a representative workout.

## Priority 6 --- Verify state, settings, and session accounting

Inspect the `Ranks` persistence and all callers.

1.  Verify that a session is banked exactly once when the user finishes
    it.
2.  Ensure reps cannot be accidentally counted twice if the finish
    action is triggered repeatedly.
3.  Confirm XP, wins, daily goal, streak, best streak, total sessions,
    today reps, and history remain consistent after app restart.
4.  Confirm a session with zero reps does not incorrectly increase wins
    or session totals.
5.  Verify that changing down/up thresholds updates the live analyzer
    immediately and does not leave stale values.
6.  Ensure down/up threshold settings cannot create contradictory
    state-machine transitions.
7.  Verify camera preference persistence if the UI claims the selection
    is saved.
8.  Test day changes, month/year boundaries, leap years, and device
    timezone changes for history and streak behavior.
9.  Ensure clearing data is explicit and cannot happen accidentally.
10. Add focused unit tests for rank progression, XP calculations, streak
    logic, daily aggregation, and report totals.
11. Review integer overflow or malformed local history JSON handling
    where relevant.
12. Make the UI refresh persisted values consistently after finishing a
    session or returning to a screen.

## Priority 7 --- UI/UX and accessibility

1.  Preserve the existing dark/gold Pushup Royale design and its current
    visual identity.
2.  Keep the rep count highly readable during movement.
3.  Make setup instructions, pose guidance, tracking errors, and
    camera-permission messages clear and actionable.
4.  Ensure status-bar and navigation-bar insets are handled correctly.
5.  Check small-screen layout, large font settings, contrast, touch
    target sizes, and text clipping.
6.  Verify all buttons, switches, camera controls, reports, and settings
    work rather than merely looking interactive.
7.  Avoid excessive animations or effects that harm camera performance
    or legibility.
8.  Ensure the app can recover from permission denial and camera
    initialization failure without trapping the user.
9.  Check portrait/landscape behavior and process recreation. If
    landscape is unsupported, handle it predictably.
10. Add content descriptions or other accessibility semantics to
    meaningful controls and graphics where appropriate.

## Priority 8 --- Privacy and security

1.  Confirm all pose inference and camera-frame processing remain
    on-device.
2.  Confirm the manifest declares only the permissions the app actually
    needs.
3.  Do not add internet permission, remote uploads, analytics,
    advertising SDKs, or accounts without explicit approval.
4.  Avoid logging camera frames, images, precise pose data, or
    unnecessary personal information.
5.  Verify local preferences and history are handled safely and that the
    clear-data action behaves as described.
6.  Review dependency versions and known warnings; do not upgrade major
    dependencies blindly.
7.  Ensure release builds do not expose debug-only controls or verbose
    sensitive logs.

## Priority 9 --- Testing and build verification

### Required checks

-   Inspect existing tests and add tests for the logic that can be
    tested without a camera.
-   Add tests for angle calculations, missing/low-confidence landmarks,
    state transitions, threshold edge cases, invalid tracking sequences,
    rep interval handling, and persistence.
-   Run the repository's available Gradle test tasks.
-   Run a debug build and, if signing/configuration permits, a release
    build.
-   Verify Android manifest merging and permissions.
-   Check compiler warnings and dependency issues.
-   If a physical device is available, test on it. If it is not
    available, state that device testing was not performed.
-   If a build fails, include the first relevant error and its cause; do
    not hide the failure or report success.
-   Do not claim that a real camera test, benchmark, or release build
    passed unless it was actually run.

### Suggested commands

Use the project's Gradle wrapper and the JDK/Gradle versions supported
by its configuration. From the repository root, inspect the available
tasks first. Typical commands may include:

``` bash
./gradlew tasks
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

Only run the tasks supported by the repository. Release assembly does
not necessarily mean the APK is signed for distribution or ready for
Play Store submission.

## Priority 10 --- Code organization and maintainability

The current `MainActivity.kt` appears large and combines many UI and
workflow responsibilities.

1.  Identify logical boundaries before moving code.
2.  Where practical, separate camera lifecycle management, workout
    screen UI, skeleton overlay, settings, reports, and
    state/persistence logic into focused files.
3.  Keep the refactor incremental and behavior-preserving.
4.  Avoid creating many tiny files that make navigation harder.
5.  Use clear names and comments for state-machine transitions and
    measurement assumptions.
6.  Remove dead code only after confirming it is unused.
7.  Keep business logic testable without launching the UI or camera.
8.  Avoid introducing a new architecture or dependency solely for
    stylistic reasons.

------------------------------------------------------------------------

## Required workflow

Follow this order:

### Phase A --- Inspect and report

-   Inspect the full repository and identify the actual relevant files.
-   Summarize architecture and current behavior.
-   List confirmed defects separately from potential risks and
    unverified assumptions.
-   Identify any existing tests and current build limitations.
-   Do not start with a large rewrite.

### Phase B --- Fix critical correctness issues

-   Address camera/analyzer lifecycle.
-   Correct missing-landmark and body-alignment validation.
-   Verify camera transform and skeleton overlay mapping.
-   Improve rep-state reset and invalid-pose handling.

### Phase C --- Validate and optimize

-   Add focused tests.
-   Run Gradle checks and build tasks.
-   Measure on-device inference performance if a physical device is
    available.
-   Tune thresholds only with documented test observations.

### Phase D --- Finish and report

Provide a concise engineering report containing:

1.  **Files changed** --- exact paths and what changed.
2.  **Bugs fixed** --- confirmed root cause and fix.
3.  **Tests run** --- exact commands and actual outcomes.
4.  **Build result** --- success/failure and relevant details.
5.  **Device verification** --- device, Android version, scenarios
    tested, and observed results, only if actually tested.
6.  **Remaining issues** --- anything not fixed or not verified.
7.  **Manual test checklist** --- practical steps for the owner to test
    on the phone.
8.  **Git diff summary** --- ensure unrelated work was not overwritten.

## Final manual test checklist

-   [ ] Grant camera permission and open workout.
-   [ ] Put the phone in the recommended position and move until the
    entire required body pose is visible.
-   [ ] Verify the skeleton follows the correct joints in the preview.
-   [ ] Hold the ready position and confirm the app does not count a rep
    prematurely.
-   [ ] Perform one slow, controlled full push-up and check the count.
-   [ ] Perform several consecutive valid reps at a comfortable pace.
-   [ ] Try an incomplete rep and an arm-only movement; neither should
    be counted as a valid rep.
-   [ ] Move a required landmark out of frame and verify counting pauses
    with clear guidance.
-   [ ] Switch camera modes and verify the preview, landmarks, and
    counting remain coherent.
-   [ ] Leave the workout screen and return; confirm the camera and
    tracking still work.
-   [ ] Background and resume the app; verify it recovers correctly.
-   [ ] Finish a session once, then verify XP, wins, streaks, and
    history.
-   [ ] Restart the app and verify persisted stats.
-   [ ] Test permission denial and camera initialization failure if
    practical.
-   [ ] Complete a representative workout while checking heat,
    responsiveness, crashes, and tracking stability.

------------------------------------------------------------------------

## Definition of done

The work is complete only when:

-   Critical lifecycle and pose-validation issues are fixed or clearly
    documented as unresolved.
-   Missing landmarks cannot silently pass required form checks.
-   Rep counting is validated against a documented set of realistic and
    invalid movements.
-   Camera preview and skeleton coordinates are checked for the
    supported camera modes.
-   Existing progression, settings, reports, and local persistence
    continue to work.
-   Tests/build commands have been run where possible and their actual
    outcomes are reported.
-   No unverified claim is presented as a successful test.
-   The app remains privacy-conscious and offline-first.

**Primary objective:** Make Pushup Royale a dependable Android workout
app that recognizes real push-ups, rejects invalid movement as safely as
practical, responds promptly, and survives normal navigation and camera
lifecycle changes---while preserving the existing app rather than
rebuilding it unnecessarily.
