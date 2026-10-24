package io.github.filipembcruz.rumo.core

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.filipembcruz.rumo.data.ActivityLog
import io.github.filipembcruz.rumo.data.Categorizer
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Goal
import io.github.filipembcruz.rumo.data.Limit
import io.github.filipembcruz.rumo.data.Schedule
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.work.FocusEndWorker
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Mutations triggered from the UI. Safe to call from a background thread. */
object Actions {
    fun startFocus(context: Context, minutes: Int, goalId: Long?) {
        val prefs = Prefs.get(context)
        val now = System.currentTimeMillis()
        prefs.focusStart = now
        prefs.focusUntil = now + minutes * 60_000L
        prefs.focusGoalId = goalId ?: -1
        Db.get(context).startFocus(now, minutes, goalId)
        val req = OneTimeWorkRequestBuilder<FocusEndWorker>().setInitialDelay(minutes.toLong(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniqueWork("focus-end", ExistingWorkPolicy.REPLACE, req)
    }

    fun stopFocus(context: Context, completed: Boolean) {
        val prefs = Prefs.get(context)
        if (prefs.focusUntil == 0L) return
        prefs.focusUntil = 0
        Db.get(context).endFocus(System.currentTimeMillis(), completed)
        if (!completed) WorkManager.getInstance(context).cancelUniqueWork("focus-end")
    }

    fun saveLimit(context: Context, l: Limit) { Db.get(context).saveLimit(l) }
    fun deleteLimit(context: Context, id: Long) { Db.get(context).deleteLimit(id) }

    fun saveSchedule(context: Context, s: Schedule) { Db.get(context).saveSchedule(s) }
    fun deleteSchedule(context: Context, id: Long) { Db.get(context).deleteSchedule(id) }

    fun setPause(context: Context, pkg: String, on: Boolean) { Db.get(context).setPause(pkg, on) }

    fun setCategory(context: Context, kind: TargetKind, target: String, cat: String) {
        Db.get(context).setOverride(if (kind == TargetKind.APP) "app" else "site", target, cat)
        Categorizer.invalidate()
    }

    fun logActivity(context: Context, goalId: Long?, label: String, minutes: Int) {
        Db.get(context).addActivity(ActivityLog(0, LocalDate.now().toString(), goalId, label, minutes, System.currentTimeMillis()))
    }

    fun deleteActivity(context: Context, id: Long) { Db.get(context).deleteActivity(id) }

    fun saveGoal(context: Context, g: Goal) { Db.get(context).saveGoal(g) }
    fun deleteGoal(context: Context, id: Long) { Db.get(context).deleteGoal(id) }

    fun toggleFocusApp(context: Context, pkg: String, on: Boolean) {
        val prefs = Prefs.get(context)
        prefs.focusApps = if (on) prefs.focusApps + pkg else prefs.focusApps - pkg
    }

    fun wipe(context: Context) {
        Db.get(context).wipe()
        Categorizer.invalidate()
    }
}
