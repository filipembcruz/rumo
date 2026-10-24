@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.filipembcruz.rumo.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LifeGoal
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.GhostButton
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.Mood
import io.github.filipembcruz.rumo.ui.components.MoodOrb
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.SelectChip
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STEPS = 7

@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    val nav = LocalNav.current
    val prefs = remember { Prefs.get(context) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var goals by remember { mutableStateOf(prefs.lifeGoals) }
    var name by remember { mutableStateOf(prefs.name) }
    var goalMin by remember { mutableFloatStateOf(prefs.dailyGoalMin.toFloat()) }
    var lang by remember { mutableStateOf(prefs.language) }
    val usage = remember(nav.tick, step) { Perms.usage(context) }
    val a11y = remember(nav.tick, step) { Perms.accessibility(context) }
    var notifTick by remember { mutableIntStateOf(0) }
    val notif = remember(nav.tick, step, notifTick) { Perms.notifications(context) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifTick++ }
    val reality = rememberModel(usage, step == 4) {
        if (usage) { Collector.sync(context); Repo.recentDailyAverage(context) } else 0L
    }

    fun go(to: Int) { forward = to > step; step = to.coerceIn(0, STEPS - 1) }
    // Going back skips the permission step once access is granted.
    fun back() { go(if (step == 4 && usage) 2 else step - 1) }
    BackHandler(enabled = step > 0) { back() }
    // Auto-advance only when access was just granted on the permission step.
    var wasGranted by remember { mutableStateOf(usage) }
    LaunchedEffect(usage) {
        if (usage && !wasGranted && step == 3) go(4)
        wasGranted = usage
    }

    OnboardingScreen(
        step = step, forward = forward, lang = lang, name = name, goals = goals, goalMin = goalMin.toInt(),
        usage = usage, a11y = a11y, notif = notif, dailyAvg = reality ?: 0L,
        onLang = { lang = it; prefs.language = it },
        onName = { name = it },
        onToggleGoal = { id -> goals = if (id in goals) goals - id else goals + id },
        onGoalMin = { goalMin = it.toFloat() },
        onUsage = { Perms.openUsageSettings(context) },
        onA11y = nav.setupBlocking,
        onNotif = {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else Perms.openNotificationSettings(context)
        },
        onBack = { back() },
        onNext = {
            when (step) {
                1 -> { prefs.name = name.trim(); prefs.lifeGoals = goals }
                2 -> prefs.dailyGoalMin = goalMin.toInt()
            }
            if (step == STEPS - 1) onDone() else go(if (step == 2 && usage) 4 else step + 1)
        },
    )
}

@Composable
fun OnboardingScreen(
    step: Int, forward: Boolean, lang: String, name: String, goals: Set<String>, goalMin: Int,
    usage: Boolean, a11y: Boolean, notif: Boolean, dailyAvg: Long,
    onLang: (String) -> Unit, onName: (String) -> Unit, onToggleGoal: (String) -> Unit, onGoalMin: (Int) -> Unit,
    onUsage: () -> Unit, onA11y: () -> Unit, onNotif: () -> Unit, onBack: () -> Unit, onNext: () -> Unit,
) {
    val s = LocalStrings.current
    val c = R2.colors
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).bounceClick(enabled = step > 0, onClick = onBack).clip(CircleShape).background(if (step > 0) c.surface else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { if (step > 0) Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.back, tint = c.text) }
            Spacer(Modifier.width(14.dp))
            val p by animateFloatAsState((step + 1f) / STEPS, tween(400), label = "obp")
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(c.surfaceAlt)) {
                Box(Modifier.fillMaxWidth(p).height(8.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(c.primary, c.primaryDeep))))
            }
            Spacer(Modifier.width(56.dp))
        }
        AnimatedContent(
            step, Modifier.weight(1f),
            transitionSpec = {
                val d = if (forward) 1 else -1
                (slideInHorizontally(tween(350)) { d * it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { -d * it / 4 } + fadeOut(tween(200)))
            },
            label = "ob",
        ) { st ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (st) {
                    0 -> Welcome(lang, onLang)
                    1 -> GoalsStep(name, goals, onName, onToggleGoal)
                    2 -> TargetStep(goalMin, onGoalMin)
                    3 -> PermStep(Icons.Rounded.QueryStats, s.obUsageTitle, s.obUsageBody, listOf(s.obUsage1, s.obUsage2, s.obUsage3), usage, s.grantAccess, onUsage)
                    4 -> RealityStep(dailyAvg, goals)
                    5 -> SuperStep(a11y, notif, onA11y, onNotif)
                    else -> DoneStep()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            val label = when (step) {
                0 -> s.getStarted
                3 -> if (usage) s.continueLabel else s.grantAccess
                STEPS - 1 -> s.letsGo
                else -> s.continueLabel
            }
            PrimaryButton(label, Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.ArrowForward, enabled = step != 1 || goals.isNotEmpty()) {
                if (step == 3 && !usage) onUsage() else onNext()
            }
            if (step == 5) GhostButton(s.maybeLater, Modifier.fillMaxWidth(), onClick = onNext)
        }
    }
}

