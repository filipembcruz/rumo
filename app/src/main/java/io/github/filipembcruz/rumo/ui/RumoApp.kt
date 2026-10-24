package io.github.filipembcruz.rumo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import io.github.filipembcruz.rumo.RumoApplication
import io.github.filipembcruz.rumo.data.Grouping
import io.github.filipembcruz.rumo.data.PeriodStats
import io.github.filipembcruz.rumo.data.Stats
import io.github.filipembcruz.rumo.data.UsageCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private enum class Tab(val label: String) { TODAY("Hoje"), STATS("Estatísticas") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RumoApp(resumeTick: Int) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageCollector.hasPermission(context)) }
    LaunchedEffect(resumeTick) { hasPermission = UsageCollector.hasPermission(context) }

    CrashReportDialog()

    if (!hasPermission) {
        PermissionScreen()
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var customFrom by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(13).toString()) }
    var customTo by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    var today by remember { mutableStateOf<PeriodStats?>(null) }
    var week by remember { mutableStateOf<PeriodStats?>(null) }
    var periodStats by remember { mutableStateOf<PeriodStats?>(null) }

    LaunchedEffect(resumeTick, refresh, period, customFrom, customTo) {
        loading = true
        withContext(Dispatchers.IO) {
            runCatching { UsageCollector.sync(context) }
            val now = LocalDate.now()
            today = Stats.load(context, now, now, Grouping.DAY)
            week = Stats.load(context, now.minusDays(6), now, Grouping.DAY)
            periodStats = when (period) {
                Period.WEEK -> week
                Period.MONTH -> Stats.load(context, now.minusDays(29), now, Grouping.DAY)
                Period.YEAR -> Stats.load(context, now.withDayOfMonth(1).minusMonths(11), now, Grouping.MONTH)
                Period.CUSTOM -> {
                    val f = LocalDate.parse(customFrom)
                    val t = LocalDate.parse(customTo)
                    val grouping = if (t.toEpochDay() - f.toEpochDay() > 62) Grouping.MONTH else Grouping.DAY
                    Stats.load(context, f, t, grouping)
                }
            }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rumo", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, "Atualizar") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.TODAY,
                    onClick = { tab = Tab.TODAY },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text(Tab.TODAY.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.STATS,
                    onClick = { tab = Tab.STATS },
                    icon = { Icon(Icons.Filled.DateRange, null) },
                    label = { Text(Tab.STATS.label) },
                )
            }
        },
    ) { padding ->
        if (loading && today == null) {
            LinearProgressIndicator(Modifier.padding(padding).fillMaxWidth())
        }
        val t = today
        val w = week
        when (tab) {
            Tab.TODAY -> if (t != null && w != null) TodayScreen(t, w, padding)
            Tab.STATS -> StatsScreen(
                period = period,
                stats = periodStats,
                contentPadding = padding,
                onPeriod = { period = it },
                onCustomRange = { f, to ->
                    customFrom = f.toString()
                    customTo = to.toString()
                    period = Period.CUSTOM
                },
            )
        }
    }
}

/** If the app crashed last time, show what happened (and let the user copy it). */
@Composable
private fun CrashReportDialog() {
    val context = LocalContext.current
    val file = remember { File(context.filesDir, RumoApplication.CRASH_FILE) }
    var text by remember { mutableStateOf(if (file.exists()) runCatching { file.readText() }.getOrNull() else null) }
    val clipboard = LocalClipboardManager.current
    val report = text ?: return
    AlertDialog(
        onDismissRequest = { file.delete(); text = null },
        title = { Text("O Rumo fechou inesperadamente") },
        text = { Text(report.take(4000), modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(report)); file.delete(); text = null }) {
                Text("Copiar e fechar")
            }
        },
        dismissButton = { TextButton(onClick = { file.delete(); text = null }) { Text("Fechar") } },
    )
}
