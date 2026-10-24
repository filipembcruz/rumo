package io.github.filipembcruz.rumo.data

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** One bar of a chart: a day or a month. */
data class Bucket(val label: String, val ms: Long, val isCurrent: Boolean)

data class AppTotal(val pkg: String, val ms: Long, val opens: Int)

data class PeriodStats(
    val from: LocalDate,
    val to: LocalDate,
    val buckets: List<Bucket>,
    val total: Long,
    val dailyAverage: Long,
    val previousTotal: Long,
    val unlocks: Int,
    val apps: List<AppTotal>,
    val trackedDays: Int,
)

enum class Grouping { DAY, MONTH }

object Stats {

    fun load(context: Context, from: LocalDate, to: LocalDate, grouping: Grouping): PeriodStats {
        val db = UsageDb.get(context)
        val f = from.toString()
        val t = to.toString()
        val perDay = db.totalsByDay(f, t)
        val today = LocalDate.now()

        val buckets = when (grouping) {
            Grouping.DAY -> generateSequence(from) { it.plusDays(1) }
                .takeWhile { !it.isAfter(to) }
                .map { d -> Bucket(dayLabel(d, from, to), perDay[d.toString()] ?: 0L, d == today) }
                .toList()
            Grouping.MONTH -> generateSequence(YearMonth.from(from)) { it.plusMonths(1) }
                .takeWhile { !it.isAfter(YearMonth.from(to)) }
                .map { ym ->
                    val sum = perDay.filterKeys { it.startsWith(ym.toString()) }.values.sum()
                    Bucket(MONTHS[ym.monthValue - 1], sum, ym == YearMonth.from(today))
                }
                .toList()
        }

        val total = perDay.values.sum()
        // Average only over days that have data and are not in the future.
        val firstTracked = db.firstDay()?.let { LocalDate.parse(it) }
        val effectiveFrom = if (firstTracked != null && firstTracked.isAfter(from)) firstTracked else from
        val effectiveTo = if (to.isAfter(today)) today else to
        val days = (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) + 1).coerceAtLeast(1)

        val length = ChronoUnit.DAYS.between(from, to) + 1
        val prevTo = from.minusDays(1)
        val prevFrom = prevTo.minusDays(length - 1)
        val previousTotal = db.totalsByDay(prevFrom.toString(), prevTo.toString()).values.sum()

        val opens = db.opensByApp(f, t)
        val apps = db.topApps(f, t).map { (pkg, ms) -> AppTotal(pkg, ms, opens[pkg] ?: 0) }

        return PeriodStats(
            from = from,
            to = to,
            buckets = buckets,
            total = total,
            dailyAverage = total / days,
            previousTotal = previousTotal,
            unlocks = db.unlocks(f, t),
            apps = apps,
            trackedDays = days.toInt(),
        )
    }

    private fun dayLabel(d: LocalDate, from: LocalDate, to: LocalDate): String {
        val length = ChronoUnit.DAYS.between(from, to) + 1
        return if (length <= 7) WEEKDAYS[d.dayOfWeek.value - 1] else d.dayOfMonth.toString()
    }

    private val WEEKDAYS = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
    private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
}
