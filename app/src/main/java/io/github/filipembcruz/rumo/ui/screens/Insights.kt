@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.PeriodModel
import io.github.filipembcruz.rumo.data.Range
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.BottomSpacer
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LifeGoal
import io.github.filipembcruz.rumo.ui.LocalLocale
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.Banner
import io.github.filipembcruz.rumo.ui.components.BarChart
import io.github.filipembcruz.rumo.ui.components.CatDot
import io.github.filipembcruz.rumo.ui.components.Donut
import io.github.filipembcruz.rumo.ui.components.EmptyNote
import io.github.filipembcruz.rumo.ui.components.Heatmap
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Pill
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.Segmented
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.components.color
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle

@Composable
fun InsightsRoute() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val locale = LocalLocale.current
    var range by rememberSaveable { mutableStateOf(Range.WEEK) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var cFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var cTo by rememberSaveable { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val model = rememberModel(nav.tick, range, anchor, cFrom, cTo, locale) {
        Repo.period(context, range, LocalDate.parse(anchor), locale, cFrom?.let(LocalDate::parse), cTo?.let(LocalDate::parse))
    }
    val lifeGoals = remember { Prefs.get(context).lifeGoals.mapNotNull { LifeGoal.of(it) } }

    if (showPicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(enabled = state.selectedStartDateMillis != null, onClick = {
                    val s0 = state.selectedStartDateMillis ?: return@TextButton
                    val e0 = state.selectedEndDateMillis ?: s0
                    cFrom = Instant.ofEpochMilli(s0).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    cTo = Instant.ofEpochMilli(e0).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    range = Range.CUSTOM
                    showPicker = false
                }) { Text(LocalStrings.current.apply) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(LocalStrings.current.cancel) } },
        ) { DateRangePicker(state = state, modifier = Modifier.weight(1f)) }
    }

    InsightsScreen(
        model = model, range = range,
        lifeGoals = lifeGoals.ifEmpty { listOf(LifeGoal.LANGUAGE, LifeGoal.BOOKS, LifeGoal.FITNESS) },
        sitesEnabled = remember(nav.tick) { Perms.accessibility(context) },
        onRange = { range = it; anchor = LocalDate.now().toString() },
        onShift = { dir ->
            val a = LocalDate.parse(anchor)
            anchor = when (range) {
                Range.DAY -> a.plusDays(dir.toLong())
                Range.WEEK -> a.plusWeeks(dir.toLong())
                Range.MONTH -> a.plusMonths(dir.toLong())
                Range.YEAR -> a.plusYears(dir.toLong())
                Range.CUSTOM -> a
            }.let { if (it.isAfter(LocalDate.now())) LocalDate.now() else it }.toString()
        },
        onCalendar = { showPicker = true },
        onApp = { nav.openDetail(TargetKind.APP, it) },
        onSite = { nav.openDetail(TargetKind.SITE, it) },
        onEnableSites = { Perms.openAccessibilitySettings(context) },
    )
}

