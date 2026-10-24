package io.github.filipembcruz.rumo.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    fun duration(ms: Long): String {
        val minutes = ms / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h h $m min"
            h > 0 -> "$h h"
            else -> "$m min"
        }
    }

    fun hours(ms: Long): Double = ms / 3_600_000.0

    /** "+12% vs período anterior" style text, or null when there is nothing to compare. */
    fun change(current: Long, previous: Long): String? {
        if (previous <= 0) return null
        val pct = ((current - previous) * 100.0 / previous).roundToInt()
        return when {
            pct == 0 -> "igual ao período anterior"
            pct > 0 -> "+$pct% vs período anterior"
            else -> "−${abs(pct)}% vs período anterior"
        }
    }

    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale("pt", "BR"))
    private val longDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "BR"))

    fun range(from: LocalDate, to: LocalDate): String =
        if (from.year == to.year) "${from.format(shortDate)} – ${to.format(longDate)}"
        else "${from.format(longDate)} – ${to.format(longDate)}"
}
