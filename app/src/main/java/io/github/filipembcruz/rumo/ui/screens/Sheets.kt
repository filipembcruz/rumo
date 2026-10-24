@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Categorizer
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.Goal
import io.github.filipembcruz.rumo.data.Limit
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.Schedule
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LifeGoal
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.AppIcon
import io.github.filipembcruz.rumo.ui.components.CatDot
import io.github.filipembcruz.rumo.ui.components.GhostButton
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.Segmented
import io.github.filipembcruz.rumo.ui.components.SelectChip
import io.github.filipembcruz.rumo.ui.components.SiteAvatar
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.components.color
import io.github.filipembcruz.rumo.ui.rememberModel
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun RumoSheet(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = R2.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = c.text)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun field() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = R2.colors.primary, unfocusedBorderColor = R2.colors.outline,
    focusedContainerColor = R2.colors.surface, unfocusedContainerColor = R2.colors.surface,
    focusedTextColor = R2.colors.text, unfocusedTextColor = R2.colors.text,
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = R2.colors.primary, activeTrackColor = R2.colors.primary, inactiveTrackColor = R2.colors.surfaceAlt,
)

// ------------------------------------------------------------------ log activity
@Composable
fun LogActivitySheet(goals: List<Goal>, onDismiss: () -> Unit, onSave: (Long?, String, Int) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    var goal by remember { mutableStateOf(goals.firstOrNull()) }
    var label by remember { mutableStateOf(goals.firstOrNull()?.title ?: "") }
    var minutes by remember { mutableFloatStateOf(30f) }
    RumoSheet(onDismiss, s.logActivity) {
        Text(s.logActivitySub, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        Spacer(Modifier.height(14.dp))
        if (goals.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { g ->
                    SelectChip(g.title, goal?.id == g.id, { goal = g; label = g.title }, LifeGoal.of(g.preset)?.icon)
                }
                SelectChip(s.otherActivity, goal == null, { goal = null; label = "" })
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text(s.whatDidYouDo) }, singleLine = true,
            shape = RoundedCornerShape(18.dp), colors = field())
        Spacer(Modifier.height(18.dp))
        Text(s.minutesBig(minutes.toInt()), style = MaterialTheme.typography.headlineMedium, color = c.text)
        Slider(minutes, { minutes = (it / 5f).toInt() * 5f }, valueRange = 5f..240f, colors = sliderColors())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60, 90, 120).forEach { v -> SelectChip("${v}m", minutes.toInt() == v, { minutes = v.toFloat() }) }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(s.save, Modifier.fillMaxWidth(), Icons.Rounded.Check, enabled = label.isNotBlank()) {
            onSave(goal?.id, label.trim(), minutes.toInt())
        }
    }
}

// ------------------------------------------------------------------ target picker
@Composable
fun TargetPickerSheet(onDismiss: () -> Unit, onPick: (TargetKind, String) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val apps = rememberModel(Unit) { AppNames.launchable(context) } ?: emptyList()
    val sites = rememberModel(Unit) {
        val today = LocalDate.now()
        Db.get(context).webTotals(today.minusDays(30).toString(), today.toString()).map { it.first }
    } ?: emptyList()
    RumoSheet(onDismiss, s.chooseWhatToLimit) {
        Segmented(listOf(s.apps, s.sites, s.categories), tab, { tab = it; query = "" })
        Spacer(Modifier.height(12.dp))
        if (tab != 2) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(if (tab == 0) s.searchApps else s.typeDomain) },
                leadingIcon = { Icon(if (tab == 0) Icons.Rounded.Search else Icons.Rounded.Public, null) },
                shape = RoundedCornerShape(18.dp), colors = field())
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            when (tab) {
                0 -> items(apps.filter { query.isBlank() || it.second.contains(query, true) }, key = { it.first }) { (pkg, label) ->
                    PickRow({ AppIcon(pkg, 38.dp) }, label) { onPick(TargetKind.APP, pkg) }
                }
                1 -> {
                    val typed = query.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
                    if (typed.contains('.') && typed !in sites) item {
                        PickRow({ IconBadge(Icons.Rounded.Add, c.primarySoft, c.primary, 38.dp) }, s.addSite(typed)) { onPick(TargetKind.SITE, typed) }
                    }
                    items(sites.filter { query.isBlank() || it.contains(query, true) }) { d ->
                        PickRow({ SiteAvatar(d, Categorizer.site(context, d), 38.dp) }, d) { onPick(TargetKind.SITE, d) }
                    }
                }
                else -> items(Cat.entries.filter { it != Cat.OTHER }) { cat ->
                    PickRow({ Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cat.color().copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { CatDot(cat, 14.dp) } },
                        s.cat(cat)) { onPick(TargetKind.CATEGORY, cat.id) }
                }
            }
        }
    }
}

