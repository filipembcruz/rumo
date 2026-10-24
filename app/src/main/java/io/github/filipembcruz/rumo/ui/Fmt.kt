package io.github.filipembcruz.rumo.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    fun dur(ms: Long): String {
        if (ms <= 0) return "0m"
        val minutes = ms / 60_000
        if (minutes < 1) return "<1m"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /** Big hero number split into value + unit parts, e.g. ("3", "h", "24", "m"). */
    fun durParts(ms: Long): List<Pair<String, String>> {
        val minutes = (ms / 60_000).coerceAtLeast(0)
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) listOf("$h" to "h", "$m" to "m") else listOf("$m" to "m")
    }

    fun hours(ms: Long): Double = ms / 3_600_000.0

    fun hoursText(h: Double): String = when {
        h >= 100 -> "${h.roundToInt()}h"
        h >= 10 -> "%.0fh".format(h)
        else -> "%.1fh".format(h)
    }

    fun pct(current: Long, previous: Long): Int? {
        if (previous <= 0) return null
        return ((current - previous) * 100.0 / previous).roundToInt()
    }

    fun signedPct(p: Int): String = if (p > 0) "+$p%" else if (p < 0) "−${abs(p)}%" else "0%"

    fun clock(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    fun clockMin(min: Int): String = "%02d:%02d".format((min / 60) % 24, min % 60)

    fun countdown(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    fun day(d: LocalDate, locale: Locale): String = d.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))

    fun range(from: LocalDate, to: LocalDate, locale: Locale): String {
        val short = DateTimeFormatter.ofPattern("d MMM", locale)
        val long = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return when {
            from == to -> from.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))
            from.year == to.year -> "${from.format(short)} – ${to.format(long)}"
            else -> "${from.format(long)} – ${to.format(long)}"
        }
    }

    fun month(d: LocalDate, locale: Locale): String =
        d.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
}
