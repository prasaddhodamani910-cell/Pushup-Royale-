package com.example.pushuproyale

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

/* ── Data models ─────────────────────────────────────────── */

data class Point2D(
    val x: Float,
    val y: Float,
    val confidence: Float = 1f
)

data class ArmBones(
    val shoulder: Point2D,
    val elbow: Point2D,
    val wrist: Point2D,
    val isLeft: Boolean,
    val angle: Double
)

data class FullSkeleton(
    val nose: Point2D? = null,
    val leftShoulder: Point2D? = null,
    val rightShoulder: Point2D? = null,
    val leftElbow: Point2D? = null,
    val rightElbow: Point2D? = null,
    val leftWrist: Point2D? = null,
    val rightWrist: Point2D? = null,
    val leftHip: Point2D? = null,
    val rightHip: Point2D? = null,
    val leftKnee: Point2D? = null,
    val rightKnee: Point2D? = null,
    val leftAnkle: Point2D? = null,
    val rightAnkle: Point2D? = null,
    val isBodyStraight: Boolean = true,
    val isFrontFacing: Boolean = true
)

enum class TrackingGuidance(val message: String, val isWarning: Boolean = false) {
    LOOKING_FOR_USER("Position phone in front & step into view", false),
    STEP_BACK("Move back — ensure hips & legs are in frame", true),
    HOLD_STILL("Hold top plank position to initialize...", false),
    SAG_HIPS("Straighten body — don't sag hips!", true),
    PIKE_HIPS("Lower your hips — maintain straight plank", true),
    READY("Ready! Lower your chest to begin", false),
    DESCENDING("Lower chest towards floor", false),
    BOTTOM("Good depth! Push up now", false),
    ASCENDING("Drive all the way up", false),
    CHEST_NOT_LOW("Pushup too shallow — go all the way down", true),
    NO_TORSO_MOVE("Anti-Cheat: Torso must move, not just arms!", true)
}

enum class PushupState {
    WAITING_FOR_READY,
    TOP,
    DESCENDING,
    BOTTOM,
    ASCENDING,
    REP_CONFIRMED
}

data class PushupFeedback(
    val angle: Int?,
    val reps: Int,
    val isDown: Boolean,
    val arms: List<ArmBones>,
    val skeleton: FullSkeleton?,
    val imageWidth: Int,
    val imageHeight: Int,
    val isRepJustCompleted: Boolean,
    val state: PushupState,
    val guidance: TrackingGuidance,
    val plankAlignmentAngle: Double?,
    val torsoDisplacementRatio: Float
)

/* ── Analyzer ────────────────────────────────────────────── */

/**
 * High-performance full-body pushup analyzer with Anti-Cheat and Front-Camera guidance.
 *
 * State Machine:
 * WAITING_FOR_READY -> TOP -> DESCENDING -> BOTTOM -> ASCENDING -> REP_CONFIRMED -> TOP
 *
 * Anti-Cheat rules:
 * 1. Arm movement alone never counts; requires verified vertical torso displacement.
 * 2. Plank alignment check: Shoulder-Hip-Ankle (or Knee) angle must remain >= 135 deg.
 * 3. Front-camera guidance alerts if user is too close or body is out of frame.
 * 4. Single in-flight inference pipeline prevents dropped frame queuing.
 */
