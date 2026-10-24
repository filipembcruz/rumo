@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.AppUse
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Insight
import io.github.filipembcruz.rumo.data.InsightKind
import io.github.filipembcruz.rumo.data.Limit
import io.github.filipembcruz.rumo.data.LimitStatus
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.SiteUse
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.data.TodayModel
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.BottomSpacer
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.Strings
import io.github.filipembcruz.rumo.ui.Tab
import io.github.filipembcruz.rumo.ui.components.AppIcon
import io.github.filipembcruz.rumo.ui.components.Banner
import io.github.filipembcruz.rumo.ui.components.BarChart
import io.github.filipembcruz.rumo.ui.components.CatDot
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Mood
import io.github.filipembcruz.rumo.ui.components.MoodOrb
import io.github.filipembcruz.rumo.ui.components.Pill
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.ProgressLine
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.Ring
import io.github.filipembcruz.rumo.ui.components.SectionHeader
import io.github.filipembcruz.rumo.ui.components.SiteAvatar
import io.github.filipembcruz.rumo.ui.components.StackedBar
import io.github.filipembcruz.rumo.ui.components.StatTile
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.components.color
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

class HomeActions(
    val onSettings: () -> Unit = {},
    val onApp: (String) -> Unit = {},
    val onSite: (String) -> Unit = {},
    val onSeeAll: () -> Unit = {},
    val onFocus: () -> Unit = {},
    val onLog: () -> Unit = {},
    val onAddLimit: () -> Unit = {},
    val onEnableSites: () -> Unit = {},
    val onLimit: (Limit) -> Unit = {},
)

@Composable
fun HomeRoute() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val model = rememberModel(nav.tick) { Repo.today(context) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var limitTarget by remember { mutableStateOf<Pair<TargetKind, String>?>(null) }
    val prefs = remember { Prefs.get(context) }

    val actions = HomeActions(
        onSettings = nav.openSettings,
        onApp = { nav.openDetail(TargetKind.APP, it) },
        onSite = { nav.openDetail(TargetKind.SITE, it) },
        onSeeAll = { nav.goTab(Tab.INSIGHTS) },
        onFocus = { nav.goTab(Tab.FOCUS) },
        onLog = { sheet = "log" },
        onAddLimit = { sheet = "pick" },
        onEnableSites = nav.setupBlocking,
        onLimit = { limitTarget = it.kind to it.target },
    )
    if (model == null) { LoadingScreen(); return }
    HomeScreen(model, prefs.name, actions)

    when (sheet) {
        "log" -> {
            val goals = rememberModel(nav.tick) { Db.get(context).goals() } ?: emptyList()
            LogActivitySheet(goals, onDismiss = { sheet = null }) { goalId, label, minutes ->
                sheet = null
                scope.launch { withContext(Dispatchers.IO) { Actions.logActivity(context, goalId, label, minutes) }; nav.refresh() }
            }
        }
        "pick" -> TargetPickerSheet(onDismiss = { sheet = null }) { kind, id -> sheet = null; limitTarget = kind to id }
    }
    limitTarget?.let { (kind, id) ->
        LimitSheetRoute(kind, id, onDone = { limitTarget = null; nav.refresh() })
    }
}

