package io.github.filipembcruz.rumo.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.ui.InterventionActivity

/** Starts the interrupt screen for a decision (shared by both blocking engines). */
object Interventions {
    @Volatile private var lastLaunch = 0L
    @Volatile private var lastKey = ""

    fun launch(context: Context, d: Decision, pkg: String) {
        val now = System.currentTimeMillis()
        val key = "${d.reason}:${d.target}"
        if (key == lastKey && now - lastLaunch < 4000) return
        lastLaunch = now
        lastKey = key
        val intent = Intent(context, InterventionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(InterventionActivity.EXTRA_REASON, d.reason.name)
            .putExtra(InterventionActivity.EXTRA_KIND, d.targetKind.id)
            .putExtra(InterventionActivity.EXTRA_TARGET, d.target)
            .putExtra(InterventionActivity.EXTRA_PKG, pkg)
            .putExtra(InterventionActivity.EXTRA_USED, d.usedMs)
            .putExtra(InterventionActivity.EXTRA_LIMIT, d.limit?.todayMinutes ?: 0)
            .putExtra(InterventionActivity.EXTRA_SCHEDULE, d.schedule?.name ?: "")
        runCatching { context.startActivity(intent) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            runCatching { io.github.filipembcruz.rumo.work.RumoWorker.schedule(context) }
        }
    }
}
