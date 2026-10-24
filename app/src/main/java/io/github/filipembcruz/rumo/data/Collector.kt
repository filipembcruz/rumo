package io.github.filipembcruz.rumo.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads the phone's own usage history (UsageStatsManager) and stores daily and hourly
 * totals per app in the local database. Nothing leaves the device.
 */
object Collector {
    private const val RESUMED = 1
    private const val PAUSED = 2
    private const val SCREEN_ON = 15
    private const val SCREEN_OFF = 16
    private const val UNLOCKED = 18
    private const val SHUTDOWN = 26
    private const val BACKFILL_DAYS = 30L
    private const val LOOKBACK_MS = 3 * 60 * 60 * 1000L
    private const val HOUR = 3_600_000L

    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private class DayData(
        val apps: HashMap<String, AppDay>,
        val hourly: HashMap<String, LongArray>,
        val unlocks: Int,
        val firstUse: Long,
    )

    /** Recomputes today and yesterday, and fills older missing days the system still remembers. */
    @Synchronized
    fun sync(context: Context, full: Boolean = true) {
        if (!hasPermission(context)) return
        val db = Db.get(context)
        val excluded = excluded(context)
        val today = LocalDate.now()
        val last = if (full) BACKFILL_DAYS else 1L
        for (i in 0..last) {
            val day = today.minusDays(i)
            val key = day.toString()
            if (i > 1 && db.hasDay(key)) continue
            val d = collect(context, day, excluded)
            if (i > 1 && d.apps.isEmpty() && d.unlocks == 0) continue
            db.replaceDay(key, d.apps, d.hourly, d.unlocks, d.firstUse)
        }
    }

    @Volatile private var liveCache: Pair<Long, Map<String, Long>>? = null

    /** Today's ms per package, straight from the system (cached for [maxAgeMs]). */
    fun todayLive(context: Context, maxAgeMs: Long = 45_000): Map<String, Long> {
        val now = System.currentTimeMillis()
        liveCache?.let { (t, m) -> if (now - t < maxAgeMs) return m }
        if (!hasPermission(context)) return emptyMap()
        val d = collect(context, LocalDate.now(), excluded(context))
        val m = d.apps.mapValues { it.value.ms }
        liveCache = now to m
        return m
    }

    /** Foreground ms per package between [from] and now (any length, e.g. the current battery cycle). */
    fun usageSince(context: Context, from: Long): Map<String, Long> {
        if (!hasPermission(context)) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val excluded = excluded(context)
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(from - LOOKBACK_MS, now)
        val out = HashMap<String, Long>()
        var current: String? = null
        var start = 0L
        fun close(at: Long) {
            val p = current ?: return
            val s = maxOf(start, from)
            if (at > s && p !in excluded) out[p] = (out[p] ?: 0L) + (at - s)
            current = null
        }
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                RESUMED -> { if (current != e.packageName) close(e.timeStamp); if (current == null) { current = e.packageName; start = e.timeStamp } }
                PAUSED -> if (current == e.packageName) close(e.timeStamp)
                SCREEN_OFF, SHUTDOWN -> close(e.timeStamp)
            }
        }
        close(now)
        return out
    }

    private fun collect(context: Context, day: LocalDate, excluded: Set<String>): DayData {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val windowEnd = minOf(end, now)
        val apps = HashMap<String, AppDay>()
        val hourly = HashMap<String, LongArray>()
        if (windowEnd <= start) return DayData(apps, hourly, 0, 0)

        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return DayData(apps, hourly, 0, 0)
        val events = usm.queryEvents(start - LOOKBACK_MS, windowEnd)
        var unlocks = 0
        var screenOns = 0
        var firstUse = 0L
        var current: String? = null
        var currentStart = 0L
        var lastForeground: String? = null

        fun addSpan(pkg: String, s: Long, e: Long) {
            if (e <= s) return
            apps.getOrPut(pkg) { AppDay() }.ms += e - s
            val arr = hourly.getOrPut(pkg) { LongArray(24) }
            var t = s
            while (t < e) {
                val h = ((t - start) / HOUR).toInt().coerceIn(0, 23)
                val hourEnd = if (h == 23) e else minOf(e, start + (h + 1) * HOUR)
                arr[h] += hourEnd - t
                t = hourEnd
            }
        }

        fun close(at: Long) {
            val pkg = current ?: return
            val s = maxOf(currentStart, start)
            val e = minOf(at, windowEnd)
            if (pkg !in excluded) addSpan(pkg, s, e)
            current = null
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val ts = event.timeStamp
            when (event.eventType) {
                RESUMED -> {
                    val pkg = event.packageName
                    if (current != pkg) close(ts)
                    if (current == null) {
                        current = pkg
                        currentStart = ts
                    }
                    if (lastForeground != pkg && ts >= start && pkg !in excluded) {
                        apps.getOrPut(pkg) { AppDay() }.opens++
                        if (firstUse == 0L) firstUse = ts
                    }
                    lastForeground = pkg
                }
                PAUSED -> if (current == event.packageName) close(ts)
                SCREEN_OFF, SHUTDOWN -> {
                    close(ts)
                    lastForeground = null
                }
                UNLOCKED -> if (ts >= start) unlocks++
                SCREEN_ON -> if (ts >= start) screenOns++
            }
        }
        close(windowEnd)
        return DayData(apps, hourly, if (unlocks > 0) unlocks else screenOns, firstUse)
    }

    @Volatile private var excludedCache: Set<String>? = null

    /** Rumo itself, home-screen launchers and the system UI are not "app usage". */
    fun excluded(context: Context): Set<String> = excludedCache ?: run {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = context.packageManager.queryIntentActivities(home, 0).mapNotNull { it.activityInfo?.packageName }
        (launchers + context.packageName + "com.android.systemui" + "android").toSet().also { excludedCache = it }
    }
}
