@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxHeight
import io.github.filipembcruz.rumo.ui.components.PopIn
import io.github.filipembcruz.rumo.ui.components.Appear
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Achievement
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.GoalProgress
import io.github.filipembcruz.rumo.data.GrowthModel
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.ui.BottomSpacer
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LifeGoal
import io.github.filipembcruz.rumo.ui.LocalLocale
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.DualBars
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Pill
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.ProgressLine
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.SectionHeader
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun GrowthRoute() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val locale = LocalLocale.current
    val scope = rememberCoroutineScope()
    val model = rememberModel(nav.tick, locale) { Repo.growth(context, locale) }
    val lifeGoals = remember(nav.tick) { Prefs.get(context).lifeGoals.mapNotNull { LifeGoal.of(it) } }
    var sheet by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<GoalProgress?>(null) }
    fun io(block: () -> Unit) = scope.launch { withContext(Dispatchers.IO) { block() }; nav.refresh() }
    if (model == null) { LoadingScreen(); return }
    GrowthScreen(model, lifeGoals.ifEmpty { listOf(LifeGoal.LANGUAGE, LifeGoal.BOOKS, LifeGoal.FITNESS, LifeGoal.DEGREE) },
        onAddGoal = { sheet = "goal" }, onLog = { sheet = "log" }, onGoal = { editing = it; sheet = "edit" })
    when (sheet) {
        "goal" -> GoalSheet(null, onDismiss = { sheet = null }, onSave = { g -> sheet = null; io { Actions.saveGoal(context, g) } }, onDelete = {})
        "edit" -> GoalSheet(editing?.goal, onDismiss = { sheet = null }, onSave = { g -> sheet = null; io { Actions.saveGoal(context, g) } },
            onDelete = { id -> sheet = null; io { Actions.deleteGoal(context, id) } })
        "log" -> {
            val goals = rememberModel(nav.tick) { Db.get(context).goals() } ?: emptyList()
            LogActivitySheet(goals, onDismiss = { sheet = null }) { goalId, label, minutes ->
                sheet = null; io { Actions.logActivity(context, goalId, label, minutes) }
            }
        }
    }
}

@Composable
fun GrowthScreen(m: GrowthModel, lifeGoals: List<LifeGoal>, onAddGoal: () -> Unit, onLog: () -> Unit, onGoal: (GoalProgress) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(s.growth, style = MaterialTheme.typography.headlineLarge, color = c.text, modifier = Modifier.statusBarsPadding().padding(top = 16.dp)) }
        item { StreakCard(m) }
        item {
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.thisWeekTitle, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(s.productiveVsDistracting, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    Text(Fmt.dur(m.productiveWeekMs), style = MaterialTheme.typography.titleLarge, color = c.mint)
                }
                Spacer(Modifier.height(14.dp))
                DualBars(m.weekProductive, m.weekDistracting, m.weekLabels, c.mint, c.coral)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend(c.mint, s.productive)
                    Legend(c.coral, s.distracting)
                }
            }
        }
        item { SectionHeader(s.yourGoals, s.add, onAddGoal) }
        if (m.goals.isEmpty()) item {
            RCard(onClick = onAddGoal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Flag, c.primarySoft, c.primary, 48.dp, 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.noGoalsTitle, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(s.noGoalsBody, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }
        m.goals.forEach { g -> item(key = "g${g.goal.id}") { GoalCard(g) { onGoal(g) } } }
        item { PrimaryButton(s.logActivity, Modifier.fillMaxWidth(), Icons.Rounded.EditNote, onClick = onLog) }
        item { LostTimeCard(m, lifeGoals) }
        item { SectionHeader(s.achievements) }
        item { AchievementsGrid(m.achievements) }
        if (m.recent.isNotEmpty()) {
            item { SectionHeader(s.recentActivity) }
            item {
                RCard {
                    m.recent.forEach { a ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.CheckCircle, c.mint.copy(alpha = 0.14f), c.mint, 36.dp, 18.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.label, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Fmt.day(LocalDate.parse(a.day), LocalLocale.current), style = MaterialTheme.typography.labelSmall, color = c.muted)
                            }
                            Text("+${Fmt.dur(a.minutes * 60_000L)}", style = MaterialTheme.typography.labelLarge, color = c.mint)
                        }
                    }
                }
            }
        }
        item { BottomSpacer() }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = R2.colors.muted)
    }
}

@Composable
private fun StreakCard(m: GrowthModel) {
    val s = LocalStrings.current
    val c = R2.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFD7FA7E), Color(0xFFB2EC4A))))
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(s.streakDays(m.streak), style = MaterialTheme.typography.headlineMedium, color = c.onLime)
                Text(s.streakSub, style = MaterialTheme.typography.bodyMedium, color = c.onLime.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
                Pill(s.bestStreak(m.bestStreak), Color.White.copy(alpha = 0.6f), c.onLime, icon = Icons.Rounded.EmojiEvents)
            }
        }
    }
}