class PushupAnalyzer(
    private val onUpdate: (PushupFeedback) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile private var isClosed = false
    private var detector: com.google.mlkit.vision.pose.PoseDetector? = null

    @Synchronized
    private fun getDetector(): com.google.mlkit.vision.pose.PoseDetector {
        val current = detector
        if (current != null && !isClosed) return current
        val newDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
        detector = newDetector
        isClosed = false
        return newDetector
    }

    private val main = Handler(Looper.getMainLooper())
    private val isProcessing = AtomicBoolean(false)

    @Volatile private var state = PushupState.WAITING_FOR_READY
    @Volatile private var reps = 0

    private var lastRepTimeMs = 0L
    private var lastSeenPoseTimeMs = 0L
    private var smoothedAngle: Double? = null
    private var readyStableFrames = 0

    // Reference top plank measurements
    private var topShoulderWristSpan: Float? = null
    private var maxTorsoDisplacementInRep = 0f

    var downThreshold: Double = DOWN
    var upThreshold: Double = UP

    fun reset() {
        reps = 0
        state = PushupState.WAITING_FOR_READY
        smoothedAngle = null
        lastRepTimeMs = 0L
        lastSeenPoseTimeMs = 0L
        readyStableFrames = 0
        topShoulderWristSpan = null
        maxTorsoDisplacementInRep = 0f
    }

    fun close() {
        synchronized(this) {
            isClosed = true
            try { detector?.close() } catch (_: Exception) { /* best effort */ }
            detector = null
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val img = proxy.image
        if (img == null) {
            proxy.close()
            return
        }

        // Drop frame immediately if inference is already running (single in-flight)
        if (!isProcessing.compareAndSet(false, true)) {
            proxy.close()
            return
        }

        val activeDetector = synchronized(this) {
            if (isClosed) null else getDetector()
        }
        if (activeDetector == null) {
            proxy.close()
            isProcessing.set(false)
            return
        }

        val rot = proxy.imageInfo.rotationDegrees
        val rotated = rot == 90 || rot == 270
        val w = if (rotated) img.height else img.width
        val h = if (rotated) img.width else img.height

        val inputImage = try {
            InputImage.fromMediaImage(img, rot)
        } catch (e: Exception) {
            proxy.close()
            isProcessing.set(false)
            return
        }

        activeDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                try {
                    processPose(pose, w, h)
                } catch (e: Exception) {
                    Log.e("PushupAnalyzer", "Error processing pose: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PushupAnalyzer", "ML Kit pose detection failure: ${e.message}")
            }
            .addOnCompleteListener {
                proxy.close()
                isProcessing.set(false)
            }
    }

    private fun processPose(pose: Pose, imageWidth: Int, imageHeight: Int) {
        val now = SystemClock.uptimeMillis()

        // 1. Extract Full Body Landmarks
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)?.takeIf { it.inFrameLikelihood >= 0.45f }?.toPoint2D()
        val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val le = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val re = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val lw = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val rw = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.takeIf { it.inFrameLikelihood >= MIN_CONFIDENCE }?.toPoint2D()
        val lh = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.takeIf { it.inFrameLikelihood >= 0.42f }?.toPoint2D()
        val rh = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.takeIf { it.inFrameLikelihood >= 0.42f }?.toPoint2D()
        val lk = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.takeIf { it.inFrameLikelihood >= 0.40f }?.toPoint2D()
        val rk = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.takeIf { it.inFrameLikelihood >= 0.40f }?.toPoint2D()
        val la = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.takeIf { it.inFrameLikelihood >= 0.35f }?.toPoint2D()
        val ra = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.takeIf { it.inFrameLikelihood >= 0.35f }?.toPoint2D()

        // Check if upper body is present
        val hasShoulders = ls != null || rs != null
        val hasArms = (ls != null && le != null && lw != null) || (rs != null && re != null && rw != null)
        val hasHips = lh != null || rh != null

        // Reset state safely if user completely disappears for > 1500ms
        if (!hasShoulders) {
            if (now - lastSeenPoseTimeMs > POSE_LOSS_TIMEOUT_MS && state != PushupState.WAITING_FOR_READY) {
                state = PushupState.WAITING_FOR_READY
                readyStableFrames = 0
                topShoulderWristSpan = null
                smoothedAngle = null
            }
            dispatch(
                PushupFeedback(
                    angle = null,
                    reps = reps,
                    isDown = false,
                    arms = emptyList(),
                    skeleton = null,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isRepJustCompleted = false,
                    state = state,
                    guidance = TrackingGuidance.LOOKING_FOR_USER,
                    plankAlignmentAngle = null,
                    torsoDisplacementRatio = 0f
                )
            )
            return
        }

        lastSeenPoseTimeMs = now

        // Check front-camera placement: if user is too close, hips & legs are cut off
        if (!hasHips) {
            dispatch(
                PushupFeedback(
                    angle = null,
                    reps = reps,
                    isDown = false,
                    arms = emptyList(),
                    skeleton = FullSkeleton(
                        nose = nose,
                        leftShoulder = ls,
                        rightShoulder = rs,
                        leftElbow = le,
                        rightElbow = re,
                        leftWrist = lw,
                        rightWrist = rw,
                        isBodyStraight = false
                    ),
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isRepJustCompleted = false,
                    state = PushupState.WAITING_FOR_READY,
                    guidance = TrackingGuidance.STEP_BACK,
                    plankAlignmentAngle = null,
                    torsoDisplacementRatio = 0f
                )
            )
            return
        }

        // 2. Compute Arm Angles
        val arms = mutableListOf<ArmBones>()
        if (ls != null && le != null && lw != null) {
            val a = computeAngle(ls, le, lw)
            arms += ArmBones(ls, le, lw, isLeft = true, angle = a)
        }
        if (rs != null && re != null && rw != null) {
            val a = computeAngle(rs, re, rw)
            arms += ArmBones(rs, re, rw, isLeft = false, angle = a)
        }

        val rawArmAngle = if (arms.isNotEmpty()) arms.map { it.angle }.average() else null

        // Lightweight EMA angle smoothing to eliminate jitter without inducing lag
        val currentArmAngle = if (rawArmAngle != null) {
            val smoothed = smoothedAngle?.let { prev ->
                prev * (1.0 - EMA_ALPHA) + rawArmAngle * EMA_ALPHA
            } ?: rawArmAngle
            smoothedAngle = smoothed
            smoothed
        } else {
            smoothedAngle
        }

        // 3. Compute Plank Alignment Angle (Shoulder - Hip - Leg)
        val shoulderMid = averagePoints(ls, rs)
        val hipMid = averagePoints(lh, rh)
        val ankleMid = averagePoints(la, ra)
        val kneeMid = averagePoints(lk, rk)
        val lowerLegPoint = ankleMid ?: kneeMid

        val plankAngle = if (shoulderMid != null && hipMid != null && lowerLegPoint != null) {
            computeAngle(shoulderMid, hipMid, lowerLegPoint)
        } else null

        // Body straightness evaluation (NEVER treat missing measurements as proof of good form!)
        val hasVerifiedAlignment = plankAngle != null
        val isPlankStraight = hasVerifiedAlignment && plankAngle!! >= PLANK_MIN_STRAIGHT_ANGLE
        val isSagging = hasVerifiedAlignment && plankAngle!! < PLANK_MIN_STRAIGHT_ANGLE && hipMid != null && shoulderMid != null && hipMid.y > shoulderMid.y + 15f
        val isPiking = hasVerifiedAlignment && plankAngle!! < PLANK_MIN_STRAIGHT_ANGLE && hipMid != null && shoulderMid != null && hipMid.y < shoulderMid.y - 15f

        // 4. Compute Torso Vertical Displacement (Anti-Cheat)
        // In image coordinates, y increases downward.
        // During pushup: Wrists stay grounded (larger Y), while shoulders drop down towards wrists (Y increases).
        // Therefore, (wristY - shoulderY) shrinks from TOP to BOTTOM!
        val wristMid = averagePoints(lw, rw)
        val currentSpan = if (shoulderMid != null && wristMid != null) {
            wristMid.y - shoulderMid.y
        } else null

        // Displacement ratio: how much the shoulder has lowered towards the wrist relative to top span
        val displacementRatio = if (currentSpan != null && topShoulderWristSpan != null && topShoulderWristSpan!! > 15f) {
            val delta = topShoulderWristSpan!! - currentSpan
            max(0f, delta / topShoulderWristSpan!!)
        } else 0f

        if (displacementRatio > maxTorsoDisplacementInRep) {
            maxTorsoDisplacementInRep = displacementRatio
        }

        // 5. State Machine & Anti-Cheat Logic
        var justCompleted = false
        var activeGuidance = TrackingGuidance.READY

        when (state) {
            PushupState.WAITING_FOR_READY -> {
                when {
                    !hasArms -> {
                        activeGuidance = TrackingGuidance.LOOKING_FOR_USER
                        readyStableFrames = 0
                    }
                    !hasVerifiedAlignment -> {
                        // Strict validation: hips and legs must be visible before entering ready!
                        activeGuidance = TrackingGuidance.STEP_BACK
                        readyStableFrames = 0
                    }
                    isSagging -> {
                        activeGuidance = TrackingGuidance.SAG_HIPS
                        readyStableFrames = 0
                    }
                    isPiking -> {
                        activeGuidance = TrackingGuidance.PIKE_HIPS
                        readyStableFrames = 0
                    }
                    currentArmAngle == null || currentArmAngle < upThreshold -> {
                        activeGuidance = TrackingGuidance.HOLD_STILL
                        readyStableFrames = 0
                    }
                    else -> {
                        // User in top plank position with straight arms and verified straight body
                        readyStableFrames++
                        if (readyStableFrames >= REQUIRED_STABLE_FRAMES) {
                            state = PushupState.TOP
                            topShoulderWristSpan = currentSpan
                            maxTorsoDisplacementInRep = 0f
                            activeGuidance = TrackingGuidance.READY
                        } else {
                            activeGuidance = TrackingGuidance.HOLD_STILL
                        }
                    }
                }
            }

            PushupState.TOP -> {
                activeGuidance = TrackingGuidance.READY
                if (currentSpan != null && (topShoulderWristSpan == null || currentSpan > topShoulderWristSpan!!)) {
                    topShoulderWristSpan = currentSpan
                }

                if (!hasVerifiedAlignment) {
                    activeGuidance = TrackingGuidance.STEP_BACK
                } else if (isSagging) {
                    activeGuidance = TrackingGuidance.SAG_HIPS
                } else if (currentArmAngle != null && currentArmAngle < 135.0) {
                    state = PushupState.DESCENDING
                    maxTorsoDisplacementInRep = displacementRatio
                    activeGuidance = TrackingGuidance.DESCENDING
                }
            }

            PushupState.DESCENDING -> {
                activeGuidance = TrackingGuidance.DESCENDING

                if (!hasVerifiedAlignment) {
                    activeGuidance = TrackingGuidance.STEP_BACK
                } else if (isSagging) {
                    activeGuidance = TrackingGuidance.SAG_HIPS
                }

                if (currentArmAngle != null && currentArmAngle <= downThreshold) {
                    // Anti-Cheat check: Torso must have vertically lowered!
                    if (maxTorsoDisplacementInRep >= MIN_TORSO_DISPLACEMENT_RATIO || displacementRatio >= MIN_TORSO_DISPLACEMENT_RATIO) {
                        state = PushupState.BOTTOM
                        activeGuidance = TrackingGuidance.BOTTOM
                    } else {
                        // Elbows bent but body did not move (e.g. waving hands or bending elbows while sitting)
                        activeGuidance = TrackingGuidance.NO_TORSO_MOVE
                    }
                } else if (currentArmAngle != null && currentArmAngle >= upThreshold - 10.0 && maxTorsoDisplacementInRep < MIN_TORSO_DISPLACEMENT_RATIO) {
                    // Returned up without hitting depth
                    state = PushupState.TOP
                    activeGuidance = TrackingGuidance.CHEST_NOT_LOW
                }
            }

            PushupState.BOTTOM -> {
                activeGuidance = TrackingGuidance.BOTTOM

                if (currentArmAngle != null && currentArmAngle >= downThreshold + 15.0) {
                    state = PushupState.ASCENDING
                    activeGuidance = TrackingGuidance.ASCENDING
                }
            }

            PushupState.ASCENDING -> {
                activeGuidance = TrackingGuidance.ASCENDING

                if (currentArmAngle != null && currentArmAngle >= upThreshold) {
                    val timeSinceLastRep = now - lastRepTimeMs
                    if (timeSinceLastRep >= MIN_REP_INTERVAL_MS && isPlankStraight) {
                        // Rep Confirmed!
                        reps++
                        lastRepTimeMs = now
                        justCompleted = true
                        state = PushupState.TOP
                        maxTorsoDisplacementInRep = 0f
                        activeGuidance = TrackingGuidance.READY
                    } else if (!hasVerifiedAlignment) {
                        activeGuidance = TrackingGuidance.STEP_BACK
                        state = PushupState.TOP
                    } else if (!isPlankStraight) {
                        activeGuidance = TrackingGuidance.SAG_HIPS
                        state = PushupState.TOP
                    }
                }
            }

            PushupState.REP_CONFIRMED -> {
                state = PushupState.TOP
                activeGuidance = TrackingGuidance.READY
            }
        }

        // 6. Build Full Skeleton Model for UI Canvas Rendering
        val skeleton = FullSkeleton(
            nose = nose,
            leftShoulder = ls,
            rightShoulder = rs,
            leftElbow = le,
            rightElbow = re,
            leftWrist = lw,
            rightWrist = rw,
            leftHip = lh,
            rightHip = rh,
            leftKnee = lk,
            rightKnee = rk,
            leftAnkle = la,
            rightAnkle = ra,
            isBodyStraight = isPlankStraight,
            isFrontFacing = true
        )

        val feedback = PushupFeedback(
            angle = currentArmAngle?.toInt(),
            reps = reps,
            isDown = state == PushupState.BOTTOM,
            arms = arms,
            skeleton = skeleton,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            isRepJustCompleted = justCompleted,
            state = state,
            guidance = activeGuidance,
            plankAlignmentAngle = plankAngle,
            torsoDisplacementRatio = maxTorsoDisplacementInRep
        )

        dispatch(feedback)
    }

    private fun dispatch(feedback: PushupFeedback) {
        main.post { onUpdate(feedback) }
    }

    /* ── Geometry & Math Utilities ──────────────────────── */

    private fun PoseLandmark.toPoint2D(): Point2D {
        return Point2D(position.x, position.y, inFrameLikelihood)
    }

    private fun averagePoints(p1: Point2D?, p2: Point2D?): Point2D? {
        return when {
            p1 != null && p2 != null -> Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f, (p1.confidence + p2.confidence) / 2f)
            p1 != null -> p1
            p2 != null -> p2
            else -> null
        }
    }

    companion object {
        const val DOWN = 90.0
        const val UP   = 150.0
        const val MIN_REP_INTERVAL_MS = 380L
        const val POSE_LOSS_TIMEOUT_MS = 1500L
        const val MIN_CONFIDENCE = 0.50f
        const val PLANK_MIN_STRAIGHT_ANGLE = 135.0
        const val MIN_TORSO_DISPLACEMENT_RATIO = 0.12f
        private const val EMA_ALPHA = 0.45
        private const val REQUIRED_STABLE_FRAMES = 3

        /**
         * Calculates angle between three points with vertex at p2, in degrees (0..180).
         */
        fun computeAngle(p1: Point2D, p2: Point2D, p3: Point2D): Double {
            val r = atan2((p3.y - p2.y).toDouble(), (p3.x - p2.x).toDouble()) -
                    atan2((p1.y - p2.y).toDouble(), (p1.x - p2.x).toDouble())
            val deg = abs(Math.toDegrees(r))
            return if (deg > 180.0) 360.0 - deg else deg
        }

        /**
         * Verifies if the plank line formed by shoulder-hip-leg is straight (>= 135 degrees).
         * Returns false if any required landmark is missing.
         */
        fun isPlankValid(shoulder: Point2D?, hip: Point2D?, leg: Point2D?): Boolean {
            if (shoulder == null || hip == null || leg == null) return false
            val angle = computeAngle(shoulder, hip, leg)
            return angle >= PLANK_MIN_STRAIGHT_ANGLE
        }
    }
}
