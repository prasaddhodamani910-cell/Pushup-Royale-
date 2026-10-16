PUSHUP ROYALE — MASTER FIX PROMPT

Role

Act as a senior Android developer specializing in Kotlin, Jetpack Compose, CameraX, ML Kit Pose Detection, computer vision, and real-time fitness tracking.

Your task is to fix and improve the existing Pushup Royale app, not create an unrelated replacement.

Package: "com.example.pushuproyale"

1. Inspect Before Editing

- Inspect the entire existing project and identify the current camera, pose detection, push-up counter, UI, game logic, and settings implementations.
- Find the actual causes of slow detection, incorrect rep counting, and arm-only tracking.
- Reuse the existing architecture wherever practical.
- Do not blindly replace working files or remove existing features.
- Check the current ML Kit API version and use APIs compatible with the project's dependencies.
- Do not claim that a fix works unless it has actually been tested.

2. Main Goal: Full-Body Push-Up Tracking

The current tracker can count movement when primarily the arms move. Fix this.

The tracker must evaluate the user's overall push-up movement, not merely the elbow angle.

Use the available ML Kit pose landmarks, including:

- Left and right shoulders
- Left and right elbows
- Left and right wrists
- Left and right hips
- Left and right knees
- Left and right ankles
- Nose or other head landmarks when useful

Check landmark visibility and confidence before relying on measurements. Handle partially hidden or unavailable landmarks safely.

Full-body validation

A repetition should require:

1. The user is correctly positioned and enough required landmarks are visible.
2. A valid starting position is established.
3. The body maintains a reasonably straight shoulder-to-hip-to-ankle or appropriate modified-push-up alignment.
4. The torso and body move through a meaningful push-up range.
5. The elbows bend during the lowering phase.
6. The user reaches a valid bottom position.
7. The user pushes back to a valid top position.
8. The complete movement sequence is detected before the repetition is counted.

Use normalized measurements and relative geometry where appropriate rather than relying exclusively on fixed pixel distances.

Account for normal differences in body proportions and camera distance.

Do not require perfectly straight arms or a perfectly rigid body at every frame. Use sensible tolerances and smoothing.

3. Anti-Cheat Rep Counting

Implement a robust state machine, such as:

"WAITING_FOR_READY → TOP → DESCENDING → BOTTOM → ASCENDING → TOP → REP_CONFIRMED"

Adapt the names to the existing architecture if appropriate.

Rules:

- Arm or wrist movement alone must never count as a push-up.
- Elbow-angle changes alone must not be enough to count.
- Require meaningful torso/body movement and valid full-body geometry.
- Do not count repeated frames of the same position as multiple repetitions.
- Do not count partial movements that fail the configured range-of-motion requirements.
- Ignore unstable or low-confidence landmark detections.
- Reset or pause the state safely when tracking is lost.
- Do not generate a repetition simply because the user re-enters the camera frame.
- Prevent duplicate counts during noisy transitions.
- Allow a short, configurable cooldown without making the tracker unnecessarily slow.

If the system cannot reliably determine whether a complete push-up occurred, do not count it. Show a useful instruction explaining what needs to improve.

4. Front-Camera Positioning and Setup

The user wants to place the phone in front of their face, rather than relying on a side-on setup.

Design a live setup screen that checks whether the camera can see enough of the user's body.

Provide clear instructions such as:

- Move farther from the phone.
- Move the phone back until your full body fits in the frame.
- Keep your shoulders, hips, knees, and ankles visible.
- Adjust the phone angle.
- Improve lighting.
- Hold still while tracking initializes.
- Your body is partly outside the frame.
- Tracking is ready.

Show a live skeleton or landmark overlay where feasible.

Do not allow counting until the setup has enough reliable landmarks to support the selected tracking method.

Important camera limitation

A front-facing view can hide important body landmarks when the user performs a push-up. If the camera angle does not provide enough reliable information to validate full-body movement, clearly ask the user to reposition the phone.

Do not silently fall back to counting arm-only movements.

If necessary, implement an explicit camera-positioning check and explain the limitations honestly.

5. Improve Tracking Speed

The current tracking feels too slow. Profile and fix the actual bottlenecks.

Inspect and optimize:

- CameraX image analysis configuration.
- ML Kit pose detection latency.
- Image rotation and conversion overhead.
- Landmark processing and smoothing.
- Recomposition frequency in Jetpack Compose.
- Unnecessary allocations and repeated calculations.
- Blocking work on the main thread.
- Camera and detector lifecycle handling.

Camera pipeline requirements

