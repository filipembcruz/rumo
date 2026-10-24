@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.DetailModel
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LocalLocale
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.AppIcon
import io.github.filipembcruz.rumo.ui.components.AreaChart
import io.github.filipembcruz.rumo.ui.components.BarChart
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Pill
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.ProgressLine
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.SecondaryButton
import io.github.filipembcruz.rumo.ui.components.SiteAvatar
import io.github.filipembcruz.rumo.ui.components.ToggleRow
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.components.color
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DetailRoute(kind: TargetKind, id: String) {
    val context = LocalContext.current
    val nav = LocalNav.current
    val locale = LocalLocale.current
    val scope = rememberCoroutineScope()
    val model = rememberModel(nav.tick, kind, id) { Repo.detail(context, kind, id, locale) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val prefs = remember { Prefs.get(context) }
    var inFocus by remember { mutableStateOf(id in prefs.focusApps) }
    fun io(block: () -> Unit) = scope.launch { withContext(Dispatchers.IO) { block() }; nav.refresh() }

    if (model == null) { LoadingScreen(); return }
    DetailScreen(
        model,
        title = if (kind == TargetKind.APP) remember(id) { AppNames.label(context, id) } else id.removePrefix("www."),
        inFocus = inFocus,
        onBack = nav.back,
        onCategory = { sheet = "cat" },
        onLimit = { sheet = "limit" },
        onPause = { on -> io { Actions.setPause(context, id, on) } },
        onFocusToggle = { on -> inFocus = on; io { Actions.toggleFocusApp(context, id, on) } },
    )
    when (sheet) {
        "cat" -> CategorySheet(model.cat, onDismiss = { sheet = null }) { cat ->
            sheet = null; io { Actions.setCategory(context, kind, id, cat.id) }
        }
        "limit" -> LimitSheetRoute(kind, id, onDone = { sheet = null; nav.refresh() })
    }
}

@Composable
fun DetailScreen(
    m: DetailModel, title: String, inFocus: Boolean, onBack: () -> Unit, onCategory: () -> Unit, onLimit: () -> Unit,
    onPause: (Boolean) -> Unit, onFocusToggle: (Boolean) -> Unit,
) {
    val s = LocalStrings.current
    val c = R2.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).bounceClick(onClick = onBack).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.back, tint = c.text)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (m.kind == TargetKind.APP) AppIcon(m.id, 84.dp) else SiteAvatar(m.id, m.cat, 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.bounceClick(onClick = onCategory)) {
                    Pill(s.cat(m.cat), m.cat.color().copy(alpha = 0.15f), m.cat.color(), icon = Icons.Rounded.Edit)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigStat(s.today, Fmt.dur(m.today), Modifier.weight(1f))
                BigStat(s.dailyAvg30, Fmt.dur(m.avgDaily), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigStat(if (m.kind == TargetKind.APP) s.opensToday else s.visitsToday, "${m.opensToday}", Modifier.weight(1f))
                val avgSession = if (m.opens30 > 0) m.total30 / m.opens30 else 0L
                BigStat(s.avgSession, Fmt.dur(avgSession), Modifier.weight(1f))
            }
        }
        m.batteryPct?.let { pct ->
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Rounded.BatteryChargingFull, c.mint.copy(alpha = 0.15f), c.mint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.batteryThisCharge, style = MaterialTheme.typography.labelMedium, color = c.muted)
                        Text(s.batteryPct(pct), style = MaterialTheme.typography.headlineSmall, color = c.text)
                    }
                    Text(s.estimate, style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }
        item {
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.last30, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    val ch = Fmt.pct(m.weekTotal, m.prevWeekTotal)
                    if (ch != null) Pill(s.thisWeek(Fmt.signedPct(ch)), if (ch > 0) c.coralSoft else c.limeSoft, if (ch > 0) c.coral else androidx.compose.ui.graphics.Color(0xFF4F7A0E))
                }
                Spacer(Modifier.height(6.dp))
                AreaChart(m.daily, m.dayLabels, color = m.cat.color())
            }
        }
        if (m.hourly.any { it > 0 }) item {
            RCard {
                Text(s.whenYouUseIt, style = MaterialTheme.typography.titleMedium, color = c.text)
                Spacer(Modifier.height(6.dp))
                BarChart(m.hourly, (0 until 24).map { if (it % 6 == 0) "%02d".format(it) else "" }, highlight = peakHour(m.hourly),
                    height = 150.dp, color = m.cat.color())
            }
        }
        item {
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Timer, c.coral.copy(alpha = 0.14f), c.coral)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.dailyLimit, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(
                            m.limit?.let { (if (it.todayMinutes > 0) Fmt.dur(it.todayMs) else s.noLimitToday) + (if (it.days != null) " · ${s.variesByDay}" else "") + " · ${if (it.block) s.blocksWhenReached else s.alertsOnly}" } ?: s.noLimit,
                            style = MaterialTheme.typography.bodySmall, color = c.muted,
                        )
                    }
                }
                m.limit?.let {
                    Spacer(Modifier.height(12.dp))
                    val f = if (it.todayMinutes > 0) m.today.toFloat() / it.todayMs else 0f
                    ProgressLine(f, if (f >= 1f) c.coral else if (f >= 0.8f) c.amber else c.mint)
                }
                Spacer(Modifier.height(14.dp))
                if (m.limit == null) PrimaryButton(s.setLimit, Modifier.fillMaxWidth(), Icons.Rounded.HourglassTop, onClick = onLimit)
                else SecondaryButton(s.editLimit, Modifier.fillMaxWidth(), Icons.Rounded.Edit, onClick = onLimit)
            }
        }
        item {
            RCard {
                if (m.kind == TargetKind.APP) {
                    ToggleRow(Icons.Rounded.SelfImprovement, s.mindfulPause, s.mindfulPauseSub, m.pause, onPause, c.mint)
                    ToggleRow(Icons.Rounded.Block, s.blockInFocus, s.blockInFocusSub, inFocus, onFocusToggle, c.primary)
                } else {
                    Text(s.siteBlockNote, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
    }
}

@Composable
private fun BigStat(label: String, value: String, modifier: Modifier) {
    val c = R2.colors
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(c.surface).padding(18.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.text)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
}