@Composable
private fun ColumnScope.Title(title: String, body: String?) {
    val c = R2.colors
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge, color = c.text)
    if (body != null) {
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.muted)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Welcome(lang: String, onLang: (String) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        MoodOrb(Mood.HAPPY, size = 180.dp)
        Spacer(Modifier.height(30.dp))
        Text(s.obWelcomeTitle, style = MaterialTheme.typography.displaySmall, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(s.obWelcomeBody, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectChip("English", lang == "en", { onLang("en") })
            SelectChip("Português", lang == "pt", { onLang("pt") })
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = c.mint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.obPrivate, style = MaterialTheme.typography.labelMedium, color = c.muted)
        }
    }
}

@Composable
private fun GoalsStep(name: String, goals: Set<String>, onName: (String) -> Unit, onToggle: (String) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    Column {
        Title(s.obGoalsTitle, s.obGoalsBody)
        OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(s.yourNameOptional) }, singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.primary, unfocusedBorderColor = c.outline,
                focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, focusedTextColor = c.text, unfocusedTextColor = c.text))
        Spacer(Modifier.height(18.dp))
        LifeGoal.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                row.forEach { g ->
                    val sel = g.id in goals
                    Column(
                        Modifier.weight(1f).bounceClick { onToggle(g.id) }.clip(RoundedCornerShape(24.dp))
                            .background(if (sel) g.color.copy(alpha = 0.14f) else c.surface)
                            .border(2.dp, if (sel) g.color else Color.Transparent, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                    ) {
                        Row {
                            IconBadge(g.icon, g.color.copy(alpha = 0.16f), g.color, 42.dp, 22.dp)
                            Spacer(Modifier.weight(1f))
                            if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = g.color)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(s.goalName(g), style = MaterialTheme.typography.titleSmall, color = c.text, minLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetStep(goalMin: Int, onGoalMin: (Int) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    Column {
        Title(s.obTargetTitle, s.obTargetBody)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFF4A2FD9)))).padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MoodOrb(if (goalMin <= 180) Mood.HAPPY else if (goalMin <= 300) Mood.CALM else Mood.WORRIED, size = 72.dp)
                Spacer(Modifier.height(12.dp))
                AnimatedDuration(goalMin * 60_000L, Color.White)
                Text(s.perDay, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Slider(goalMin.toFloat(), { onGoalMin(((it / 15f).toInt() * 15).coerceAtLeast(30)) }, valueRange = 30f..600f,
            colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceAlt))
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 120, 180, 240, 300).forEach { v -> SelectChip(Fmt.dur(v * 60_000L), goalMin == v, { onGoalMin(v) }) }
        }
        Spacer(Modifier.height(16.dp))
        Text(s.obTargetHint, style = MaterialTheme.typography.bodyMedium, color = c.muted)
    }
}

