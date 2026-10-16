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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
    var poseState   by remember { mutableStateOf(PushupState.WAITING_FOR_READY) }
    var arms        by remember { mutableStateOf<List<ArmBones>>(emptyList()) }
    var skeleton    by remember { mutableStateOf<FullSkeleton?>(null) }
    var guidance    by remember { mutableStateOf(TrackingGuidance.LOOKING_FOR_USER) }
    var plankAngle  by remember { mutableStateOf<Double?>(null) }
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
            angle      = fb.angle
            reps       = fb.reps
            isDown     = fb.isDown
            poseState  = fb.state
            arms       = fb.arms
            skeleton   = fb.skeleton
            guidance   = fb.guidance
            plankAngle = fb.plankAlignmentAngle
            srcDims    = fb.imageWidth to fb.imageHeight

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

    DisposableEffect(Unit) {
        onDispose {
            try { analyzer.close() } catch (_: Exception) {}
        }
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
                SkeletonOverlay(skeleton, arms, srcDims.first, srcDims.second, isFront, isDown)

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
                    guidance    = guidance,
                    plankAngle  = plankAngle,
                    wonBanner   = wonBanner,
                    dailyGoal   = dailyGoal,
                    todayTotal  = todayReps + reps,
                    repPop      = repPop,
                    onFinish    = {
                        val currentReps = reps
                        if (currentReps > 0) {
                            Ranks.addSession(ctx, currentReps)
                            xp        = Ranks.xp(ctx)
                            wins      = Ranks.wins(ctx)
                            streak    = Ranks.streak(ctx)
                            todayReps = Ranks.todayReps(ctx)
                            wonBanner = currentReps >= dailyGoal
                            analyzer.reset()
                            reps = 0
                        }
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
                    dailyGoal        = dailyGoal,
                    onGoalChange     = {
                        dailyGoal = it
                        Ranks.setDailyGoal(ctx, it)
                    },
                    downAngle        = downAngle,
                    onDownAngleChange= {
                        val clamped = it.coerceIn(60f, (upAngle - 20f).coerceAtMost(115f))
                        downAngle = clamped
                        Ranks.setDownAngle(ctx, clamped)
                    },
                    upAngle          = upAngle,
                    onUpAngleChange  = {
                        val clamped = it.coerceIn((downAngle + 20f).coerceAtLeast(130f), 175f)
                        upAngle = clamped
                        Ranks.setUpAngle(ctx, clamped)
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
                            RoundStepButton("+") { onDownAngleChange((downAngle + 5f).coerceAtMost(upAngle - 20f).coerceAtMost(115f)) }
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
                            RoundStepButton("-") { onUpAngleChange((upAngle - 5f).coerceAtLeast(downAngle + 20f).coerceAtLeast(130f)) }
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
                                    Text("v3.0.0", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        "🛡️ Anti-Cheat Full-Body Engine (Torso Displacement)",
                        "📐 Real-Time Body Alignment & Plank Guidance",
                        "📷 Front-Camera Optimized Tracking Pipeline",
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
    guidance: TrackingGuidance,
    plankAngle: Double?,
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

            // State and Guidance indicator
            val stateText = buildString {
                append(guidance.message)
                if (angle != null) {
                    append("  •  $angle°")
                }
                if (plankAngle != null && (guidance == TrackingGuidance.SAG_HIPS || guidance == TrackingGuidance.PIKE_HIPS)) {
                    append(" (${plankAngle.toInt()}°)")
                }
            }

            val stateColor = when {
                guidance.isWarning -> Coral
                isDown -> Mint
                guidance == TrackingGuidance.READY -> GoldLight
                else -> TextSecondary
            }

            Text(
                stateText,
                color = stateColor,
                fontSize = 13.sp,
                fontWeight = if (isDown || guidance.isWarning) FontWeight.SemiBold else FontWeight.Normal,
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
    skeleton: FullSkeleton?,
    arms: List<ArmBones>,
    sourceW: Int,
    sourceH: Int,
    isFront: Boolean,
    isDown: Boolean,
    modifier: Modifier = Modifier
) {
    if (sourceW <= 0 || sourceH <= 0 || (arms.isEmpty() && skeleton == null)) return

    Canvas(modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        val scale = maxOf(cw / sourceW, ch / sourceH)
        val ox = (cw - sourceW * scale) / 2f
        val oy = (ch - sourceH * scale) / 2f

        fun map(p: Point2D?): Offset? {
            if (p == null) return null
            val px = ox + p.x * scale
            val py = oy + p.y * scale
            return Offset(if (isFront) cw - px else px, py)
        }

        val isStraight = skeleton?.isBodyStraight ?: true
        val bone = when {
            !isStraight -> Coral
            isDown -> Mint
            else -> Gold
        }
        val glow = bone.copy(alpha = 0.35f)
        val joint = TextPrimary

        fun drawBone(p1: Offset?, p2: Offset?, strokeWidth: Float = 5f, glowWidth: Float = 16f) {
            if (p1 != null && p2 != null) {
                drawLine(glow, p1, p2, glowWidth, StrokeCap.Round)
                drawLine(bone, p1, p2, strokeWidth, StrokeCap.Round)
            }
        }

        fun drawJointNode(p: Offset?, radius: Float = 8f, isEmphasized: Boolean = false) {
            if (p != null) {
                if (isEmphasized) {
                    drawCircle(glow, radius + 10f, p)
                }
                drawCircle(bone, radius, p)
                drawCircle(joint, radius * 0.45f, p)
            }
        }

        // Draw arms
        for (arm in arms) {
            val s = map(arm.shoulder)
            val e = map(arm.elbow)
            val w = map(arm.wrist)

            drawBone(s, e, 6f, 18f)
            drawBone(e, w, 6f, 18f)

            drawJointNode(s, 10f)
            val er = if (isDown) 18f else 13f
            drawJointNode(e, er, isEmphasized = true)
            drawJointNode(w, 10f)
        }

        // Draw Full-Body Torso & Lower Body if detected
        if (skeleton != null) {
            val ls = map(skeleton.leftShoulder)
            val rs = map(skeleton.rightShoulder)
            val lh = map(skeleton.leftHip)
            val rh = map(skeleton.rightHip)
            val lk = map(skeleton.leftKnee)
            val rk = map(skeleton.rightKnee)
            val la = map(skeleton.leftAnkle)
            val ra = map(skeleton.rightAnkle)
            val nose = map(skeleton.nose)

            // Shoulder clavicle
            drawBone(ls, rs, 4f, 12f)

            // Torso lateral borders
            drawBone(ls, lh, 5f, 14f)
            drawBone(rs, rh, 5f, 14f)

            // Pelvic line
            drawBone(lh, rh, 4f, 12f)

            // Spine line (mid shoulders to mid hips)
            if (ls != null && rs != null && lh != null && rh != null) {
                val midS = Offset((ls.x + rs.x) / 2f, (ls.y + rs.y) / 2f)
                val midH = Offset((lh.x + rh.x) / 2f, (lh.y + rh.y) / 2f)
                drawBone(midS, midH, 4f, 12f)
            }

            // Lower body (Hips -> Knees -> Ankles)
            drawBone(lh, lk, 5f, 14f)
            drawBone(lk, la, 5f, 14f)
            drawBone(rh, rk, 5f, 14f)
            drawBone(rk, ra, 5f, 14f)

            // Hip & Leg joint nodes
            drawJointNode(lh, 8f)
            drawJointNode(rh, 8f)
            drawJointNode(lk, 9f)
            drawJointNode(rk, 9f)
            drawJointNode(la, 8f)
            drawJointNode(ra, 8f)

            // Head node
            if (nose != null) {
                drawCircle(glow, 20f, nose)
                drawCircle(bone.copy(alpha = 0.5f), 12f, nose)
                drawCircle(joint, 5f, nose)
            }
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
            try { provider?.unbindAll() } catch (_: Exception) {}
            try { executor.shutdown() } catch (_: Exception) {}
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

                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    android.util.Size(480, 640),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also {
                                it.setSurfaceProvider(view.surfaceProvider)
                            }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setResolutionSelector(resolutionSelector)
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