@Composable
fun InsightsScreen(
    model: PeriodModel?, range: Range, lifeGoals: List<LifeGoal>, sitesEnabled: Boolean,
    onRange: (Range) -> Unit, onShift: (Int) -> Unit, onCalendar: () -> Unit,
    onApp: (String) -> Unit, onSite: (String) -> Unit, onEnableSites: () -> Unit,
) {
    val s = LocalStrings.current
    val c = R2.colors
    val locale = LocalLocale.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ranges = listOf(Range.DAY, Range.WEEK, Range.MONTH, Range.YEAR)
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(s.insights, style = MaterialTheme.typography.headlineLarge, color = c.text,
                modifier = Modifier.statusBarsPadding().padding(top = 16.dp))
        }
        item {
            Segmented(listOf(s.day, s.week, s.month, s.year), ranges.indexOf(range).coerceAtLeast(0) .let { if (range == Range.CUSTOM) -1 else it }.coerceAtLeast(0),
                { onRange(ranges[it]) })
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, enabled = range != Range.CUSTOM) { onShift(-1) }
                Text(
                    model?.let { if (it.range == Range.MONTH) Fmt.month(it.from, locale) else if (it.range == Range.YEAR) "${it.from.year}" else Fmt.range(it.from, it.to, locale) } ?: "",
                    style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                val canNext = model != null && model.to.isBefore(LocalDate.now()) && range != Range.CUSTOM
                RoundIcon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, enabled = canNext) { onShift(1) }
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.CalendarMonth, enabled = true, tint = if (range == Range.CUSTOM) c.primary else c.text) { onCalendar() }
            }
        }
        if (model == null) return@LazyColumn
        item { SummaryCard(model) }
        item { Segmented(listOf(s.apps, s.sites, s.categories, s.patterns), tab, { tab = it }) }
        item {
            AnimatedContent(tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "itab") { t ->
                when (t) {
                    0 -> RCard {
                        if (model.apps.isEmpty()) EmptyNote(Icons.Rounded.Public, s.noDataTitle, s.noDataBody)
                        val max = model.apps.firstOrNull()?.ms ?: 1L
                        model.apps.take(40).forEach { AppUseRow(it, max, model.total) { onApp(it.pkg) } }
                    }
                    1 -> RCard {
                        if (!sitesEnabled) Banner(Icons.Rounded.Public, s.enableSitesBanner, s.enable, c.primarySoft, c.primary, onEnableSites)
                        else if (model.sites.isEmpty()) EmptyNote(Icons.Rounded.Public, s.noSitesTitle, s.noSitesBody)
                        if (!sitesEnabled && model.sites.isNotEmpty()) Spacer(Modifier.height(10.dp))
                        val max = model.sites.firstOrNull()?.ms ?: 1L
                        model.sites.take(40).forEach { SiteUseRow(it, max) { onSite(it.domain) } }
                    }
                    2 -> CategoriesCard(model)
                    else -> PatternsCard(model)
                }
            }
        }
        item { EquivalenceCard(model.distractingMs, model.days, lifeGoals) }
        item { BottomSpacer() }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, tint: Color = R2.colors.text, onClick: () -> Unit) {
    val c = R2.colors
    Box(
        Modifier.size(40.dp).bounceClick(enabled = enabled, onClick = onClick).clip(CircleShape).background(c.surface),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) tint else c.muted.copy(alpha = 0.4f)) }
}

