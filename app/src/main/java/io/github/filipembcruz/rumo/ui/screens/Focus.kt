@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Goal
import io.github.filipembcruz.rumo.data.LimitStatus
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.Schedule
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.BottomSpacer
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.AppIcon
import io.github.filipembcruz.rumo.ui.components.Banner
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Mood
import io.github.filipembcruz.rumo.ui.components.MoodOrb
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.Ring
import io.github.filipembcruz.rumo.ui.components.SectionHeader
import io.github.filipembcruz.rumo.ui.components.SelectChip
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.components.color
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FocusModel(
    val until: Long,
    val start: Long,
    val cats: Set<String>,
    val apps: Set<String>,
    val strict: Boolean,
    val schedules: List<Schedule>,
    val limits: List<LimitStatus>,
    val pauseApps: Set<String>,
    val goals: List<Goal>,
    val accessibility: Boolean,
)

class FocusActions(
    val onStart: (Int, Long?) -> Unit = { _, _ -> },
    val onStop: () -> Unit = {},
    val onBlockList: () -> Unit = {},
    val onSchedule: (Schedule?) -> Unit = {},
    val onToggleSchedule: (Schedule, Boolean) -> Unit = { _, _ -> },
    val onAddLimit: () -> Unit = {},
    val onLimit: (LimitStatus) -> Unit = {},
    val onAddPause: () -> Unit = {},
    val onApp: (String) -> Unit = {},
    val onEnableBlocking: () -> Unit = {},
)

@Composable
fun FocusRoute() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val model = rememberModel(nav.tick) {
        val p = Prefs.get(context)
        val db = Db.get(context)
        FocusModel(
            until = p.focusUntil, start = p.focusStart, cats = p.focusCats, apps = p.focusApps, strict = p.strict,
            schedules = db.schedules(), limits = Repo.limitStatuses(context), pauseApps = db.pauseApps(),
            goals = db.goals(), accessibility = Perms.canBlock(context),
        )
    }
    var sheet by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Schedule?>(null) }
    var limitTarget by remember { mutableStateOf<Pair<TargetKind, String>?>(null) }
    fun io(block: () -> Unit) = scope.launch { withContext(Dispatchers.IO) { block() }; nav.refresh() }

    if (model == null) { LoadingScreen(); return }
    FocusScreen(model, FocusActions(
        onStart = { min, goal -> io { Actions.startFocus(context, min, goal) } },
        onStop = { io { Actions.stopFocus(context, false) } },
        onBlockList = { sheet = "block" },
        onSchedule = { editing = it; sheet = "schedule" },
        onToggleSchedule = { sc, on -> io { Actions.saveSchedule(context, sc.copy(enabled = on)) } },
        onAddLimit = { sheet = "pick" },
        onLimit = { limitTarget = it.limit.kind to it.limit.target },
        onAddPause = { sheet = "pause" },
        onApp = { nav.openDetail(TargetKind.APP, it) },
        onEnableBlocking = nav.setupBlocking,
    ))
    when (sheet) {
        "block" -> BlockListSheet(model.cats, model.apps, onDismiss = { sheet = null; nav.refresh() })
        "schedule" -> ScheduleSheet(editing, onDismiss = { sheet = null },
            onSave = { sc -> sheet = null; io { Actions.saveSchedule(context, sc) } },
            onDelete = { id -> sheet = null; io { Actions.deleteSchedule(context, id) } })
        "pick" -> TargetPickerSheet(onDismiss = { sheet = null }) { k, id -> sheet = null; limitTarget = k to id }
        "pause" -> AppPickerSheet(onDismiss = { sheet = null }) { pkg -> sheet = null; io { Actions.setPause(context, pkg, true) } }
    }
    limitTarget?.let { (k, id) -> LimitSheetRoute(k, id, onDone = { limitTarget = null; nav.refresh() }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(m: FocusModel, a: FocusActions) {
    val s = LocalStrings.current
    val c = R2.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(s.focus, style = MaterialTheme.typography.headlineLarge, color = c.text, modifier = Modifier.statusBarsPadding().padding(top = 16.dp))
        }
        if (!m.accessibility) item {
            Banner(Icons.Rounded.Shield, s.enableBlockingBanner, s.enable, c.coralSoft, c.coral, a.onEnableBlocking)
        }
        item { FocusCard(m, a) }
        item {
            RCard(onClick = a.onBlockList) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Block, c.coral.copy(alpha = 0.14f), c.coral)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.whatGetsBlocked, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(s.blockSummary(m.cats.size, m.apps.size), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    m.cats.map { Cat.of(it) }.forEach { cat ->
                        io.github.filipembcruz.rumo.ui.components.Pill(s.cat(cat), cat.color().copy(alpha = 0.15f), cat.color())
                    }
                    m.apps.take(6).forEach { AppIcon(it, 26.dp) }
                }
            }
        }
        item { SectionHeader(s.schedules, s.add) { a.onSchedule(null) } }
        if (m.schedules.isEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PresetSchedule(Icons.Rounded.Bedtime, s.bedtime, "22:30 – 07:00", c.primary, Modifier.weight(1f)) {
                    a.onSchedule(Schedule(0, s.bedtime, 22 * 60 + 30, 7 * 60, 0b1111111, setOf("social", "video", "games", "shopping"), emptySet(), true, "moon"))
                }
                PresetSchedule(Icons.Rounded.Work, s.workHours, "09:00 – 18:00", c.sky, Modifier.weight(1f)) {
                    a.onSchedule(Schedule(0, s.workHours, 9 * 60, 18 * 60, 0b0011111, setOf("social", "video", "games"), emptySet(), true, "work"))
                }
            }
        }
        m.schedules.forEach { sc -> item(key = "sc${sc.id}") { ScheduleCard(sc, a) } }
        item { SectionHeader(s.limits, s.add, a.onAddLimit) }
        item {
            RCard {
                if (m.limits.isEmpty()) Text(s.noLimitsYet, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                m.limits.forEach { LimitRow(it) { a.onLimit(it) } }
            }
        }
        item { SectionHeader(s.mindfulPause, s.add, a.onAddPause) }
        item {
            RCard {
                Text(s.mindfulPauseExplain, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                if (m.pauseApps.isNotEmpty()) Spacer(Modifier.height(12.dp))
                val context = LocalContext.current
                m.pauseApps.forEach { pkg ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).bounceClick { a.onApp(pkg) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(pkg, 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(remember(pkg) { AppNames.label(context, pkg) }, style = MaterialTheme.typography.titleSmall, color = c.text)
                    }
                }
            }
        }
        item { BottomSpacer() }
    }
}

