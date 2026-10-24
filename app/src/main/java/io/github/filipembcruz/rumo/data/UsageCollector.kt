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
 * Reads the phone's own usage history (UsageStatsManager) and stores daily totals
 * per app in the local database. Nothing leaves the device.
 */
object UsageCollector {

    // Raw event type values (stable across Android versions) to avoid API-level lint noise.
    private const val RESUMED = 1          // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
    private const val PAUSED = 2           // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
    private const val SCREEN_OFF = 16      // SCREEN_NON_INTERACTIVE
    private const val UNLOCKED = 18        // KEYGUARD_HIDDEN
    private const val SHUTDOWN = 26        // DEVICE_SHUTDOWN

    private const val BACKFILL_DAYS = 30L
    private const val LOOKBACK_MS = 3 * 60 * 60 * 1000L

    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Recomputes today and yesterday, and fills any older missing day the system still remembers. */
    @Synchronized
    fun sync(context: Context) {
        if (!hasPermission(context)) return
        val db = UsageDb.get(context)
        val excluded = excludedPackages(context)
        val today = LocalDate.now()
        for (i in 0..BACKFILL_DAYS) {
            val day = today.minusDays(i)
            val key = day.toString()
            if (i > 1 && db.hasDay(key)) continue
            val (apps, unlocks) = collectDay(context, day, excluded)
            if (i > 1 && apps.isEmpty() && unlocks == 0) continue
            db.replaceDay(key, apps, unlocks)
        }
    }

    private fun collectDay(
        context: Context,
        day: LocalDate,
        excluded: Set<String>,
    ): Pair<Map<String, AppDay>, Int> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val windowEnd = minOf(end, now)
        if (windowEnd <= start) return emptyMap<String, AppDay>() to 0

        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(start - LOOKBACK_MS, windowEnd)
        val result = HashMap<String, AppDay>()
        var unlocks = 0
        var current: String? = null
        var currentStart = 0L
        var lastForeground: String? = null

        fun close(at: Long) {
            val pkg = current ?: return
            val s = maxOf(currentStart, start)
            val e = minOf(at, windowEnd)
            if (e > s && pkg !in excluded) result.getOrPut(pkg) { AppDay() }.ms += e - s
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
                        result.getOrPut(pkg) { AppDay() }.opens++
                    }
                    lastForeground = pkg
                }
                PAUSED -> if (current == event.packageName) close(ts)
                SCREEN_OFF, SHUTDOWN -> {
                    close(ts)
                    lastForeground = null
                }
                UNLOCKED -> if (ts >= start) unlocks++
            }
        }
        close(windowEnd)
        return result to unlocks
    }

    /** Rumo itself, home-screen launchers and the system UI are not "app usage". */
    private fun excludedPackages(context: Context): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = context.packageManager.queryIntentActivities(home, 0)
            .mapNotNull { it.activityInfo?.packageName }
        return (launchers + context.packageName + "com.android.systemui").toSet()
    }
}