@Composable
private fun GoalCard(g: GoalProgress, onClick: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val preset = LifeGoal.of(g.goal.preset)
    val color = preset?.color ?: c.primary
    val f = (g.doneHours / g.goal.targetHours).toFloat()
    RCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(preset?.icon ?: Icons.Rounded.Flag, color.copy(alpha = 0.15f), color, 46.dp, 23.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.goal.title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Fmt.hoursText(g.doneHours)} / ${Fmt.hoursText(g.goal.targetHours)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Text("${(f * 100).toInt().coerceAtMost(100)}%", style = MaterialTheme.typography.titleLarge, color = color)
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(f, color, height = 10.dp)
        Spacer(Modifier.height(10.dp))
        val remaining = (g.goal.targetHours - g.doneHours).coerceAtLeast(0.0)
        val eta = if (g.weekHours > 0.05) s.etaWeeks(Math.ceil(remaining / g.weekHours).toInt()) else s.etaUnknown
        Row {
            Text(s.thisWeekHours(Fmt.hoursText(g.weekHours)), style = MaterialTheme.typography.labelMedium, color = c.mint, modifier = Modifier.weight(1f))
            Text(eta, style = MaterialTheme.typography.labelMedium, color = c.muted)
        }
    }
}

@Composable
private fun LostTimeCard(m: GrowthModel, goals: List<LifeGoal>) {
    val s = LocalStrings.current
    val c = R2.colors
    val year = Fmt.hours(m.yearDistractingMs)
    val projected = Fmt.hours(m.projectedYearMs)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.navy).padding(20.dp)) {
        Column {
            Text(s.lostThisYear, style = MaterialTheme.typography.labelLarge, color = c.onNavy.copy(alpha = 0.7f))
            Text(Fmt.hoursText(year), style = MaterialTheme.typography.displayMedium, color = c.onNavy)
            Text(s.projectedYear(Fmt.hoursText(projected)), style = MaterialTheme.typography.bodyMedium, color = c.onNavy.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))
            Text(s.thatCouldBe, style = MaterialTheme.typography.titleSmall, color = c.onNavy)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                goals.take(3).forEachIndexed { i, g ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(g.icon, null, tint = c.lime, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(g.amount(projected), style = MaterialTheme.typography.headlineSmall, color = c.onNavy, textAlign = TextAlign.Center, maxLines = 1)
                        Text(s.goalUnit(g), style = MaterialTheme.typography.labelMedium, color = c.lime, textAlign = TextAlign.Center, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Text(s.goalName(g), style = MaterialTheme.typography.labelSmall, color = c.onNavy.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                    }
                }
            }
        }
    }
}

private fun achIcon(id: String) = when (id) {
    "first_focus", "focus_10" -> Icons.Rounded.HourglassTop
    "under_goal" -> Icons.Rounded.CheckCircle
    "streak_3", "streak_7", "streak_30" -> Icons.Rounded.LocalFireDepartment
    "productive_10h", "productive_100h" -> Icons.Rounded.Timer
    "early_bird" -> Icons.Rounded.WbSunny
    "night_off" -> Icons.Rounded.Bedtime
    "goal_set" -> Icons.Rounded.Flag
    "limit_set" -> Icons.Rounded.Lock
    else -> Icons.Rounded.WorkspacePremium
}

@Composable
private fun AchievementsGrid(list: List<Achievement>) {
    val s = LocalStrings.current
    val c = R2.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { j, a ->
                    val idx = list.indexOf(a)
                    Box(Modifier.weight(1f)) {
                        PopIn(idx, "ach:${a.id}") {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(vertical = 14.dp, horizontal = 8.dp)
                                    .graphicsLayer { alpha = if (a.unlocked) 1f else 0.6f },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                    if (a.unlocked) ShineRing(Modifier.size(56.dp))
                                    Box(
                                        Modifier.size(46.dp).clip(CircleShape)
                                            .background(if (a.unlocked) Brush.linearGradient(listOf(c.amber, Color(0xFFFF8A3D))) else Brush.linearGradient(listOf(c.surfaceAlt, c.surfaceAlt))),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(achIcon(a.id), null, tint = if (a.unlocked) Color.White else c.muted, modifier = Modifier.size(24.dp)) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(s.achievement(a.id), style = MaterialTheme.typography.labelMedium, color = c.text, textAlign = TextAlign.Center, maxLines = 2,
                                    minLines = 2)
                                Spacer(Modifier.height(6.dp))
                                if (!a.unlocked) ProgressLine(a.progress, c.amber, Modifier.padding(horizontal = 8.dp), height = 4.dp)
                                else Text(s.unlocked, style = MaterialTheme.typography.labelSmall, color = c.amber)
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Slowly rotating golden shine around unlocked badges. */
@Composable
private fun ShineRing(modifier: Modifier) {
    if (!io.github.filipembcruz.rumo.ui.UiFlags.animations) return
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "shine")
    val rot by t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(
        androidx.compose.animation.core.tween(3200, easing = androidx.compose.animation.core.LinearEasing)), label = "rot")
    androidx.compose.foundation.Canvas(modifier.graphicsLayer { rotationZ = rot }) {
        drawCircle(
            Brush.sweepGradient(listOf(Color.Transparent, Color(0xAAFFC24B), Color.Transparent, Color(0x66FF8A3D), Color.Transparent)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
        )
    }
}