@Composable
fun HomeScreen(m: TodayModel, name: String, a: HomeActions) {
    val s = LocalStrings.current
    val c = R2.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HomeHeader(name, a.onSettings) }
        item { Appear(0) { HeroCard(m) } }
        item { Appear(1) { InsightCard(m.insight, m, a) } }
        item {
            Appear(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.Bolt, s.focusScore, "${m.focusScore}", c.primary, c.primarySoft, Modifier.weight(1f), sub = s.scoreLabel(m.focusScore))
                    StatTile(Icons.Rounded.TouchApp, s.pickups, "${m.pickups}", c.amber, c.amber.copy(alpha = 0.16f), Modifier.weight(1f),
                        sub = m.firstUse?.let { s.firstAt(Fmt.clock(it)) })
                    StatTile(Icons.Rounded.WbSunny, s.productive, Fmt.dur(m.productiveMs), c.mint, c.mint.copy(alpha = 0.16f), Modifier.weight(1f),
                        sub = s.distractingShort(Fmt.dur(m.distractingMs)), subColor = c.coral)
                }
            }
        }
        item {
            Appear(3) {
                RCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.todayByHour, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                        Pill(s.peakAt(peakHour(m.hourly)), c.primarySoft, c.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    BarChart(m.hourly, (0 until 24).map { if (it % 6 == 0) "%02d".format(it) else "" }, highlight = LocalTime.now().hour, height = 170.dp)
                }
            }
        }
        if (m.cats.isNotEmpty()) item {
            Appear(4) {
                RCard {
                    Text(s.whereTimeWent, style = MaterialTheme.typography.titleMedium, color = c.text)
                    Spacer(Modifier.height(14.dp))
                    StackedBar(m.cats.map { it.ms.toFloat() to it.cat.color() })
                    Spacer(Modifier.height(14.dp))
                    m.cats.take(5).forEach { cu ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            CatDot(cu.cat)
                            Spacer(Modifier.width(10.dp))
                            Text(s.cat(cu.cat), style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                            Text(Fmt.dur(cu.ms), style = MaterialTheme.typography.labelLarge, color = c.text)
                            Spacer(Modifier.width(8.dp))
                            Text("${(cu.ms * 100 / m.total.coerceAtLeast(1)).toInt()}%", style = MaterialTheme.typography.labelMedium, color = c.muted,
                                modifier = Modifier.width(36.dp))
                        }
                    }
                }
            }
        }
        if (m.limits.isNotEmpty()) item {
            RCard {
                SectionHeader(s.limitsToday)
                m.limits.forEach { LimitRow(it) { a.onLimit(it.limit) } }
            }
        }
        item {
            Appear(5) {
                RCard {
                    SectionHeader(s.topApps, s.seeAll, a.onSeeAll)
                    if (m.apps.isEmpty()) Text(s.noAppsYet, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    val max = m.apps.firstOrNull()?.ms ?: 1L
                    m.apps.take(5).forEach { AppUseRow(it, max, m.total) { a.onApp(it.pkg) } }
                }
            }
        }
        item {
            RCard {
                SectionHeader(s.topSites, if (m.sites.isNotEmpty()) s.seeAll else null, a.onSeeAll)
                if (!m.sitesEnabled) {
                    Banner(Icons.Rounded.Public, s.enableSitesBanner, s.enable, c.primarySoft, c.primary, a.onEnableSites)
                } else if (m.sites.isEmpty()) {
                    Text(s.noSitesYet, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                }
                val max = m.sites.firstOrNull()?.ms ?: 1L
                m.sites.take(5).forEach { SiteUseRow(it, max) { a.onSite(it.domain) } }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Rounded.HourglassTop, s.startFocus, c.primary, Modifier.weight(1f), a.onFocus)
                QuickAction(Icons.Rounded.EditNote, s.logActivity, c.mint, Modifier.weight(1f), a.onLog)
                QuickAction(Icons.Rounded.Add, s.addLimit, c.coral, Modifier.weight(1f), a.onAddLimit)
            }
        }
        item { BottomSpacer() }
    }
}

fun peakHour(hourly: List<Long>): Int = hourly.indices.maxByOrNull { hourly[it] } ?: 0

@Composable
private fun HomeHeader(name: String, onSettings: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val hour = LocalTime.now().hour
    val hello = when (hour) { in 5..11 -> s.goodMorning; in 12..17 -> s.goodAfternoon; else -> s.goodEvening }
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (name.isBlank()) hello else "$hello,", style = MaterialTheme.typography.titleMedium, color = c.muted)
            Text(if (name.isBlank()) s.yourDay else name, style = MaterialTheme.typography.headlineLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(48.dp).bounceClick(onClick = onSettings).clip(CircleShape).background(c.surface),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Settings, s.settings, tint = c.text) }
    }
}