@Composable
private fun SummaryCard(m: PeriodModel) {
    val s = LocalStrings.current
    val c = R2.colors
    val change = Fmt.pct(m.total, m.prevTotal)
    RCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(if (m.range == Range.DAY) s.screenTime else s.totalScreenTime, style = MaterialTheme.typography.labelLarge, color = c.muted)
                AnimatedDuration(m.total, c.text)
            }
            if (change != null) {
                val up = change > 0
                Pill(Fmt.signedPct(change), if (up) c.coralSoft else c.limeSoft, if (up) c.coral else Color(0xFF4F7A0E),
                    icon = if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (m.range != Range.DAY) MiniStat(s.dailyAvg, Fmt.dur(m.avg))
            MiniStat(s.pickups, "${m.pickups}")
            MiniStat(s.productive, Fmt.dur(m.productiveMs))
        }
        Spacer(Modifier.height(14.dp))
        BarChart(
            m.bars, m.barLabels, highlight = m.currentIndex, height = 200.dp,
            goal = if (m.range == Range.WEEK || m.range == Range.MONTH || (m.range == Range.CUSTOM && m.bars.size == m.days)) m.goalMs else null,
        )
        if (m.range != Range.DAY && m.range != Range.YEAR) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 14.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(c.lime))
                Spacer(Modifier.width(6.dp))
                Text(s.dailyGoalLine, style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

@Composable
fun MiniStat(label: String, value: String) {
    Column(Modifier.padding(top = 10.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = R2.colors.text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = R2.colors.muted)
    }
}

@Composable
private fun CategoriesCard(m: PeriodModel) {
    val s = LocalStrings.current
    val c = R2.colors
    RCard {
        if (m.cats.isEmpty()) {
            EmptyNote(Icons.Rounded.Public, s.noDataTitle, s.noDataBody)
            return@RCard
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(m.cats.map { it.ms.toFloat() to it.cat.color() }, size = 190.dp, thickness = 22.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.dur(m.total), style = MaterialTheme.typography.headlineSmall, color = c.text)
                    Text(s.total, style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        val total = m.cats.sumOf { it.ms }.coerceAtLeast(1)
        m.cats.forEach { cu ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                CatDot(cu.cat, 12.dp)
                Spacer(Modifier.width(10.dp))
                Text(s.cat(cu.cat), style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                Text(Fmt.dur(cu.ms), style = MaterialTheme.typography.labelLarge, color = c.text)
                Text("  ${cu.ms * 100 / total}%", style = MaterialTheme.typography.labelMedium, color = c.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        val bal = (m.productiveMs + m.distractingMs).coerceAtLeast(1)
        Text(s.balance, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        io.github.filipembcruz.rumo.ui.components.StackedBar(listOf(m.productiveMs.toFloat() to c.mint, m.distractingMs.toFloat() to c.coral))
        Spacer(Modifier.height(6.dp))
        Row {
            Text("${s.productive} ${m.productiveMs * 100 / bal}%", style = MaterialTheme.typography.labelMedium, color = c.mint, modifier = Modifier.weight(1f))
            Text("${s.distracting} ${m.distractingMs * 100 / bal}%", style = MaterialTheme.typography.labelMedium, color = c.coral)
        }
    }
}

@Composable
private fun PatternsCard(m: PeriodModel) {
    val s = LocalStrings.current
    val c = R2.colors
    val locale = LocalLocale.current
    val days = DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, locale).take(3) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RCard {
            Text(s.whenYouUse, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(s.heatmapHint, style = MaterialTheme.typography.bodySmall, color = c.muted)
            Spacer(Modifier.height(12.dp))
            Heatmap(m.heat, days)
        }
        RCard {
            Text(s.byHour, style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            BarChart(m.hourly.map { it / m.days.coerceAtLeast(1) }, (0 until 24).map { if (it % 6 == 0) "%02d".format(it) else "" },
                highlight = peakHour(m.hourly), height = 160.dp, color = c.sky)
        }
        if (m.range != Range.DAY) RCard {
            Text(s.byWeekday, style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            val best = m.weekdayAvg.indices.maxByOrNull { m.weekdayAvg[it] } ?: -1
            BarChart(m.weekdayAvg, DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) }, highlight = best, height = 160.dp, color = c.mint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FactTile(s.peakHourTitle, "%02d:00".format(peakHour(m.hourly)), Modifier.weight(1f))
            FactTile(s.pickupsPerDay, "${m.pickups / m.days.coerceAtLeast(1)}", Modifier.weight(1f))
        }
        if (m.longestDay != null && m.range != Range.DAY) {
            FactTile(s.longestDay, "${Fmt.day(m.longestDay, locale)} · ${Fmt.dur(m.longestMs)}", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FactTile(label: String, value: String, modifier: Modifier) {
    val c = R2.colors
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(c.surface).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.text)
    }
}

@Composable
fun EquivalenceCard(distractingMs: Long, days: Int, goals: List<LifeGoal>) {
    val s = LocalStrings.current
    val c = R2.colors
    val hours = Fmt.hours(distractingMs)
    val perYear = if (days > 0) hours / days * 365 else 0.0
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.navy).padding(20.dp)) {
        Column {
            Text(s.couldHaveBeen, style = MaterialTheme.typography.titleLarge, color = c.onNavy)
            Text(s.couldHaveBeenSub(Fmt.hoursText(hours), Fmt.hoursText(perYear)), style = MaterialTheme.typography.bodyMedium,
                color = c.onNavy.copy(alpha = 0.7f))
            Spacer(Modifier.height(14.dp))
            goals.take(4).forEach { g ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(g.icon, g.color.copy(alpha = 0.22f), if (c.dark) g.color else Color.White, 40.dp, 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.goalName(g), style = MaterialTheme.typography.titleSmall, color = c.onNavy)
                        Text(s.inAYear(s.goalAmount(g, perYear)), style = MaterialTheme.typography.labelSmall, color = c.onNavy.copy(alpha = 0.6f))
                    }
                    Text(s.goalAmount(g, hours), style = MaterialTheme.typography.titleMedium, color = c.lime, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(s.estimatesNote, style = MaterialTheme.typography.labelSmall, color = c.onNavy.copy(alpha = 0.5f))
        }
    }
}
