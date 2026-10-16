# Pushup Royale v2.0.0 Release Notes

**Author:** Prasad Dhodamani  
**Release Date:** October 16, 2026  
**Build:** `gradle :app:assembleDebug`

---

### Highlights & New Features

- **Apple Liquid Glass Design Language:**
  - Frosted translucent glass panels (`GlassPanel`), specular highlights, and fluid squircle cards.
  - Floating Liquid Glass Bottom Navigation Bar with smooth screen transitions.

- **Reports & Multi-Period Analytics (Daylatch-Style):**
  - **Today:** Circular Goal Completion Donut ring, calories burned estimate, XP gained, and daily win progress.
  - **Week:** 7-Day Weekly Bar Chart (`WeeklyRepsBarChart`) with highlighted current day and capsule bars.
  - **Month:** Monthly Donut Chart (`MonthlyRepsDonutChart`) comparing Completed vs Remaining target and Active vs Rest Days with `ChartLegend`.
  - **Year:** 12-Month Bar Chart (`YearlyRepsBarChart`), lifetime statistics, total matches, and best streak.

- **Settings & Preferences Engine:**
  - Customizable Daily Pushup Goal (default: 30 reps).
  - Configurable Elbow Depth Angle (default: 90°) and Lockout Angle (default: 150°).
  - Audio beep and tactile haptic vibration toggles.
  - Camera preference selector (Front vs Back camera).
  - Local data reset with safety confirmation dialog.

- **Developer & Credits Page (Matching Daylatch Style):**
  - Dedicated about section crediting **Prasad Dhodamani**.
  - Philosophy: *"Math over magic, disk over cloud, reps over excuses."*
  - Ecosystem details linking Daylatch & Pushup Royale companion apps.

- **Official App Logo Integration:**
  - High-resolution launcher icons across all standard densities (`mipmap-mdpi` through `mipmap-xxxhdpi`).
  - Circular and squircle adaptive launcher icons.
  - High-resolution in-app logo (`drawable/app_logo.png`).

- **Architecture & Infrastructure:**
  - Start-gate 3-stage state machine (`WAITING_FOR_TOP` → `READY_TOP` → `DOWN`) preventing false start reps.
  - Exponential Moving Average (EMA α=0.4) smoothing and 400ms rep rate-limiting.
  - Offline-first on-device AI pose tracking (Google ML Kit).
  - Native ARM64 AAPT2 build configuration (`android.aapt2FromMavenOverride`).

---

### Artifacts in this Release

- `PushupRoyale-v2.0.0-debug.apk` (SHA256 verified)
- `version.json`
- `SHA256SUMS.txt`