- Use "ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST" where appropriate.
- Ensure only one inference operation is in flight at a time.
- Close every "ImageProxy" correctly, including error and exception paths.
- Close image resources after asynchronous inference completes.
- Run expensive image analysis off the main thread.
- Use an appropriate camera resolution instead of processing unnecessarily large frames.
- Reuse executors and resources where appropriate.
- Avoid recreating the pose detector on every recomposition.
- Shut down executors and release resources when the camera screen is disposed of.
- Handle lifecycle changes and camera permission changes safely.

Do not sacrifice full-body validation merely to increase frame rate.

6. Reduce Unnecessary Counting Delay

Review all existing smoothing, thresholds, state transitions, and cooldowns.

- Remove excessive smoothing that makes the tracker react too slowly.
- Use configurable and sensible thresholds.
- Prefer lightweight temporal smoothing that rejects noise without causing excessive lag.
- Make state transitions responsive while remaining stable.
- Keep the cooldown short enough to support natural exercise.
- Avoid arbitrary long delays before confirming a valid repetition.
- Avoid counting based on a single noisy frame.

Measure actual inference and rep-confirmation latency where possible. Do not invent performance numbers.

7. Skeleton Overlay

If the app displays pose landmarks or a skeleton:

- Map landmarks correctly onto the CameraX preview.
- Account for image rotation.
- Account for front-camera mirroring.
- Account for preview scaling, cropping, and aspect-ratio differences.
- Ensure the overlay remains aligned when the screen rotates or dimensions change.
- Display only reliable landmarks and connections.
- Avoid excessive UI updates that slow down the camera pipeline.

The displayed skeleton must correspond to the user's actual detected pose.

8. Calibration and Exercise Modes

If feasible within the current architecture, add a brief calibration phase that establishes the user's top position and suitable movement range.

Calibration must not replace safety and geometry validation.

If the app already supports exercise settings or modified push-ups, preserve those features. Do not make standard-push-up geometry mandatory for every supported exercise variation.

Keep thresholds configurable and document what each threshold means.

9. Preserve Existing Features

Do not remove existing working functionality, including any of the following that are already implemented:

- Push-up counter
- XP and rewards
- Ranks and progression
- Streaks
- Workout history
- Settings
- Sound and haptic feedback
- Existing screens and navigation
- Theme and visual design
- Local persistence
- Existing onboarding and permissions

Integrate the new tracking system with the current app instead of creating a disconnected demo.

Keep pose processing on-device. Do not upload camera frames or pose data to a server.

10. Error Handling and Diagnostics

Provide clear user-facing messages when:

- Camera permission is denied.
- The camera cannot start.
- Required landmarks are missing.
- The body is outside the frame.
- The pose is unstable.
- The user is not in a valid starting position.
- The movement range is insufficient.
- Tracking is temporarily lost.

Add lightweight debug logging for development, including:

- Inference duration
- Required-landmark availability
- Current push-up state
- Reason a repetition was rejected
- Camera or detector errors

Avoid logging sensitive image data or unnecessary personal information. Debug logging should be disabled or appropriately reduced in production.

11. Testing Requirements

Test the implementation against these scenarios:

1. Correct complete push-up: counts exactly once.
2. Arms bend without meaningful body movement: does not count.
3. Hands or wrists move without a complete push-up: does not count.
4. User holds the bottom position: does not repeatedly count.
5. User performs a partial repetition: does not count.
6. User briefly leaves the frame: tracker pauses or resets safely.
7. Required landmarks are missing: tracker gives guidance instead of counting.
8. User performs multiple complete repetitions: each valid repetition counts once.
9. Camera screen is opened and closed repeatedly: no accumulating camera sessions or inference tasks.
10. Camera permission is denied and later granted: app handles the transition safely.
11. Existing game features continue working.
12. The project compiles successfully.

If a physical device or camera is unavailable, distinguish code-level tests from tests that require real-device verification.

12. Build and Final Report

After making changes:

1. Run the available Gradle build and relevant tests.
2. Fix errors caused by your changes.
3. Inspect the final diff for unrelated modifications.
4. Confirm that the UI still connects to the actual tracking implementation.
5. Report which files changed and why.
6. Report the build and test results honestly.
7. List any remaining limitations, especially limitations of front-camera full-body pose estimation.

Do not merely write a plan. Inspect the source code and implement the fixes.

Do not fake test results, claim unverified improvements, or replace the existing app with a simplified mockup.

Definition of Done

The task is complete only when the existing Pushup Royale project has a faster and more responsive tracking pipeline, robust full-body validation, reliable rep-state transitions, anti-cheat protection against arm-only movements, clear camera-positioning guidance, and no avoidable regressions in existing features.