@Composable
fun AppPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = rememberModel(Unit) { AppNames.launchable(context) } ?: emptyList()
    RumoSheet(onDismiss, s.chooseApp) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(s.searchApps) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(18.dp), colors = field())
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
            items(apps.filter { query.isBlank() || it.second.contains(query, true) }, key = { it.first }) { (pkg, label) ->
                PickRow({ AppIcon(pkg, 38.dp) }, label) { onPick(pkg) }
            }
        }
    }
}

@Composable
private fun PickRow(leading: @Composable () -> Unit, title: String, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).bounceClick(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = R2.colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

// ------------------------------------------------------------------ limit
@Composable
fun LimitSheetRoute(kind: TargetKind, id: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data = rememberModel(kind, id) {
        Db.get(context).limits().firstOrNull { it.kind == kind && it.target == id } to Repo.usedToday(context, kind, id)
    } ?: return
    LimitSheet(kind, id, data.first, data.second, onDismiss = onDone,
        onSave = { min, block ->
            scope.launch {
                withContext(Dispatchers.IO) { Actions.saveLimit(context, Limit(data.first?.id ?: 0, kind, id, min, block)) }
                onDone()
            }
        },
        onDelete = { lid -> scope.launch { withContext(Dispatchers.IO) { Actions.deleteLimit(context, lid) }; onDone() } })
}

@Composable
fun LimitSheet(kind: TargetKind, id: String, existing: Limit?, usedToday: Long, onDismiss: () -> Unit,
               onSave: (Int, Boolean) -> Unit, onDelete: (Long) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    var minutes by remember { mutableFloatStateOf((existing?.minutes ?: 60).toFloat()) }
    var block by remember { mutableStateOf(existing?.block ?: true) }
    val name = when (kind) {
        TargetKind.APP -> remember(id) { AppNames.label(context, id) }
        TargetKind.SITE -> id
        TargetKind.CATEGORY -> s.cat(Cat.of(id))
    }
    RumoSheet(onDismiss, s.dailyLimitFor(name)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (kind) {
                TargetKind.APP -> AppIcon(id, 48.dp)
                TargetKind.SITE -> SiteAvatar(id, Categorizer.site(context, id), 48.dp)
                TargetKind.CATEGORY -> Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Cat.of(id).color().copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) { CatDot(Cat.of(id), 16.dp) }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(Fmt.dur(minutes.toLong() * 60_000L), style = MaterialTheme.typography.displaySmall, color = c.text)
                Text(s.usedTodaySoFar(Fmt.dur(usedToday)), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(minutes, { minutes = (it / 5f).toInt().coerceAtLeast(1) * 5f }, valueRange = 5f..300f, colors = sliderColors())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60, 90, 120, 180).forEach { v -> SelectChip(Fmt.dur(v * 60_000L), minutes.toInt() == v, { minutes = v.toFloat() }) }
        }
        Spacer(Modifier.height(18.dp))
        Text(s.whenReached, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        Segmented(listOf(s.alertMe, s.blockIt), if (block) 1 else 0, { block = it == 1 })
        Spacer(Modifier.height(6.dp))
        Text(if (block) s.blockItSub else s.alertMeSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(s.save, Modifier.fillMaxWidth(), Icons.Rounded.Check) { onSave(minutes.toInt(), block) }
        if (existing != null) GhostButton(s.removeLimit, Modifier.fillMaxWidth(), c.coral) { onDelete(existing.id) }
    }
}

// ------------------------------------------------------------------ category
@Composable
fun CategorySheet(current: Cat, onDismiss: () -> Unit, onPick: (Cat) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    RumoSheet(onDismiss, s.chooseCategory) {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            Cat.entries.forEach { cat ->
                PickRow(
                    { Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cat.color().copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { CatDot(cat, 14.dp) } },
                    s.cat(cat),
                    trailing = {
                        Text(
                            when { cat.productive -> s.productive; cat.distracting -> s.distracting; else -> s.neutral },
                            style = MaterialTheme.typography.labelSmall,
                            color = when { cat.productive -> c.mint; cat.distracting -> c.coral; else -> c.muted },
                        )
                        if (cat == current) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Check, null, tint = c.primary)
                        }
                    },
                ) { onPick(cat) }
            }
        }
    }
}