@Composable
private fun FocusCard(m: FocusModel, a: FocusActions) {
    val s = LocalStrings.current
    val c = R2.colors
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val active = m.until > now
    LaunchedEffect(m.until) {
        while (true) { now = System.currentTimeMillis(); if (m.until <= now) break; delay(1000) }
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(if (active) listOf(Color(0xFF2B1F7A), Color(0xFF1A1450)) else listOf(Color(0xFF7B5CFF), Color(0xFF4A2FD9))))
            .padding(22.dp)
    ) {
        AnimatedContent(active, transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150)) }, label = "focus") { on ->
            if (on) {
                val total = (m.until - m.start).coerceAtLeast(1)
                val left = (m.until - now).coerceAtLeast(0)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.deepFocus, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(14.dp))
                    Ring(left.toFloat() / total, size = 210.dp, thickness = 16.dp, colors = listOf(c.lime, Color(0xFF8DF0C7))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MoodOrb(Mood.FOCUS, size = 54.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(Fmt.countdown(left), style = MaterialTheme.typography.displaySmall, color = Color.White)
                            Text(s.untilClock(Fmt.clock(m.until)), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(s.focusRunningNote, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    if (m.strict) HoldToStop(s.holdToStop, a.onStop)
                    else PrimaryButton(s.endSession, Modifier.fillMaxWidth(), Icons.Rounded.Stop, bg = Color.White.copy(alpha = 0.16f), fg = Color.White, onClick = a.onStop)
                }
            } else {
                var minutes by remember { mutableFloatStateOf(25f) }
                var goal by remember { mutableStateOf<Long?>(null) }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.focusSession, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                            Text(s.minutesBig(minutes.toInt()), style = MaterialTheme.typography.displaySmall, color = Color.White)
                        }
                        MoodOrb(Mood.FOCUS, size = 70.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60, 90).forEach { v ->
                            val sel = minutes.toInt() == v
                            Box(
                                Modifier.weight(1f).height(40.dp).bounceClick { minutes = v.toFloat() }.clip(RoundedCornerShape(14.dp))
                                    .background(if (sel) c.lime else Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) { Text("$v", color = if (sel) c.onLime else Color.White, style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                    Slider(
                        value = minutes, onValueChange = { minutes = (it / 5f).toInt() * 5f }, valueRange = 5f..180f,
                        colors = SliderDefaults.colors(thumbColor = c.lime, activeTrackColor = c.lime, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                    )
                    if (m.goals.isNotEmpty()) {
                        Text(s.countTowards, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            m.goals.take(3).forEach { g ->
                                val sel = goal == g.id
                                Box(
                                    Modifier.bounceClick { goal = if (sel) null else g.id }.clip(RoundedCornerShape(50))
                                        .background(if (sel) Color.White else Color.White.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(g.title, color = if (sel) c.primaryDeep else Color.White, style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton(s.startFocus, Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow, bg = c.lime, fg = c.onLime) {
                        a.onStart(minutes.toInt(), goal)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldToStop(label: String, onStop: () -> Unit) {
    val c = R2.colors
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(holding) {
        if (holding) {
            val start = System.currentTimeMillis()
            while (holding) {
                progress = ((System.currentTimeMillis() - start) / 3000f).coerceAtMost(1f)
                if (progress >= 1f) { onStop(); break }
                delay(16)
            }
        } else progress = 0f
    }
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.14f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth(progress).height(56.dp).background(c.coral.copy(alpha = 0.6f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun PresetSchedule(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, time: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = R2.colors
    Column(modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(24.dp)).background(c.surface).padding(16.dp)) {
        IconBadge(icon, color.copy(alpha = 0.15f), color)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
        Text(time, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, null, tint = color, modifier = Modifier.size(16.dp))
            Text(LocalStrings.current.add, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun ScheduleCard(sc: Schedule, a: FocusActions) {
    val s = LocalStrings.current
    val c = R2.colors
    val icon = when (sc.icon) { "work" -> Icons.Rounded.Work; "moon" -> Icons.Rounded.Bedtime; else -> Icons.Rounded.HourglassTop }
    RCard(onClick = { a.onSchedule(sc) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, c.primary.copy(alpha = 0.14f), c.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sc.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("${Fmt.clockMin(sc.startMin)} – ${Fmt.clockMin(sc.endMin)} · ${s.daysText(sc.days)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
                Text(s.blockSummary(sc.cats.size, sc.apps.size), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            Switch(sc.enabled, { a.onToggleSchedule(sc, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = c.primary, uncheckedTrackColor = c.surfaceAlt, uncheckedBorderColor = Color.Transparent))
        }
    }
}