@Composable
fun HeroCard(m: TodayModel) {
    val s = LocalStrings.current
    val c = R2.colors
    val frac = if (m.goalMs > 0) m.total.toFloat() / m.goalMs else 0f
    val change = Fmt.pct(m.total, m.yesterday)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFF4A2FD9), Color(0xFF2E1C9E))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x66C4F25C), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.05f), size.width * 0.5f),
                size.width * 0.5f, Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(Brush.radialGradient(listOf(Color(0x40FFFFFF), Color.Transparent), Offset(0f, size.height), size.width * 0.45f),
                size.width * 0.45f, Offset(0f, size.height))
        }
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(frac, size = 132.dp, thickness = 14.dp, colors = listOf(c.lime, Color(0xFF8DF0C7))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(frac * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(s.ofGoal, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.screenTimeToday, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    AnimatedDuration(m.total, Color.White)
                    Spacer(Modifier.height(4.dp))
                    val left = m.goalMs - m.total
                    Text(
                        if (left >= 0) s.leftOfGoal(Fmt.dur(left)) else s.overGoal(Fmt.dur(-left)),
                        style = MaterialTheme.typography.bodyMedium, color = if (left >= 0) c.lime else Color(0xFFFFB3B3),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (change != null) {
                    Pill(s.vsYesterday(Fmt.signedPct(change)), Color.White.copy(alpha = 0.16f), Color.White,
                        icon = if (change > 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown)
                }
                if (m.avg7 > 0) Pill(s.avg7(Fmt.dur(m.avg7)), Color.White.copy(alpha = 0.16f), Color.White)
                if (m.streak > 0) Pill(s.streakShort(m.streak), c.lime, c.onLime, icon = Icons.Rounded.LocalFireDepartment)
            }
        }
    }
}

@Composable
fun AnimatedDuration(ms: Long, color: Color) {
    val anim = remember { androidx.compose.animation.core.Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else ms.toFloat()) }
    androidx.compose.runtime.LaunchedEffect(ms) { anim.animateTo(ms.toFloat(), tween(1100)) }
    val parts = Fmt.durParts(anim.value.toLong())
    Row(verticalAlignment = Alignment.Bottom) {
        parts.forEach { (v, u) ->
            Text(v, style = MaterialTheme.typography.displayMedium, color = color)
            Text(u, style = MaterialTheme.typography.headlineSmall, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(start = 2.dp, end = 8.dp, bottom = 6.dp))
        }
    }
}

data class InsightCopy(val mood: Mood, val title: String, val body: String, val action: String?)

fun insightCopy(i: Insight, s: Strings, label: (String) -> String): InsightCopy = when (i.kind) {
    InsightKind.NO_DATA -> InsightCopy(Mood.CALM, s.inNoDataT, s.inNoDataB, null)
    InsightKind.MORNING_FRESH -> InsightCopy(Mood.HAPPY, s.inMorningT, s.inMorningB, s.startFocus)
    InsightKind.UNDER_GOAL -> InsightCopy(Mood.HAPPY, s.inUnderT, s.inUnderB(Fmt.dur(i.ms)), null)
    InsightKind.NEAR_GOAL -> InsightCopy(Mood.WORRIED, s.inNearT, s.inNearB(Fmt.dur(i.ms)), s.startFocus)
    InsightKind.OVER_GOAL -> InsightCopy(Mood.WORRIED, s.inOverT, s.inOverB(Fmt.dur(i.ms)), s.startFocus)
    InsightKind.TOP_DISTRACTING -> InsightCopy(Mood.WORRIED, s.inTopT(label(i.subject)), s.inTopB(Fmt.dur(i.ms)), s.addLimit)
    InsightKind.FOCUS_RUNNING -> InsightCopy(Mood.FOCUS, s.inFocusT, s.inFocusB(Fmt.dur(i.ms)), null)
    InsightKind.GREAT_BALANCE -> InsightCopy(Mood.CELEBRATE, s.inBalanceT, s.inBalanceB(Fmt.dur(i.ms)), s.logActivity)
    InsightKind.LATE_NIGHT -> InsightCopy(Mood.SLEEPY, s.inLateT, s.inLateB, null)
    InsightKind.STREAK -> InsightCopy(Mood.CELEBRATE, s.inStreakT(i.n), s.inStreakB, null)
    InsightKind.SITES_OFF -> InsightCopy(Mood.CALM, s.inSitesT, s.inSitesB(Fmt.dur(i.ms)), s.enable)
}

