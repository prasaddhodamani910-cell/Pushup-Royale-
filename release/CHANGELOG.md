# Pushup Royale — Release Changelog & Versioning Guide

## Versioning Scheme (Semantic Versioning: MAJOR.MINOR.PATCH)

Pushup Royale adheres to [Semantic Versioning 2.0.0](https://semver.org/):

1. **MAJOR (X.0.0)**:
   - **When triggered**: Breaking architectural shifts, completely new tracking algorithms, major redesigns, or foundational platform rewrites.
   - **Examples**:
     - `v2.0.0`: Apple Liquid Glass visual overhaul, Reports & Analytics (Daylatch-style charts), Settings engine, Prasad Dhodamani credits, logo integration.
     - `v3.0.0`: Full-body computer vision AI pose engine, Anti-cheat torso displacement verification, front-camera placement guidance, plank alignment detection.

2. **MINOR (x.Y.0)**:
   - **When triggered**: New features added in a backward-compatible manner (e.g., adding sound packs, custom workout routines, or export capabilities).
   - **Increment**: Second number increments (`v3.1.0`, `v3.2.0`), patch resets to 0.

3. **PATCH / BUG FIX (x.y.Z)**:
   - **When triggered**: Backward-compatible bug fixes, performance tuning, threshold adjustments, or minor visual fixes.
   - **Increment**: Third number increments (`v3.0.1`, `v3.0.2`).

---

## Release History

### [v3.0.1] — 2026-10-16 *(ENGINE HARDENING & BUG FIX)*
- **Detector Lifecycle Safety**: Thread-safe lazy `getDetector()` auto-recreation preventing permanent ML Kit disposal on tab switching.
- **Strict Plank & Landmark Validation**: Null or occluded lower-body landmarks strictly evaluate to `isPlankStraight = false` with `STEP_BACK` guidance.
- **ResolutionSelector Migration**: Replaced deprecated `setTargetResolution` with CameraX `ResolutionSelector` and `FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`.
- **Threshold Constraints**: Enforced `downAngle <= upAngle - 20f` across settings UI and storage to prevent contradictory state jitter.
- **Session Banking Accuracy**: Multi-session accumulation for daily goal; prevented duplicate "Winner Dinner" win awarding on the same day.
- **JUnit 4 Test Suite**: Automated geometry, plank integrity, and XP rank progression unit test coverage.

### [v3.0.0] — 2026-10-16 *(MAJOR UPDATE)*
- **Core Engine Rewrite**: Upgraded ML Kit tracking from arm-only elbow checking to full-body kinematic analysis (Shoulders, Elbows, Wrists, Hips, Knees, Ankles, Head/Nose).
- **Anti-Cheat Verification**: Added mandatory torso vertical displacement calculation; moving hands or bending elbows alone will not trigger pushup reps.
- **Plank Line Analysis**: Real-time Shoulder-Hip-Ankle/Knee alignment angle checking with warnings for sagging hips (< 135°) or piking hips.
- **Front-Camera Positioning Guidance**: Live guidance instructions tailored for placing phone in front of user's face.
- **Full-Body Skeleton Overlay**: Liquid Glass aesthetic overlay rendering clavicle, torso, spine, hips, legs, arms, and head with dynamic state coloring.
- **Latency Optimization**: 480x640 target resolution and atomic single in-flight inference lock.

### [v2.0.0] — 2026-10-16 *(MAJOR UPDATE)*
- **Liquid Glass Design**: Apple-inspired frosted glass aesthetic (`GlassPanel`), floating bottom navigation bar.
- **Reports & Multi-Period Analytics**: Today donut ring, 7-day weekly bar chart, monthly completed vs remaining donut chart, 12-month yearly bar chart.
- **Settings Screen**: Customizable target goal, depth angle, lockout angle, audio/haptic toggles, camera switcher, data reset.
- **Developer Credits**: Prasad Dhodamani developer details and ecosystem philosophy.
- **App Logo Integration**: Adaptive launcher icons and in-app branding.

### [v1.0.0] — 2026-10-15 *(INITIAL MVP)*
- Initial release with CameraX and ML Kit Pose Detection.
- Arm-based elbow angle tracking and 8-tier Battle Royale progression ladder.
