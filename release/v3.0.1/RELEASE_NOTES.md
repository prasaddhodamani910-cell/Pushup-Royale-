# Pushup Royale v3.0.1 Release Notes (Engine Hardening & Bug Fix)

**Author:** Prasad Dhodamani  
**Release Date:** October 16, 2026  
**Update Type:** Patch / Bug Fix Release (`3.0.1`, `versionCode: 4`)  
**Build:** `GRADLE_OPTS="-Xmx1536m -Dorg.gradle.daemon=false" gradle --no-daemon :app:assembleDebug`  
**Unit Tests:** 100% Passed (JUnit 4)

---

### Core Upgrades & Fixes (Master Improvement Spec)

1. **Robust Camera & PoseDetector Lifecycle:**
   - Added thread-safe auto-recreation (`getDetector()`) in `PushupAnalyzer` to prevent permanent detector disposal across tab transitions.
   - Refactored `CameraView` lifecycle ownership so `PushupAnalyzer` is maintained by the application root and disposed cleanly on app exit.
   - Guaranteed single in-flight inference pipeline with unbinding safety and no leaked executors.

2. **Strict & Safe Pose Validation (No Blind Approvals):**
   - Eliminated flaw where missing lower-body landmarks allowed unmeasured postures to pass.
   - Missing hip, knee, or ankle landmarks now strictly yield `isPlankStraight = false` and prompt `STEP_BACK`.
   - Reps cannot be initiated or confirmed without verified body visibility and valid geometric evidence.

3. **Modern CameraX API Upgrade:**
   - Replaced deprecated `ImageAnalysis.Builder.setTargetResolution(...)` with modern `ResolutionSelector` and `ResolutionStrategy(android.util.Size(480, 640), FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)`.
   - Applied `ResolutionSelector` consistently across both `Preview` and `ImageAnalysis` for matched aspect ratios and precise coordinate overlays.

4. **Settings Threshold Constraint Enforcement:**
   - Enforced constraint `downAngle <= upAngle - 20f` across both slider/step button controls and persistence layers.
   - Prevents impossible or contradictory configurations (e.g., down=110° and up=120°) that would produce flickering false reps.

5. **Session Banking & Win Accounting Consistency:**
   - Multi-session daily goal tracking: multiple workouts within the same day combine to reach the daily goal.
   - Prevented duplicate daily wins: achieving the daily goal awards exactly one "Winner Winner Pushup Dinner" win per day.
   - Safe data reset clears all stats cleanly and restores defaults without crashes.

6. **Automated JUnit 4 Unit Tests:**
   - `PushupGeometryTest`: Tests 3-point angle math (90°, 180°, 45°, reflex angle normalization 0..180°), straight plank validation, and strict missing-landmark rejection.
   - `RanksLogicTest`: Tests all 8 battle royale progression tiers (Bronze → Conqueror), fractional progress math, XP to next tier, monotonic XP ordering, and default constants.

---

### Release Artifacts
- `PushupRoyale-v3.0.1.apk` (Saved to `/storage/emulated/0/Download/PushupRoyale-v3.0.1.apk` and `release/v3.0.1/`)
- `version.json`
- `RELEASE_NOTES.md`
- `SHA256SUMS.txt`