@Composable
private fun InsightCard(i: Insight, m: TodayModel, a: HomeActions) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val copy = insightCopy(i, s) { if (it.contains('.') && !it.contains('/')) AppNames.label(context, it) else it }
    RCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoodOrb(copy.mood, size = 58.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(copy.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                Spacer(Modifier.height(2.dp))
                Text(copy.body, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
        }
        if (copy.action != null) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton(copy.action, Modifier.fillMaxWidth().height(48.dp)) {
                when (i.kind) {
                    InsightKind.TOP_DISTRACTING -> a.onAddLimit()
                    InsightKind.SITES_OFF -> a.onEnableSites()
                    InsightKind.GREAT_BALANCE -> a.onLog()
                    else -> a.onFocus()
                }
            }
        }
    }
}

@Composable
fun AppUseRow(app: AppUse, max: Long, total: Long, onClick: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val label = remember(app.pkg) { AppNames.label(context, app.pkg) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).bounceClick(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.pkg, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Fmt.dur(app.ms), style = MaterialTheme.typography.titleSmall, color = c.text, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            ProgressLine(app.ms.toFloat() / max.coerceAtLeast(1), app.cat.color(), height = 6.dp)
            Spacer(Modifier.height(4.dp))
            Row {
                Text(s.cat(app.cat), style = MaterialTheme.typography.labelSmall, color = app.cat.color())
                if (app.opens > 0) Text(" · ${s.opens(app.opens)}", style = MaterialTheme.typography.labelSmall, color = c.muted)
                if (total > 0) Text(" · ${(app.ms * 100 / total).toInt()}%", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

@Composable
fun SiteUseRow(site: SiteUse, max: Long, onClick: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).bounceClick(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteAvatar(site.domain, site.cat, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(site.domain.removePrefix("www."), style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Fmt.dur(site.ms), style = MaterialTheme.typography.titleSmall, color = c.text, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            ProgressLine(site.ms.toFloat() / max.coerceAtLeast(1), site.cat.color(), height = 6.dp)
            Spacer(Modifier.height(4.dp))
            Row {
                Text(s.cat(site.cat), style = MaterialTheme.typography.labelSmall, color = site.cat.color())
                if (site.visits > 0) Text(" · ${s.visits(site.visits)}", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

@Composable
fun LimitRow(st: LimitStatus, onClick: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val l = st.limit
    val name = when (l.kind) {
        TargetKind.APP -> remember(l.target) { AppNames.label(context, l.target) }
        TargetKind.SITE -> l.target
        TargetKind.CATEGORY -> s.cat(io.github.filipembcruz.rumo.data.Cat.of(l.target))
    }
    val f = st.fraction
    val col = when { f >= 1f -> c.coral; f >= 0.8f -> c.amber; else -> c.mint }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).bounceClick(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        when (l.kind) {
            TargetKind.APP -> AppIcon(l.target, 38.dp)
            TargetKind.SITE -> SiteAvatar(l.target, io.github.filipembcruz.rumo.data.Cat.OTHER, 38.dp)
            TargetKind.CATEGORY -> IconBadge(Icons.Rounded.HourglassTop, io.github.filipembcruz.rumo.data.Cat.of(l.target).color().copy(alpha = 0.16f),
                io.github.filipembcruz.rumo.data.Cat.of(l.target).color(), 38.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(name, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Fmt.dur(st.usedMs)} / ${Fmt.dur(l.minutes * 60_000L)}", style = MaterialTheme.typography.labelMedium, color = col)
            }
            Spacer(Modifier.height(6.dp))
            ProgressLine(f, col, height = 6.dp)
            Text(if (l.block) s.blocksWhenReached else s.alertsOnly, style = MaterialTheme.typography.labelSmall, color = c.muted,
                modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = R2.colors
    Column(
        modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(24.dp)).background(c.surface).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, color.copy(alpha = 0.15f), color, 42.dp, 22.dp)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Staggered entrance for cards. */
@Composable
fun Appear(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(!io.github.filipembcruz.rumo.ui.UiFlags.animations) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(40L * index)
        shown = true
    }
    AnimatedVisibility(shown, enter = fadeIn(tween(380)) + slideInVertically(tween(420)) { it / 6 }) { content() }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(R2.colors.bg), contentAlignment = Alignment.Center) {
        MoodOrb(Mood.CALM, size = 72.dp)
    }
}
