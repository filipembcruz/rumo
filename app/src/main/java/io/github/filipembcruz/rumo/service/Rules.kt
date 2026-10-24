package io.github.filipembcruz.rumo.service

import android.content.Context
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Categorizer
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Limit
import io.github.filipembcruz.rumo.data.Schedule
import io.github.filipembcruz.rumo.data.TargetKind
import java.time.LocalDateTime

enum class BlockReason { LIMIT, FOCUS, SCHEDULE, PAUSE, ADULT }

data class Decision(
    val reason: BlockReason,
    val targetKind: TargetKind,
    val target: String,
    val limit: Limit? = null,
    val usedMs: Long = 0,
    val schedule: Schedule? = null,
)

/** Decides whether the app / site on screen should be interrupted. */
object Rules {
    /** Never interrupt these: phone, settings, launchers, ourselves. */
    private val safe = setOf(
        "com.android.settings", "com.android.systemui", "com.google.android.dialer", "com.android.dialer",
        "com.samsung.android.dialer", "com.android.phone", "com.google.android.apps.maps",
        "com.android.emergency", "com.google.android.inputmethod.latin", "com.android.vending",
    )

    fun isSafe(context: Context, pkg: String): Boolean =
        pkg == context.packageName || pkg in safe || pkg in io.github.filipembcruz.rumo.data.Collector.excluded(context)

    fun scheduleActive(s: Schedule, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (!s.enabled) return false
        val min = now.hour * 60 + now.minute
        val dow = now.dayOfWeek.value - 1
        return if (s.startMin <= s.endMin) {
            (s.days and (1 shl dow)) != 0 && min >= s.startMin && min < s.endMin
        } else {
            // overnight: the part after midnight belongs to the previous day's schedule
            val prev = (dow + 6) % 7
            ((s.days and (1 shl dow)) != 0 && min >= s.startMin) || ((s.days and (1 shl prev)) != 0 && min < s.endMin)
        }
    }

    /**
     * @param appToday ms used today per package (live)
     * @param siteToday ms used today per domain (live)
     */
    fun evaluate(
        context: Context,
        pkg: String,
        domain: String?,
        appToday: Map<String, Long>,
        siteToday: Map<String, Long>,
    ): Decision? {
        if (isSafe(context, pkg)) return null
        val prefs = Prefs.get(context)
        val db = Db.get(context)
        val appCat = Categorizer.app(context, pkg)
        val siteCat = domain?.let { Categorizer.site(context, it) }
        val now = System.currentTimeMillis()

        // 0. adult content (+18)
        if (prefs.blockAdult && domain != null && io.github.filipembcruz.rumo.data.KnownCategories.isAdult(domain)) {
            return Decision(BlockReason.ADULT, TargetKind.SITE, domain)
        }
        // 1. focus session
        if (prefs.focusActive) {
            val cats = prefs.focusCats
            if (pkg in prefs.focusApps || appCat.id in cats) return Decision(BlockReason.FOCUS, TargetKind.APP, pkg)
            if (domain != null && siteCat != null && siteCat.id in cats) return Decision(BlockReason.FOCUS, TargetKind.SITE, domain)
        }
        // 2. schedules
        for (s in db.schedules()) if (scheduleActive(s)) {
            if (pkg in s.apps || appCat.id in s.cats) return Decision(BlockReason.SCHEDULE, TargetKind.APP, pkg, schedule = s)
            if (domain != null && siteCat != null && siteCat.id in s.cats) return Decision(BlockReason.SCHEDULE, TargetKind.SITE, domain, schedule = s)
        }
        // 3. limits that block
        for (l in db.limits()) {
            if (!l.enabled || !l.block) continue
            val matches = when (l.kind) {
                TargetKind.APP -> l.target == pkg
                TargetKind.SITE -> domain != null && (domain == l.target || domain.endsWith(".${l.target}"))
                TargetKind.CATEGORY -> l.target == appCat.id || (siteCat != null && l.target == siteCat.id)
            }
            if (!matches) continue
            if (prefs.passUntil("extra:${l.kind.id}:${l.target}") > now) continue
            val used = used(context, l, appToday, siteToday)
            if (used >= l.minutes * 60_000L) {
                return Decision(BlockReason.LIMIT, l.kind, l.target, l, used)
            }
        }
        // 4. mindful pause
        if (pkg in db.pauseApps() && prefs.passUntil("pause:$pkg") < now) return Decision(BlockReason.PAUSE, TargetKind.APP, pkg)
        return null
    }

    fun used(context: Context, l: Limit, appToday: Map<String, Long>, siteToday: Map<String, Long>): Long = when (l.kind) {
        TargetKind.APP -> appToday[l.target] ?: 0L
        TargetKind.SITE -> siteToday.filterKeys { it == l.target || it.endsWith(".${l.target}") }.values.sum()
        TargetKind.CATEGORY -> {
            val cat = Cat.of(l.target)
            appToday.filterKeys { Categorizer.app(context, it) == cat }.values.sum() +
                siteToday.filterKeys { Categorizer.site(context, it) == cat }.values.sum()
        }
    }
}
