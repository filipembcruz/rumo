package io.github.filipembcruz.rumo.data

import android.content.Context
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Builds the UI models from the local database. Call from a background thread. */
object Repo {
    private const val MIN = 60_000L

    private fun zone() = ZoneId.systemDefault()
    private fun startMs(d: LocalDate) = d.atStartOfDay(zone()).toInstant().toEpochMilli()

    // ------------------------------------------------------------------ today
    fun today(context: Context): TodayModel {
        val db = Db.get(context)
        val prefs = Prefs.get(context)
        val today = LocalDate.now()
        val key = today.toString()
        val apps = db.appTotals(key, key).map { (pkg, ms, opens) -> AppUse(pkg, ms, opens, Categorizer.app(context, pkg)) }
        val sites = db.webTotals(key, key).map { (d, ms, v) -> SiteUse(d, ms, v, Categorizer.site(context, d)) }
        val total = apps.sumOf { it.ms }
        val goalMs = prefs.dailyGoalMin * MIN
        val hourly = LongArray(24)
        db.hourly(key, key).forEach { (_, h, ms) -> hourly[h] += ms }
        val week = db.totalsByDay(today.minusDays(7).toString(), today.minusDays(1).toString())
        val avg7 = if (week.isEmpty()) 0L else week.values.sum() / week.size
        val (prod, dist) = balance(apps, sites)
        val pickups = db.unlocks(key, key)
        val streak = streak(context, goalMs)
        val cats = categories(apps, sites)
        val sitesOn = Perms.accessibility(context)
        val limits = limitStatuses(context)
        val focusScore = focusScore(total, goalMs, dist, pickups)

        val topDistracting = (apps.filter { it.cat.distracting }.map { it.pkg to it.ms } +
            sites.filter { it.cat.distracting }.map { it.domain to it.ms }).maxByOrNull { it.second }
        val browserMs = apps.filter { it.cat == Cat.BROWSER }.sumOf { it.ms }
        val hour = LocalTime.now().hour
        val insight = when {
            prefs.focusActive -> Insight(InsightKind.FOCUS_RUNNING, ms = prefs.focusUntil - System.currentTimeMillis())
            total < 2 * MIN -> Insight(if (hour < 12) InsightKind.MORNING_FRESH else InsightKind.NO_DATA)
            hour >= 23 || hour < 4 -> Insight(InsightKind.LATE_NIGHT, ms = total)
            total > goalMs -> Insight(InsightKind.OVER_GOAL, ms = total - goalMs)
            topDistracting != null && topDistracting.second > 45 * MIN ->
                Insight(InsightKind.TOP_DISTRACTING, subject = topDistracting.first, ms = topDistracting.second)
            !sitesOn && browserMs > 15 * MIN -> Insight(InsightKind.SITES_OFF, ms = browserMs)
            total > goalMs * 0.8 -> Insight(InsightKind.NEAR_GOAL, ms = goalMs - total)
            streak >= 3 -> Insight(InsightKind.STREAK, n = streak)
            prod > dist && prod > 20 * MIN -> Insight(InsightKind.GREAT_BALANCE, ms = prod)
            else -> Insight(InsightKind.UNDER_GOAL, ms = goalMs - total)
        }

        return TodayModel(
            total = total, goalMs = goalMs, yesterday = week[today.minusDays(1).toString()] ?: 0L, avg7 = avg7,
            pickups = pickups, firstUse = db.firstUse(key), productiveMs = prod, distractingMs = dist,
            focusScore = focusScore, hourly = hourly.toList(), apps = apps, sites = sites, cats = cats,
            focusUntil = prefs.focusUntil, streak = streak, sitesEnabled = sitesOn, insight = insight, limits = limits,
        )
    }

    fun focusScore(total: Long, goalMs: Long, distracting: Long, pickups: Int): Int {
        if (total <= 0) return 100
        val goalPart = if (total <= goalMs) 50.0 else 50.0 * goalMs / total
        val share = (distracting.toDouble() / total).coerceIn(0.0, 1.0)
        val balancePart = 30.0 * (1 - share)
        val pickupPart = 20.0 * (1 - ((pickups - 40) / 80.0).coerceIn(0.0, 1.0))
        return (goalPart + balancePart + pickupPart).roundToInt().coerceIn(0, 100)
    }

