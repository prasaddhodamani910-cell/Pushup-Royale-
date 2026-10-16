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
        val goal        = dailyGoal(c)
        val alreadyWonToday = (last == today) && (curToday >= goal)
        val newToday    = curToday + reps
        val wonNow      = !alreadyWonToday && (newToday >= goal)
        val newWins     = wins(c) + if (wonNow) 1 else 0

        val newStreak = when (last) {
            today -> maxOf(curStreak, 1)       // same day — keep
            yest  -> curStreak + 1             // consecutive — bump
            else  -> 1                         // gap — restart
        }
        val best = maxOf(p.getInt("best_streak", 0), newStreak)
        val curSessions = p.getInt("total_sessions", 0) + 1

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
            .putInt("wins",           newWins)
            .putInt("streak",         newStreak)
            .putInt("best_streak",    best)
            .putInt("total_sessions", curSessions)
            .putString("last_date",   today)
            .putInt("today_reps",     newToday)
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
