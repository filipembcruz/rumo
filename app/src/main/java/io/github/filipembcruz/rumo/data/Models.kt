package io.github.filipembcruz.rumo.data

import java.time.LocalDate

enum class TargetKind(val id: String) {
    APP("app"), SITE("site"), CATEGORY("cat");
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: APP }
}

data class Limit(
    val id: Long,
    val kind: TargetKind,
    val target: String,
    val minutes: Int,
    val block: Boolean,
    val enabled: Boolean = true,
    /** Optional minutes per weekday, Monday..Sunday; 0 = no limit that day. Null = [minutes] every day. */
    val days: List<Int>? = null,
) {
    fun minutesOn(d: java.time.DayOfWeek): Int = days?.getOrNull(d.value - 1) ?: minutes
    /** Today's limit in minutes (0 = no limit today). */
    val todayMinutes: Int get() = minutesOn(LocalDate.now().dayOfWeek)
    val todayMs: Long get() = todayMinutes * 60_000L
    val active: Boolean get() = enabled && todayMinutes > 0
}

data class Schedule(
    val id: Long,
    val name: String,
    val startMin: Int,
    val endMin: Int,
    /** bit 0 = Monday … bit 6 = Sunday */
    val days: Int,
    val cats: Set<String>,
    val apps: Set<String>,
    val enabled: Boolean,
    val icon: String = "moon",
)

data class Goal(
    val id: Long,
    val title: String,
    val preset: String?,
    val targetHours: Double,
    val created: Long,
)

data class ActivityLog(
    val id: Long,
    val day: String,
    val goalId: Long?,
    val label: String,
    val minutes: Int,
    val ts: Long,
)

data class FocusSession(
    val id: Long,
    val start: Long,
    val end: Long,
    val plannedMin: Int,
    val completed: Boolean,
    val goalId: Long?,
)

data class AppUse(val pkg: String, val ms: Long, val opens: Int, val cat: Cat)
data class SiteUse(val domain: String, val ms: Long, val visits: Int, val cat: Cat)
data class CatUse(val cat: Cat, val ms: Long)

enum class InsightKind {
    NO_DATA, MORNING_FRESH, UNDER_GOAL, NEAR_GOAL, OVER_GOAL, TOP_DISTRACTING, FOCUS_RUNNING,
    GREAT_BALANCE, LATE_NIGHT, STREAK, SITES_OFF,
}

data class Insight(val kind: InsightKind, val subject: String = "", val ms: Long = 0, val n: Int = 0)

data class TodayModel(
    val total: Long,
    val goalMs: Long,
    val yesterday: Long,
    val avg7: Long,
    val pickups: Int,
    val firstUse: Long?,
    val productiveMs: Long,
    val distractingMs: Long,
    val focusScore: Int,
    val hourly: List<Long>,
    val apps: List<AppUse>,
    val sites: List<SiteUse>,
    val cats: List<CatUse>,
    val focusUntil: Long,
    val streak: Int,
    val sitesEnabled: Boolean,
    val insight: Insight,
    val limits: List<LimitStatus>,
    val battery: BatteryUi? = null,
)

/** Estimated battery use in the current discharge cycle. */
data class BatteryUi(val startMs: Long, val startLevel: Int, val level: Int, val shares: Map<String, Float>)

data class LimitStatus(val limit: Limit, val usedMs: Long) {
    val fraction: Float get() = if (limit.todayMinutes <= 0) 0f else usedMs / (limit.todayMinutes * 60_000f)
}

enum class Range { DAY, WEEK, MONTH, YEAR, CUSTOM }

data class PeriodModel(
    val range: Range,
    val from: LocalDate,
    val to: LocalDate,
    val bars: List<Long>,
    val barLabels: List<String>,
    val currentIndex: Int,
    val total: Long,
    val avg: Long,
    val prevTotal: Long,
    val goalMs: Long,
    val apps: List<AppUse>,
    val sites: List<SiteUse>,
    val cats: List<CatUse>,
    /** 7 rows (Mon..Sun) × 24 hours, average minutes. */
    val heat: List<List<Float>>,
    val hourly: List<Long>,
    val weekdayAvg: List<Long>,
    val pickups: Int,
    val productiveMs: Long,
    val distractingMs: Long,
    val days: Int,
    val longestDay: LocalDate?,
    val longestMs: Long,
)

data class DetailModel(
    val kind: TargetKind,
    val id: String,
    val cat: Cat,
    val today: Long,
    val avgDaily: Long,
    val total30: Long,
    val opensToday: Int,
    val opens30: Int,
    val daily: List<Long>,
    val dayLabels: List<String>,
    val hourly: List<Long>,
    val limit: Limit?,
    val pause: Boolean,
    val weekTotal: Long,
    val prevWeekTotal: Long,
    val batteryPct: Float? = null,
)

data class GoalProgress(val goal: Goal, val doneHours: Double, val weekHours: Double)

data class Achievement(val id: String, val unlocked: Boolean, val progress: Float)

data class GrowthModel(
    val streak: Int,
    val bestStreak: Int,
    val goals: List<GoalProgress>,
    val recent: List<ActivityLog>,
    val weekProductive: List<Long>,
    val weekDistracting: List<Long>,
    val weekLabels: List<String>,
    val yearDistractingMs: Long,
    val projectedYearMs: Long,
    val productiveWeekMs: Long,
    val focusCount: Int,
    val achievements: List<Achievement>,
)
