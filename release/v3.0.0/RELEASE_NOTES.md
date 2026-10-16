# Pushup Royale v3.0.0 Release Notes (Major Update)

**Author:** Prasad Dhodamani  
**Release Date:** October 16, 2026  
**Update Type:** Major Version Release (`3.0.0`)  
**Build:** `gradle :app:assembleDebug`

---

### Major Architectural Upgrades (PushupRoyale Master Fix)

1. **Full-Body Computer Vision AI Pose Engine:**
   - Upgraded ML Kit tracking from arm-only elbow angles to full kinematic body tracking.
   - Concurrently tracks: Shoulders (11, 12), Elbows (13, 14), Wrists (15, 16), Hips (23, 24), Knees (25, 26), Ankles (27, 28), and Head/Nose (0).

2. **Anti-Cheat Torso Displacement Verification:**
   - Moving wrists or bending elbows alone will NEVER count as a pushup.
   - Calculates relative vertical torso displacement between shoulders and grounded wrists.
   - Requires minimum 12-15% vertical torso drop before bottom depth is recognized.

3. **Plank Line & Body Alignment Biofeedback:**
   - Continuously computes Shoulder-to-Hip-to-Ankle/Knee alignment angle.
   - Identifies sagging hips (< 135°) and piking hips.
   - Provides live visual warnings (Coral skeleton color change) and text alerts ("Straighten body — don't sag hips!").

4. **Front-Camera Placement Guidance:**
   - Optimized for workout phone placement on the floor in front of the athlete's face.
   - Real-time setup feedback:
     - *"Position phone in front & step into view"*
     - *"Move back — ensure hips & legs are in frame"*
     - *"Hold top plank position to initialize..."*
     - *"Straighten body — don't sag hips!"*
     - *"Ready! Lower your chest to begin"*
     - *"Anti-Cheat: Torso must move, not just arms!"*

5. **Liquid Glass Full-Body Skeleton Overlay:**
   - Expanded Canvas renderer in `MainActivity.kt` to draw head node, clavicle, lateral torso lines, spine line, pelvic line, legs, and arms.
   - Pulsing depth nodes and adaptive color states (Gold -> Mint -> Coral warning).

6. **Fast Inference Camera Pipeline:**
   - Added `ImageAnalysis.setTargetResolution(480, 640)` for 3x faster ML Kit frame processing.
   - Single in-flight inference pipeline via `AtomicBoolean` preventing frame queue lag.
   - Responsive 380ms cooldown timer.

---

### Artifacts in this Release
- `PushupRoyale-v3.0.0-debug.apk`
- `version.json`
- `RELEASE_NOTES.md`
- `SHA256SUMS.txt`
