package io.github.filipembcruz.rumo.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** All user settings. Every write bumps [changes] so the UI can recompose. */
class Prefs private constructor(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("rumo", Context.MODE_PRIVATE)
    private val marks: SharedPreferences = context.getSharedPreferences("rumo_marks", Context.MODE_PRIVATE)
    /** Internal state that should not trigger UI refreshes (battery cycle…). */
    private val state: SharedPreferences = context.getSharedPreferences("rumo_state", Context.MODE_PRIVATE)
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    private fun bump() { _changes.value = _changes.value + 1 }
    private fun str(k: String, d: String) = sp.getString(k, d) ?: d
    private fun putStr(k: String, v: String) { sp.edit().putString(k, v).apply(); bump() }
    private fun bool(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun putBool(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply(); bump() }
    private fun int(k: String, d: Int) = sp.getInt(k, d)
    private fun putInt(k: String, v: Int) { sp.edit().putInt(k, v).apply(); bump() }
    private fun long(k: String, d: Long) = sp.getLong(k, d)
    private fun putLong(k: String, v: Long) { sp.edit().putLong(k, v).apply(); bump() }
    private fun set(k: String, d: Set<String>) = sp.getStringSet(k, d)?.toSet() ?: d
    private fun putSet(k: String, v: Set<String>) { sp.edit().putStringSet(k, v).apply(); bump() }

    var language: String get() = str("lang", "en"); set(v) = putStr("lang", v)
    var theme: String get() = str("theme", "system"); set(v) = putStr("theme", v)
    var onboarded: Boolean get() = bool("onboarded", false); set(v) = putBool("onboarded", v)
    var name: String get() = str("name", ""); set(v) = putStr("name", v)
    var dailyGoalMin: Int get() = int("goal_min", 180); set(v) = putInt("goal_min", v)
    var lifeGoals: Set<String> get() = set("life_goals", emptySet()); set(v) = putSet("life_goals", v)

    // notifications
    var limitAlerts: Boolean get() = bool("n_limits", true); set(v) = putBool("n_limits", v)
    var nudges: Boolean get() = bool("n_nudges", true); set(v) = putBool("n_nudges", v)
    var nudgeAfterMin: Int get() = int("n_nudge_min", 20); set(v) = putInt("n_nudge_min", v)
    var dailySummary: Boolean get() = bool("n_daily", true); set(v) = putBool("n_daily", v)
    var summaryHour: Int get() = int("n_daily_hour", 21); set(v) = putInt("n_daily_hour", v)
    var weeklyReport: Boolean get() = bool("n_weekly", true); set(v) = putBool("n_weekly", v)
    var bedtimeReminder: Boolean get() = bool("n_bedtime", false); set(v) = putBool("n_bedtime", v)
    var bedtimeMin: Int get() = int("n_bedtime_min", 23 * 60); set(v) = putInt("n_bedtime_min", v)
    var monitoringNotification: Boolean get() = bool("n_monitor", true); set(v) = putBool("n_monitor", v)

    // blocking
    var pauseSeconds: Int get() = int("pause_s", 8); set(v) = putInt("pause_s", v)
    var strict: Boolean get() = bool("strict", false); set(v) = putBool("strict", v)
    var siteIcons: Boolean get() = bool("site_icons", true); set(v) = putBool("site_icons", v)
    var blockAdult: Boolean get() = bool("block_adult", false); set(v) = putBool("block_adult", v)

    // lists
    var minListMinutes: Int get() = int("min_list_min", 1); set(v) = putInt("min_list_min", v)

    // battery cycle (silent)
    var cycleStart: Long get() = state.getLong("cycle_start", -1); set(v) { state.edit().putLong("cycle_start", v).apply() }
    var cycleLevel: Int get() = state.getInt("cycle_level", -1); set(v) { state.edit().putInt("cycle_level", v).apply() }

    // focus session
    var focusStart: Long get() = long("focus_start", 0); set(v) = putLong("focus_start", v)
    var focusUntil: Long get() = long("focus_until", 0); set(v) = putLong("focus_until", v)
    var focusCats: Set<String>
        get() = set("focus_cats", setOf("social", "video", "games", "shopping"))
        set(v) = putSet("focus_cats", v)
    var focusApps: Set<String> get() = set("focus_apps", emptySet()); set(v) = putSet("focus_apps", v)
    var focusGoalId: Long get() = long("focus_goal", -1); set(v) = putLong("focus_goal", v)

    // bookkeeping
    var lastSummaryDay: String get() = str("last_summary", ""); set(v) = putStr("last_summary", v)
    var lastWeeklyDay: String get() = str("last_weekly", ""); set(v) = putStr("last_weekly", v)
    var lastBedtimeDay: String get() = str("last_bedtime", ""); set(v) = putStr("last_bedtime", v)

    val isPt: Boolean get() = language == "pt"
    val focusActive: Boolean get() = focusUntil > System.currentTimeMillis()

    /** One-shot marks such as "limit 80% already notified today". Reset every day. */
    fun markOnce(key: String): Boolean {
        val today = LocalDate.now().toString()
        if (marks.getString("_day", "") != today) marks.edit().clear().putString("_day", today).apply()
        if (marks.getBoolean(key, false)) return false
        marks.edit().putBoolean(key, true).apply()
        return true
    }

    /** Temporary passes: "pause passed for pkg until", "extra 5 min for target until". */
    fun passUntil(key: String): Long = marks.getLong("pass_$key", 0)
    fun grantPass(key: String, untilMs: Long) { marks.edit().putLong("pass_$key", untilMs).apply() }

    fun exportMap(): Map<String, Any?> = sp.all
    fun importMap(map: Map<String, Any?>) {
        val e = sp.edit().clear()
        for ((k, v) in map) when (v) {
            is Boolean -> e.putBoolean(k, v)
            is Int -> e.putInt(k, v)
            is Long -> e.putLong(k, v)
            is String -> e.putString(k, v)
            is Set<*> -> e.putStringSet(k, v.map { it.toString() }.toSet())
        }
        e.apply()
        bump()
    }

    companion object {
        @Volatile private var instance: Prefs? = null
        fun get(context: Context): Prefs =
            instance ?: synchronized(this) { instance ?: Prefs(context.applicationContext).also { instance = it } }
    }
}
