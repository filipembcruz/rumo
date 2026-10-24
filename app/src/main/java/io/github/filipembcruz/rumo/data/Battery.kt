package io.github.filipembcruz.rumo.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.github.filipembcruz.rumo.core.Prefs

/**
 * Battery estimate per app for the current discharge cycle (since the phone was unplugged).
 * Android doesn't let regular apps read real per-app battery stats, so Rumo splits the
 * battery drained during the cycle by each app's screen time. It's an estimate.
 */
object Battery {
    data class Cycle(val startMs: Long, val startLevel: Int, val level: Int) {
        val drained: Int get() = (startLevel - level).coerceAtLeast(0)
    }

    data class Estimate(val cycle: Cycle, val shares: Map<String, Float>, val screenMs: Long)

    private fun read(context: Context): Pair<Int, Boolean>? {
        val i = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull() ?: return null
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return null
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        return (level * 100 / scale) to plugged
    }

    /** Call often (app open, worker, services): tracks where the current cycle started. */
    fun sample(context: Context) {
        val (level, plugged) = read(context) ?: return
        val prefs = Prefs.get(context)
        if (plugged) {
            prefs.cycleStart = 0 // charging: a new cycle starts when unplugged
        } else if (prefs.cycleStart <= 0 || level > prefs.cycleLevel) {
            prefs.cycleStart = System.currentTimeMillis()
            prefs.cycleLevel = level
        }
    }

    fun cycle(context: Context): Cycle? {
        sample(context)
        val prefs = Prefs.get(context)
        val (level, plugged) = read(context) ?: return null
        if (plugged || prefs.cycleStart <= 0) return null
        return Cycle(prefs.cycleStart, prefs.cycleLevel, level)
    }

    fun estimate(context: Context): Estimate? {
        val cy = cycle(context) ?: return null
        val use = Collector.usageSince(context, cy.startMs)
        val total = use.values.sum()
        if (total <= 0 || cy.drained <= 0) return Estimate(cy, emptyMap(), total)
        return Estimate(cy, use.mapValues { it.value.toFloat() / total * cy.drained }, total)
    }
}
