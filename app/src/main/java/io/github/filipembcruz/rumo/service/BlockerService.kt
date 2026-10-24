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

/**
 * Blocking without accessibility: watches the foreground app through usage events and,
 * with "Display over other apps", shows the interrupt screen. Websites can't be seen in this mode.
 * Runs only when the accessibility service is off.
 */
class BlockerService : Service() {
    private lateinit var thread: HandlerThread
    private lateinit var bg: Handler
    private var lastPkg: String? = null
    private var ticks = 0

    private val loop = object : Runnable {
        override fun run() {
            val interactive = runCatching { getSystemService(PowerManager::class.java)?.isInteractive ?: true }.getOrDefault(true)
            runCatching { if (interactive) step() else lastPkg = null }
            if (running) bg.postDelayed(this, if (interactive) 1200 else 5000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val s = Notifier.strings(this)
        val n = Notifier.build(this, Notifier.CH_MONITOR, s.blockerT, s.blockerB, ongoing = true)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.ID_MONITOR, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.ID_MONITOR, n)
        }
        running = true
        thread = HandlerThread("rumo-blocker").apply { start() }
        bg = Handler(thread.looper)
        bg.post(loop)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running = false
        if (::bg.isInitialized) { bg.removeCallbacks(loop); thread.quitSafely() }
        super.onDestroy()
    }

    private fun step() {
        if (RumoAccessibilityService.running || !Perms.overlay(this) || !Collector.hasPermission(this)) {
            stopSelf(); return
        }
        val pkg = foreground() ?: return
        val changed = pkg != lastPkg
        lastPkg = pkg
        ticks++
        if (pkg == packageName) return
        if (changed || ticks % 12 == 0) {
            val d = Rules.evaluate(this, pkg, null, Collector.todayLive(this), emptyMap()) ?: return
            Interventions.launch(this, d, pkg)
        }
    }

    /** The app most recently brought to the front (from the last minute of usage events). */
    private fun foreground(): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000, now)
        val e = UsageEvents.Event()
        var top: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == 1) top = e.packageName // ACTIVITY_RESUMED
        }
        return top ?: lastPkg
    }

    companion object {
        @Volatile var running = false

        /** Start or stop the fallback blocker depending on permissions. Safe to call often. */
        fun ensure(context: Context) {
            val want = Perms.overlay(context) && !Perms.accessibility(context) && Collector.hasPermission(context)
            if (want && !running) {
                runCatching {
                    val i = Intent(context, BlockerService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
                }
            } else if (!want && running) {
                runCatching { context.stopService(Intent(context, BlockerService::class.java)) }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            BlockerService.ensure(context)
        }
    }
}