    /** productive and distracting ms; browser time is attributed to the sites visited. */
    private fun balance(apps: List<AppUse>, sites: List<SiteUse>): Pair<Long, Long> {
        var prod = 0L
        var dist = 0L
        for (a in apps) if (a.cat != Cat.BROWSER) {
            if (a.cat.productive) prod += a.ms
            if (a.cat.distracting) dist += a.ms
        }
        for (s in sites) {
            if (s.cat.productive) prod += s.ms
            if (s.cat.distracting) dist += s.ms
        }
        return prod to dist
    }

    private fun categories(apps: List<AppUse>, sites: List<SiteUse>): List<CatUse> {
        val map = HashMap<Cat, Long>()
        val siteMs = sites.sumOf { it.ms }
        for (a in apps) map[a.cat] = (map[a.cat] ?: 0L) + a.ms
        // move the part of browser time we can explain into the sites' categories
        val browser = map[Cat.BROWSER] ?: 0L
        if (browser > 0 && siteMs > 0) {
            val moved = min(browser, siteMs)
            val scale = moved.toDouble() / siteMs
            map[Cat.BROWSER] = browser - moved
            for (s in sites) map[s.cat] = (map[s.cat] ?: 0L) + (s.ms * scale).toLong()
        }
        return map.filter { it.value > 0 }.map { CatUse(it.key, it.value) }.sortedByDescending { it.ms }
    }

    fun streak(context: Context, goalMs: Long): Int {
        val db = Db.get(context)
        val today = LocalDate.now()
        val totals = db.totalsByDay(today.minusDays(400).toString(), today.toString())
        var n = 0
        var d = today.minusDays(1)
        while (true) {
            val t = totals[d.toString()] ?: break
            if (t > goalMs) break
            n++
            d = d.minusDays(1)
        }
        val t0 = totals[today.toString()] ?: 0L
        if (t0 in 1..goalMs) n++
        return n
    }

    private fun bestStreak(context: Context, goalMs: Long): Int {
        val db = Db.get(context)
        val today = LocalDate.now()
        val totals = db.totalsByDay(today.minusDays(400).toString(), today.toString())
        var best = 0
        var cur = 0
        var d = today.minusDays(400)
        while (!d.isAfter(today)) {
            val t = totals[d.toString()]
            if (t != null && t in 1..goalMs) { cur++; best = max(best, cur) } else cur = 0
            d = d.plusDays(1)
        }
        return best
    }

    // ------------------------------------------------------------------ limits
    fun limitStatuses(context: Context): List<LimitStatus> =
        Db.get(context).limits().map { LimitStatus(it, usedToday(context, it.kind, it.target)) }

    fun usedToday(context: Context, kind: TargetKind, target: String, live: Map<String, Long>? = null): Long {
        val db = Db.get(context)
        val key = LocalDate.now().toString()
        val apps: Map<String, Long> = live ?: db.appTotals(key, key).associate { it.first to it.second }
        return when (kind) {
            TargetKind.APP -> apps[target] ?: 0L
            TargetKind.SITE -> db.webTotals(key, key).firstOrNull { it.first == target }?.second ?: 0L
            TargetKind.CATEGORY -> {
                val cat = Cat.of(target)
                apps.entries.filter { Categorizer.app(context, it.key) == cat }.sumOf { it.value } +
                    db.webTotals(key, key).filter { Categorizer.site(context, it.first) == cat }.sumOf { it.second }
            }
        }
    }

    // ------------------------------------------------------------------ period
    fun bounds(range: Range, anchor: LocalDate, customFrom: LocalDate?, customTo: LocalDate?): Pair<LocalDate, LocalDate> = when (range) {
        Range.DAY -> anchor to anchor
        Range.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
        Range.MONTH -> anchor.withDayOfMonth(1).let { it to it.plusMonths(1).minusDays(1) }
        Range.YEAR -> anchor.withDayOfYear(1).let { it to it.plusYears(1).minusDays(1) }
        Range.CUSTOM -> (customFrom ?: anchor.minusDays(13)) to (customTo ?: anchor)
    }

