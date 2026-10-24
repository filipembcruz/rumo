package io.github.filipembcruz.rumo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.service.Notifier
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.Fmt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Every 15 minutes: save usage, send summaries/reminders, and limit alerts when the service is off. */
class RumoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        runCatching { Collector.sync(ctx, full = LocalTime.now().minute < 15) }
        runCatching { io.github.filipembcruz.rumo.service.BlockerService.ensure(ctx) }
        runCatching { summaries(ctx) }
        runCatching { if (!Perms.accessibility(ctx)) limitAlerts(ctx) }
        return Result.success()
    }

    private fun summaries(ctx: Context) {
        val prefs = Prefs.get(ctx)
        val s = Notifier.strings(ctx)
        val today = LocalDate.now()
        val now = LocalTime.now()
        if (prefs.dailySummary && now.hour >= prefs.summaryHour && prefs.lastSummaryDay != today.toString()) {
            prefs.lastSummaryDay = today.toString()
            val m = Repo.today(ctx)
            val top = m.apps.firstOrNull()?.let { AppNames.label(ctx, it.pkg) }
            Notifier.post(ctx, Notifier.CH_REPORTS, 3000, s.nDailyT(Fmt.dur(m.total)),
                s.nDailyB(top, m.focusScore, m.total <= m.goalMs), tab = "insights")
        }
        if (prefs.weeklyReport && today.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= prefs.summaryHour && prefs.lastWeeklyDay != today.toString()) {
            prefs.lastWeeklyDay = today.toString()
            val db = Db.get(ctx)
            val week = db.totalsByDay(today.minusDays(6).toString(), today.toString()).values.sum()
            val prev = db.totalsByDay(today.minusDays(13).toString(), today.minusDays(7).toString()).values.sum()
            Notifier.post(ctx, Notifier.CH_REPORTS, 3001, s.nWeeklyT(Fmt.dur(week)), s.nWeeklyB(Fmt.pct(week, prev)), tab = "insights")
        }
        if (prefs.bedtimeReminder && prefs.lastBedtimeDay != today.toString()) {
            val min = now.hour * 60 + now.minute
            val b = prefs.bedtimeMin
            if (min >= b && min < b + 60) {
                prefs.lastBedtimeDay = today.toString()
                Notifier.post(ctx, Notifier.CH_NUDGES, 3002, s.nBedtimeT, s.nBedtimeB)
            }
        }
    }

    private fun limitAlerts(ctx: Context) {
        val prefs = Prefs.get(ctx)
        if (!prefs.limitAlerts) return
        val s = Notifier.strings(ctx)
        for (st in Repo.limitStatuses(ctx)) {
            val l = st.limit
            if (!l.enabled) continue
            val name = when (l.kind) {
                TargetKind.APP -> AppNames.label(ctx, l.target)
                TargetKind.SITE -> l.target
                TargetKind.CATEGORY -> s.cat(Cat.of(l.target))
            }
            val limitMs = l.minutes * 60_000L
            val id = 1000 + (l.id % 500).toInt()
            if (st.usedMs >= limitMs) {
                if (prefs.markOnce("l100:${l.id}")) Notifier.post(ctx, Notifier.CH_LIMITS, id, s.nLimitReachedT(name), s.nLimitReachedB(Fmt.dur(limitMs), false))
            } else if (st.usedMs >= limitMs * 0.8) {
                if (prefs.markOnce("l80:${l.id}")) Notifier.post(ctx, Notifier.CH_LIMITS, id, s.nLimitNearT(name), s.nLimitNearB(Fmt.dur(limitMs - st.usedMs)))
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<RumoWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("rumo-periodic", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}

/** Fires when a focus session's time is up. */
class FocusEndWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = Prefs.get(ctx)
        if (prefs.focusUntil > 0 && prefs.focusUntil <= System.currentTimeMillis() + 30_000) {
            val minutes = ((prefs.focusUntil - prefs.focusStart) / 60_000).toInt()
            Actions.stopFocus(ctx, completed = true)
            val s = Notifier.strings(ctx)
            Notifier.post(ctx, Notifier.CH_FOCUS, 4000, s.nFocusDoneT, s.nFocusDoneB(minutes), tab = "growth")
        }
        return Result.success()
    }
}