@Composable
private fun PermStep(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, bullets: List<String>,
                     granted: Boolean, action: String, onAction: () -> Unit) {
    val c = R2.colors
    val s = LocalStrings.current
    Column {
        Spacer(Modifier.height(16.dp))
        IconBadge(icon, c.primarySoft, c.primary, 72.dp, 36.dp)
        Title(title, body)
        bullets.forEach { b ->
            Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.CheckCircle, null, tint = c.mint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(b, style = MaterialTheme.typography.bodyMedium, color = c.text)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, null, tint = c.primary)
            Spacer(Modifier.width(12.dp))
            Text(s.obPermPrivacy, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        if (granted) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = c.mint)
                Spacer(Modifier.width(8.dp))
                Text(s.accessGranted, style = MaterialTheme.typography.titleSmall, color = c.mint)
            }
        }
    }
}

@Composable
private fun RealityStep(dailyAvg: Long, goalIds: Set<String>) {
    val s = LocalStrings.current
    val c = R2.colors
    val goals = goalIds.mapNotNull { LifeGoal.of(it) }.ifEmpty { listOf(LifeGoal.LANGUAGE, LifeGoal.BOOKS) }
    val week = dailyAvg * 7
    val month = dailyAvg * 30
    val year = dailyAvg * 365
    Column {
        Title(s.obRealityTitle, if (dailyAvg > 0) s.obRealityBody else s.obRealityNoData)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.primarySoft).padding(22.dp)) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Fmt.hoursText(Fmt.hours(week)), style = MaterialTheme.typography.displayMedium, color = c.primaryDeep)
                    Text(" / ${s.week.lowercase()}", style = MaterialTheme.typography.titleMedium, color = c.primary, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(s.onYourPhone, style = MaterialTheme.typography.bodyMedium, color = c.primary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RealityTile(s.perMonth, Fmt.hoursText(Fmt.hours(month)), Modifier.weight(1f))
                    RealityTile(s.perYear, Fmt.hoursText(Fmt.hours(year)), Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(s.obRealityEquiv, style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(10.dp))
        goals.take(4).forEach { g ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(20.dp)).background(c.surface).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconBadge(g.icon, g.color.copy(alpha = 0.15f), g.color)
                Spacer(Modifier.width(12.dp))
                Text(s.goalName(g), style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                Text(s.goalAmount(g, Fmt.hours(year) * 0.25), style = MaterialTheme.typography.titleMedium, color = g.color)
            }
        }
        Text(s.obRealityFoot, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
private fun RealityTile(label: String, value: String, modifier: Modifier) {
    val c = R2.colors
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.text)
    }
}

@Composable
private fun SuperStep(a11y: Boolean, notif: Boolean, onA11y: () -> Unit, onNotif: () -> Unit) {
    val s = LocalStrings.current
    Column {
        Title(s.obSuperTitle, s.obSuperBody)
        SuperCard(Icons.Rounded.AccessibilityNew, s.obSitesTitle, s.obSitesBody, a11y, onA11y)
        Spacer(Modifier.height(12.dp))
        SuperCard(Icons.Rounded.Notifications, s.obNotifTitle, s.obNotifBody, notif, onNotif)
    }
}

@Composable
private fun SuperCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, on: Boolean, onClick: () -> Unit) {
    val c = R2.colors
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().bounceClick(enabled = !on, onClick = onClick).clip(RoundedCornerShape(26.dp)).background(c.surface).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, if (on) c.mint.copy(alpha = 0.15f) else c.primarySoft, if (on) c.mint else c.primary, 46.dp, 24.dp)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            if (on) Icon(Icons.Rounded.CheckCircle, null, tint = c.mint)
            else Text(s.enable, style = MaterialTheme.typography.labelLarge, color = c.primary)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
private fun DoneStep() {
    val s = LocalStrings.current
    val c = R2.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MoodOrb(Mood.CELEBRATE, size = 170.dp)
        Spacer(Modifier.height(30.dp))
        Text(s.obDoneTitle, style = MaterialTheme.typography.displaySmall, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(s.obDoneBody, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
    }
}