    fun period(context: Context, range: Range, anchor: LocalDate, locale: Locale,
               customFrom: LocalDate? = null, customTo: LocalDate? = null): PeriodModel {
        val db = Db.get(context)
        val goalMs = Prefs.get(context).dailyGoalMin * MIN
        val (from, to) = bounds(range, anchor, customFrom, customTo)
        val today = LocalDate.now()
        val f = from.toString()
        val t = to.toString()
        val perDay = db.totalsByDay(f, t)
        val hourRows = db.hourly(f, t)
        val hourly = LongArray(24)
        hourRows.forEach { (_, h, ms) -> hourly[h] += ms }

        val bars: List<Long>
        val labels: List<String>
        var current = -1
        val byMonth = range == Range.YEAR || (range == Range.CUSTOM && ChronoUnit.DAYS.between(from, to) > 62)
        when {
            range == Range.DAY -> {
                bars = hourly.toList()
                labels = (0 until 24).map { if (it % 6 == 0) "%02d".format(it) else "" }
                if (from == today) current = LocalTime.now().hour
            }
            byMonth -> {
                val months = generateSequence(YearMonth.from(from)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(to)) }.toList()
                bars = months.map { ym -> perDay.filterKeys { it.startsWith(ym.toString()) }.values.sum() }
                labels = months.map { it.month.getDisplayName(TextStyle.NARROW, locale) }
                current = months.indexOf(YearMonth.from(today))
            }
            else -> {
                val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
                bars = days.map { perDay[it.toString()] ?: 0L }
                labels = if (days.size <= 7) days.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) }
                else days.map { if (it.dayOfMonth == 1 || it.dayOfMonth % 5 == 0) it.dayOfMonth.toString() else "" }
                current = days.indexOf(today)
            }
        }

        val total = perDay.values.sum()
        val firstTracked = db.firstDay()?.let { LocalDate.parse(it) }
        val effFrom = if (firstTracked != null && firstTracked.isAfter(from)) firstTracked else from
        val effTo = if (to.isAfter(today)) today else to
        val days = (ChronoUnit.DAYS.between(effFrom, effTo) + 1).coerceAtLeast(1)
        val length = ChronoUnit.DAYS.between(from, to) + 1
        val prevTo = from.minusDays(1)
        val prevFrom = prevTo.minusDays(length - 1)
        val prevTotal = db.totalsByDay(prevFrom.toString(), prevTo.toString()).values.sum()

        val apps = db.appTotals(f, t).map { (pkg, ms, opens) -> AppUse(pkg, ms, opens, Categorizer.app(context, pkg)) }
        val sites = db.webTotals(f, t).map { (d, ms, v) -> SiteUse(d, ms, v, Categorizer.site(context, d)) }
        val (prod, dist) = balance(apps, sites)

        // heatmap: weekday × hour, average minutes per day
        val heat = Array(7) { FloatArray(24) }
        val dayCount = IntArray(7)
        var d = effFrom
        while (!d.isAfter(effTo)) { dayCount[d.dayOfWeek.value - 1]++; d = d.plusDays(1) }
        hourRows.forEach { (day, h, ms) ->
            val wd = LocalDate.parse(day).dayOfWeek.value - 1
            heat[wd][h] += ms / 60_000f
        }
        for (w in 0 until 7) if (dayCount[w] > 0) for (h in 0 until 24) heat[w][h] /= dayCount[w]
        val weekday = LongArray(7)
        perDay.forEach { (day, ms) -> weekday[LocalDate.parse(day).dayOfWeek.value - 1] += ms }
        val weekdayAvg = (0 until 7).map { if (dayCount[it] > 0) weekday[it] / dayCount[it] else 0L }
        val longest = perDay.maxByOrNull { it.value }

        return PeriodModel(
            range = range, from = from, to = to, bars = bars, barLabels = labels, currentIndex = current,
            total = total, avg = total / days, prevTotal = prevTotal, goalMs = goalMs, apps = apps, sites = sites,
            cats = categories(apps, sites), heat = heat.map { it.toList() }, hourly = hourly.toList(),
            weekdayAvg = weekdayAvg, pickups = db.unlocks(f, t), productiveMs = prod, distractingMs = dist,
            days = days.toInt(), longestDay = longest?.key?.let { LocalDate.parse(it) }, longestMs = longest?.value ?: 0L,
        )
    }

    // ------------------------------------------------------------------ detail
    fun detail(context: Context, kind: TargetKind, id: String, locale: Locale): DetailModel {
        val db = Db.get(context)
        val today = LocalDate.now()
        val from = today.minusDays(29)
        val daily: Map<String, Pair<Long, Int>> =
            if (kind == TargetKind.APP) db.appDaily(id, from.toString(), today.toString())
            else db.webDaily(id, from.toString(), today.toString())
        val days = (0 until 30).map { from.plusDays(it.toLong()) }
        val series = days.map { daily[it.toString()]?.first ?: 0L }
        val labels = days.map { if (it.dayOfMonth == 1 || it.dayOfMonth % 7 == 0) "${it.dayOfMonth}" else "" }
        val hourly = LongArray(24)
        if (kind == TargetKind.APP) db.hourly(from.toString(), today.toString(), id).forEach { (_, h, ms) -> hourly[h] += ms }
        val activeDays = daily.count { it.value.first > 0 }.coerceAtLeast(1)
        val total = series.sum()
        val week = series.takeLast(7).sum()
        val prevWeek = series.dropLast(7).takeLast(7).sum()
        val limit = db.limits().firstOrNull { it.kind == kind && it.target == id }
        val cat = if (kind == TargetKind.APP) Categorizer.app(context, id) else Categorizer.site(context, id)
        return DetailModel(
            kind = kind, id = id, cat = cat, today = series.last(), avgDaily = total / activeDays, total30 = total,
            opensToday = daily[today.toString()]?.second ?: 0, opens30 = daily.values.sumOf { it.second },
            daily = series, dayLabels = labels, hourly = hourly.toList(), limit = limit,
            pause = kind == TargetKind.APP && id in db.pauseApps(), weekTotal = week, prevWeekTotal = prevWeek,
        )
    }

    // ------------------------------------------------------------------ growth
    fun growth(context: Context, locale: Locale): GrowthModel {
        val db = Db.get(context)
        val prefs = Prefs.get(context)
        val goalMs = prefs.dailyGoalMin * MIN
        val today = LocalDate.now()
        val weekFrom = today.minusDays(6)
        val allActs = db.activities("2000-01-01", today.toString())
        val focus = db.focusSessions(0, Long.MAX_VALUE)
        fun focusMin(s: FocusSession): Int {
            val end = if (s.end > 0) s.end else min(System.currentTimeMillis(), s.start + s.plannedMin * MIN)
            return ((end - s.start) / MIN).toInt().coerceAtLeast(0)
        }

        val goals = db.goals().map { g ->
            val actMin = allActs.filter { it.goalId == g.id }.sumOf { it.minutes }
            val focMin = focus.filter { it.goalId == g.id }.sumOf { focusMin(it) }
            val weekAct = allActs.filter { it.goalId == g.id && it.day >= weekFrom.toString() }.sumOf { it.minutes }
            val weekFoc = focus.filter { it.goalId == g.id && it.start >= startMs(weekFrom) }.sumOf { focusMin(it) }
            GoalProgress(g, (actMin + focMin) / 60.0, (weekAct + weekFoc) / 60.0)
        }

        val appsByDay = db.appByDay(weekFrom.toString(), today.toString())
        val webByDay = db.webByDay(weekFrom.toString(), today.toString())
        val days = (0 until 7).map { weekFrom.plusDays(it.toLong()) }
        val weekProd = ArrayList<Long>()
        val weekDist = ArrayList<Long>()
        for (d in days) {
            val k = d.toString()
            val apps = (appsByDay[k] ?: emptyMap()).map { AppUse(it.key, it.value, 0, Categorizer.app(context, it.key)) }
            val sites = (webByDay[k] ?: emptyMap()).map { SiteUse(it.key, it.value, 0, Categorizer.site(context, it.key)) }
            val (p, dd) = balance(apps, sites)
            val act = allActs.filter { it.day == k }.sumOf { it.minutes } * MIN
            val foc = focus.filter { it.start >= startMs(d) && it.start < startMs(d.plusDays(1)) }.sumOf { focusMin(it) } * MIN
            weekProd.add(p + act + foc)
            weekDist.add(dd)
        }

        val yearFrom = today.withDayOfYear(1)
        val yApps = db.appTotals(yearFrom.toString(), today.toString()).map { AppUse(it.first, it.second, 0, Categorizer.app(context, it.first)) }
        val ySites = db.webTotals(yearFrom.toString(), today.toString()).map { SiteUse(it.first, it.second, 0, Categorizer.site(context, it.first)) }
        val yearDist = balance(yApps, ySites).second
        val mFrom = today.minusDays(29)
        val mApps = db.appTotals(mFrom.toString(), today.toString()).map { AppUse(it.first, it.second, 0, Categorizer.app(context, it.first)) }
        val mSites = db.webTotals(mFrom.toString(), today.toString()).map { SiteUse(it.first, it.second, 0, Categorizer.site(context, it.first)) }
        val firstDay = db.firstDay()?.let { LocalDate.parse(it) } ?: today
        val trackedDays = (ChronoUnit.DAYS.between(if (firstDay.isAfter(mFrom)) firstDay else mFrom, today) + 1).coerceAtLeast(1)
        val projected = balance(mApps, mSites).second / trackedDays * 365

        val streak = streak(context, goalMs)
        val best = max(bestStreak(context, goalMs), streak)
        val completedFocus = focus.count { it.completed }
        val productiveHours = (allActs.sumOf { it.minutes } + focus.sumOf { focusMin(it) }) / 60.0
        val recentDays = (0 until 30).map { today.minusDays(it.toLong()).toString() }
        val earlyBird = recentDays.any { day ->
            db.firstUse(day)?.let { Instant.ofEpochMilli(it).atZone(zone()).hour >= 8 } == true
        }
        val nightRows = db.hourly(today.minusDays(29).toString(), today.minusDays(1).toString())
        val nightUse = nightRows.filter { it.second >= 23 || it.second < 6 }.groupBy { it.first }.mapValues { e -> e.value.sumOf { it.third } }
        val trackedPast = db.totalsByDay(today.minusDays(29).toString(), today.minusDays(1).toString()).keys
        val nightOff = trackedPast.any { (nightUse[it] ?: 0L) < 2 * MIN }

        fun ach(id: String, cur: Double, target: Double) = Achievement(id, cur >= target, (cur / target).toFloat().coerceIn(0f, 1f))
        val achievements = listOf(
            ach("first_focus", completedFocus.toDouble(), 1.0),
            ach("under_goal", best.toDouble(), 1.0),
            ach("streak_3", best.toDouble(), 3.0),
            ach("streak_7", best.toDouble(), 7.0),
            ach("focus_10", completedFocus.toDouble(), 10.0),
            ach("productive_10h", productiveHours, 10.0),
            ach("early_bird", if (earlyBird) 1.0 else 0.0, 1.0),
            ach("night_off", if (nightOff) 1.0 else 0.0, 1.0),
            ach("goal_set", goals.size.toDouble(), 1.0),
            ach("limit_set", db.limits().size.toDouble(), 1.0),
            ach("streak_30", best.toDouble(), 30.0),
            ach("productive_100h", productiveHours, 100.0),
        )

        return GrowthModel(
            streak = streak, bestStreak = best, goals = goals,
            recent = allActs.take(8), weekProductive = weekProd, weekDistracting = weekDist,
            weekLabels = days.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) },
            yearDistractingMs = yearDist, projectedYearMs = projected,
            productiveWeekMs = weekProd.sum(), focusCount = completedFocus, achievements = achievements,
        )
    }

    /** Average daily screen time over the last 7 tracked days (onboarding "reality check"). */
    fun recentDailyAverage(context: Context): Long {
        val db = Db.get(context)
        val today = LocalDate.now()
        val m = db.totalsByDay(today.minusDays(7).toString(), today.minusDays(1).toString()).filterValues { it > 0 }
        if (m.isNotEmpty()) return m.values.sum() / m.size
        return db.totalsByDay(today.toString(), today.toString()).values.sum()
    }

    fun recentDistractingShare(context: Context): Double {
        val db = Db.get(context)
        val today = LocalDate.now()
        val from = today.minusDays(7).toString()
        val apps = db.appTotals(from, today.toString()).map { AppUse(it.first, it.second, 0, Categorizer.app(context, it.first)) }
        val total = apps.sumOf { it.ms }
        if (total <= 0) return 0.0
        return balance(apps, emptyList()).second.toDouble() / total
    }
}