// ------------------------------------------------------------------ focus block list
@Composable
fun BlockListSheet(cats: Set<String>, apps: Set<String>, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    var selCats by remember { mutableStateOf(cats) }
    var selApps by remember { mutableStateOf(apps) }
    var picking by remember { mutableStateOf(false) }
    RumoSheet(onDismiss, s.whatGetsBlocked) {
        Text(s.categories, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Cat.entries.filter { it != Cat.BROWSER && it != Cat.UTILITIES && it != Cat.OTHER }.forEach { cat ->
                val on = cat.id in selCats
                SelectChip(s.cat(cat), on, {
                    selCats = if (on) selCats - cat.id else selCats + cat.id
                    prefs.focusCats = selCats
                }, accent = cat.color())
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.apps, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            GhostButton(s.add, color = c.primary) { picking = true }
        }
        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            selApps.forEach { pkg ->
                PickRow({ AppIcon(pkg, 34.dp) }, remember(pkg) { AppNames.label(context, pkg) },
                    trailing = { Icon(Icons.Rounded.Delete, null, tint = c.muted) }) {
                    selApps = selApps - pkg; prefs.focusApps = selApps
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(s.done, Modifier.fillMaxWidth(), Icons.Rounded.Check, onClick = onDismiss)
    }
    if (picking) AppPickerSheet(onDismiss = { picking = false }) { pkg ->
        picking = false; selApps = selApps + pkg; prefs.focusApps = selApps
    }
}

// ------------------------------------------------------------------ schedule
@Composable
fun ScheduleSheet(existing: Schedule?, onDismiss: () -> Unit, onSave: (Schedule) -> Unit, onDelete: (Long) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    var name by remember { mutableStateOf(existing?.name ?: s.bedtime) }
    var start by remember { mutableIntStateOf(existing?.startMin ?: (22 * 60 + 30)) }
    var end by remember { mutableIntStateOf(existing?.endMin ?: (7 * 60)) }
    var days by remember { mutableIntStateOf(existing?.days ?: 0b1111111) }
    var cats by remember { mutableStateOf(existing?.cats ?: setOf("social", "video", "games", "shopping")) }
    var editingTime by remember { mutableStateOf<Int?>(null) }
    RumoSheet(onDismiss, if (existing == null || existing.id == 0L) s.newSchedule else s.editSchedule) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(s.name) }, singleLine = true,
            shape = RoundedCornerShape(18.dp), colors = field())
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeBox(s.from, Fmt.clockMin(start), Modifier.weight(1f)) { editingTime = 0 }
            TimeBox(s.to, Fmt.clockMin(end), Modifier.weight(1f)) { editingTime = 1 }
        }
        Spacer(Modifier.height(14.dp))
        Text(s.days, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.weekdayLetters.forEachIndexed { i, l ->
                val on = days and (1 shl i) != 0
                Box(
                    Modifier.weight(1f).height(42.dp).bounceClick { days = days xor (1 shl i) }.clip(RoundedCornerShape(14.dp))
                        .background(if (on) c.primary else c.surface),
                    contentAlignment = Alignment.Center,
                ) { Text(l, color = if (on) c.onPrimary else c.text, style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(s.blockCategories, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Cat.entries.filter { it != Cat.BROWSER && it != Cat.UTILITIES && it != Cat.OTHER }.forEach { cat ->
                val on = cat.id in cats
                SelectChip(s.cat(cat), on, { cats = if (on) cats - cat.id else cats + cat.id }, accent = cat.color())
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(s.save, Modifier.fillMaxWidth(), Icons.Rounded.Check, enabled = name.isNotBlank() && days != 0) {
            onSave(Schedule(existing?.id ?: 0, name.trim(), start, end, days, cats, existing?.apps ?: emptySet(), existing?.enabled ?: true, existing?.icon ?: "moon"))
        }
        if (existing != null && existing.id > 0) GhostButton(s.delete, Modifier.fillMaxWidth(), c.coral) { onDelete(existing.id) }
    }
    editingTime?.let { which ->
        val initial = if (which == 0) start else end
        val state = rememberTimePickerState(initial / 60, initial % 60, true)
        AlertDialog(
            onDismissRequest = { editingTime = null },
            confirmButton = {
                TextButton({
                    val v = state.hour * 60 + state.minute
                    if (which == 0) start = v else end = v
                    editingTime = null
                }) { Text(s.ok) }
            },
            dismissButton = { TextButton({ editingTime = null }) { Text(s.cancel) } },
            text = { TimePicker(state) },
            containerColor = c.surface,
        )
    }
}

@Composable
private fun TimeBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val c = R2.colors
    Column(modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = c.text)
    }
}

// ------------------------------------------------------------------ goal
@Composable
fun GoalSheet(existing: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit, onDelete: (Long) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    var preset by remember { mutableStateOf(LifeGoal.of(existing?.preset)) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var hours by remember { mutableFloatStateOf((existing?.targetHours ?: 100.0).toFloat()) }
    RumoSheet(onDismiss, if (existing == null) s.newGoal else s.editGoal) {
        if (existing == null) {
            Text(s.pickPreset, style = MaterialTheme.typography.titleSmall, color = c.text)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LifeGoal.entries.forEach { g ->
                    SelectChip(s.goalName(g), preset == g, {
                        preset = g; title = s.goalName(g)
                        hours = if (g.countable) (g.hours * 10).toFloat() else g.hours.toFloat()
                    }, g.icon, accent = g.color)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(s.goalTitle) }, singleLine = true,
            shape = RoundedCornerShape(18.dp), colors = field())
        Spacer(Modifier.height(14.dp))
        Text(s.targetHours(hours.toInt()), style = MaterialTheme.typography.headlineSmall, color = c.text)
        Slider(hours, { hours = if (it < 100) (it / 5f).toInt() * 5f else (it / 50f).toInt() * 50f }, valueRange = 5f..3500f, colors = sliderColors())
        Text(s.targetHint, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(s.save, Modifier.fillMaxWidth(), Icons.Rounded.Check, enabled = title.isNotBlank()) {
            onSave(Goal(existing?.id ?: 0, title.trim(), preset?.id, hours.toDouble(), existing?.created ?: System.currentTimeMillis()))
        }
        if (existing != null) GhostButton(s.delete, Modifier.fillMaxWidth(), c.coral) { onDelete(existing.id) }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = R2.colors.coral) } },
        dismissButton = { TextButton(onDismiss) { Text(s.cancel) } },
        containerColor = R2.colors.surface,
    )
}

