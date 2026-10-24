package io.github.filipembcruz.rumo.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Categorizer
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.Fmt
import java.time.LocalDate

/**
 * Sees which app is in front and which website the browser shows. Used to count time
 * per website and to show the block / pause screen. All data stays on the device.
 */
class RumoAccessibilityService : AccessibilityService() {
    private lateinit var worker: HandlerThread
    private lateinit var bg: Handler
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var current: String? = null
    @Volatile private var domain: String? = null
    private var domainFlushedAt = 0L
    private var lastUrlCheck = 0L
    private var distractingSince = 0L
    private var lastNudge = 0L
    private var screenOn = true
    private var imes: Set<String> = emptySet()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            bg.post {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> { screenOn = false; closeWeb(); current = null; distractingSince = 0 }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> screenOn = true
                }
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            runCatching { tick() }
            bg.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        worker = HandlerThread("rumo-a11y").apply { start() }
        bg = Handler(worker.looper)
        imes = runCatching {
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }?.toSet()
        }.getOrNull() ?: emptySet()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(screenReceiver, filter)
        }
        bg.postDelayed(ticker, TICK_MS)
        lastMonitorPref = Prefs.get(this).monitoringNotification
        runCatching { updateMonitorNotification() }
        running = true
        runCatching { stopService(Intent(this, BlockerService::class.java)) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        val type = event.eventType
        bg.post {
            runCatching {
                when (type) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindow(pkg)
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (pkg == current && Browsers.isSupported(pkg)) onBrowserContent(pkg)
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running = false
        runCatching { unregisterReceiver(screenReceiver) }
        if (::bg.isInitialized) { bg.post { closeWeb() }; bg.removeCallbacks(ticker); worker.quitSafely() }
        Notifier.cancel(this, Notifier.ID_MONITOR)
        super.onDestroy()
    }

    // ------------------------------------------------------------------ events
    private fun onWindow(pkg: String) {
        if (pkg in imes || pkg == "com.android.systemui") return
        if (pkg == current) return
        if (current != null && Browsers.isSupported(current!!)) closeWeb()
        current = pkg
        screenOn = true
        if (pkg == packageName) return
        val cat = Categorizer.app(this, pkg)
        if (!cat.distracting) distractingSince = 0 else if (distractingSince == 0L) distractingSince = System.currentTimeMillis()
        if (Browsers.isSupported(pkg)) onBrowserContent(pkg, force = true)
        check(pkg, if (Browsers.isSupported(pkg)) domain else null)
    }

    private fun onBrowserContent(pkg: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastUrlCheck < 800) return
        lastUrlCheck = now
        val d = Browsers.readDomain(rootInActiveWindow, pkg) ?: return
        if (d == domain) return
        closeWeb()
        domain = d
        domainFlushedAt = now
        Db.get(this).addWeb(LocalDate.now().toString(), d, 0, visit = true)
        val cat = Categorizer.site(this, d)
        if (cat.distracting) { if (distractingSince == 0L) distractingSince = now } else distractingSince = 0
        check(pkg, d)
    }

    private fun closeWeb() {
        val d = domain ?: return
        val now = System.currentTimeMillis()
        val ms = now - domainFlushedAt
        if (ms in 1..(6 * 3_600_000L)) Db.get(this).addWeb(LocalDate.now().toString(), d, ms, visit = false)
        domain = null
        domainFlushedAt = now
    }

    private fun flushWeb() {
        val d = domain ?: return
        val now = System.currentTimeMillis()
        val ms = now - domainFlushedAt
        if (ms > 0) Db.get(this).addWeb(LocalDate.now().toString(), d, ms, visit = false)
        domainFlushedAt = now
    }

    // ------------------------------------------------------------------ periodic
    private var lastMonitorPref: Boolean? = null

    private fun tick() {
        val mp = Prefs.get(this).monitoringNotification
        if (mp != lastMonitorPref) { lastMonitorPref = mp; updateMonitorNotification() }
        val pkg = current ?: return
        if (!screenOn || pkg == packageName) return
        flushWeb()
        check(pkg, if (Browsers.isSupported(pkg)) domain else null)
        alerts()
        nudge(pkg)
    }

    private fun siteToday(): Map<String, Long> {
        val key = LocalDate.now().toString()
        return Db.get(this).webTotals(key, key).associate { it.first to it.second }
    }

    private fun check(pkg: String, d: String?) {
        if (Rules.isSafe(this, pkg)) return
        val apps = Collector.todayLive(this)
        val decision = Rules.evaluate(this, pkg, d, apps, siteToday()) ?: return
        main.post { Interventions.launch(this, decision, pkg) }
    }

    private fun alerts() {
        val prefs = Prefs.get(this)
        if (!prefs.limitAlerts) return
        val apps = Collector.todayLive(this)
        val sites = siteToday()
        val s = Notifier.strings(this)
        for (l in Db.get(this).limits()) {
            if (!l.enabled) continue
            val used = Rules.used(this, l, apps, sites)
            val limitMs = l.minutes * 60_000L
            val name = when (l.kind) {
                TargetKind.APP -> AppNames.label(this, l.target)
                TargetKind.SITE -> l.target
                TargetKind.CATEGORY -> s.cat(io.github.filipembcruz.rumo.data.Cat.of(l.target))
            }
            val id = 1000 + (l.id % 500).toInt()
            if (used >= limitMs) {
                if (prefs.markOnce("l100:${l.id}")) Notifier.post(this, Notifier.CH_LIMITS, id, s.nLimitReachedT(name), s.nLimitReachedB(Fmt.dur(limitMs), l.block))
            } else if (used >= limitMs * 0.8) {
                if (prefs.markOnce("l80:${l.id}")) Notifier.post(this, Notifier.CH_LIMITS, id, s.nLimitNearT(name), s.nLimitNearB(Fmt.dur(limitMs - used)))
            }
        }
    }

    private fun nudge(pkg: String) {
        val prefs = Prefs.get(this)
        if (!prefs.nudges || distractingSince == 0L) return
        val now = System.currentTimeMillis()
        val run = now - distractingSince
        if (run < prefs.nudgeAfterMin * 60_000L || now - lastNudge < 30 * 60_000L) return
        lastNudge = now
        val s = Notifier.strings(this)
        val name = domain ?: AppNames.label(this, pkg)
        val goal = Db.get(this).goals().firstOrNull()?.title
        Notifier.post(this, Notifier.CH_NUDGES, 2000, s.nNudgeT(name, Fmt.dur(run)), s.nNudgeB(goal), tab = "growth")
    }

    private fun updateMonitorNotification() {
        val prefs = Prefs.get(this)
        val s = Notifier.strings(this)
        if (prefs.monitoringNotification) {
            Notifier.post(this, Notifier.CH_MONITOR, Notifier.ID_MONITOR, s.monitorT, s.monitorB, ongoing = true)
        } else Notifier.cancel(this, Notifier.ID_MONITOR)
    }

    companion object {
        private const val TICK_MS = 20_000L
        @Volatile var running = false
    }
}
