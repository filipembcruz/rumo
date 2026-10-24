@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.BuildConfig
import io.github.filipembcruz.rumo.core.Actions
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Backup
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.NavRow
import io.github.filipembcruz.rumo.ui.components.RCard
import io.github.filipembcruz.rumo.ui.components.Segmented
import io.github.filipembcruz.rumo.ui.components.ToggleRow
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsRoute() {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs.get(context) }
    var v by remember { mutableIntStateOf(0) } // local recomposition trigger after writes
    fun set(block: () -> Unit) { block(); v++ }

    var confirmWipe by remember { mutableStateOf(false) }
    val usageOk = remember(nav.tick) { Perms.usage(context) }
    val a11yOk = remember(nav.tick) { Perms.accessibility(context) }
    val overlayOk = remember(nav.tick) { Perms.overlay(context) }
    val notifOk = remember(nav.tick, v) { Perms.notifications(context) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.writeTo(context, uri) }.isSuccess }
            toast(if (ok) s.backupSaved else s.backupFailed)
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.importFrom(context, uri) }.getOrDefault(false) }
            toast(if (ok) s.backupRestored else s.backupInvalid)
            nav.refresh()
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { v++ }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).bounceClick(onClick = nav.back).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.back, tint = c.text)
                }
                Spacer(Modifier.width(14.dp))
                Text(s.settings, style = MaterialTheme.typography.headlineMedium, color = c.text)
            }
        }
        item {
            if (v < 0) return@item
            RCard {
                var name by remember { mutableStateOf(prefs.name) }
                OutlinedTextField(
                    name, { name = it; prefs.name = it }, Modifier.fillMaxWidth(), label = { Text(s.yourName) }, singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.primary, unfocusedBorderColor = c.outline,
                        focusedTextColor = c.text, unfocusedTextColor = c.text),
                )
                Spacer(Modifier.height(16.dp))
                Label(Icons.Rounded.Translate, s.language)
                Segmented(listOf("English", "Português"), if (prefs.language == "pt") 1 else 0, { set { prefs.language = if (it == 1) "pt" else "en" } })
                Spacer(Modifier.height(16.dp))
                Label(Icons.Rounded.DarkMode, s.theme)
                val themes = listOf("system", "light", "dark")
                Segmented(listOf(s.themeSystem, s.themeLight, s.themeDark), themes.indexOf(prefs.theme).coerceAtLeast(0), { set { prefs.theme = themes[it] } })
                Spacer(Modifier.height(16.dp))
                var goal by remember { mutableFloatStateOf(prefs.dailyGoalMin.toFloat()) }
                Label(Icons.Rounded.Flag, s.dailyGoalValue(Fmt.dur(goal.toLong() * 60_000L)))
                Slider(goal, { goal = (it / 15f).toInt() * 15f }, valueRange = 30f..600f, onValueChangeFinished = { prefs.dailyGoalMin = goal.toInt() },
                    colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceAlt))
            }
        }
        item { Header(s.permissions) }
        item {
            RCard {
                PermRow(Icons.Rounded.QueryStats, s.permUsage, s.permUsageSub, usageOk) { Perms.openUsageSettings(context) }
                PermRow(Icons.Rounded.AccessibilityNew, s.permSites, s.permSitesSub, a11yOk) { nav.setupBlocking() }
                PermRow(Icons.Rounded.Layers, s.permOverlay, s.permOverlaySub, overlayOk || a11yOk) { Perms.openOverlaySettings(context) }
                PermRow(Icons.Rounded.Notifications, s.permNotif, s.permNotifSub, notifOk) {
                    if (Build.VERSION.SDK_INT >= 33 && !notifOk) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else Perms.openNotificationSettings(context)
                }
            }
        }
        item { Header(s.notifications) }
        item {
            if (v < 0) return@item
            RCard {
                ToggleRow(Icons.Rounded.Timer, s.nLimits, s.nLimitsSub, prefs.limitAlerts, { set { prefs.limitAlerts = it } }, c.coral)
                ToggleRow(Icons.Rounded.SelfImprovement, s.nNudges, s.nNudgesSub(prefs.nudgeAfterMin), prefs.nudges, { set { prefs.nudges = it } }, c.mint)
                if (prefs.nudges) {
                    var nm by remember { mutableFloatStateOf(prefs.nudgeAfterMin.toFloat()) }
                    Slider(nm, { nm = (it / 5f).toInt() * 5f }, valueRange = 10f..90f, onValueChangeFinished = { set { prefs.nudgeAfterMin = nm.toInt() } },
                        colors = SliderDefaults.colors(thumbColor = c.mint, activeTrackColor = c.mint, inactiveTrackColor = c.surfaceAlt))
                }
                ToggleRow(Icons.Rounded.Summarize, s.nDaily, s.nDailySub(prefs.summaryHour), prefs.dailySummary, { set { prefs.dailySummary = it } }, c.primary)
                if (prefs.dailySummary) {
                    var h by remember { mutableFloatStateOf(prefs.summaryHour.toFloat()) }
                    Slider(h, { h = it.toInt().toFloat() }, valueRange = 17f..23f, steps = 5, onValueChangeFinished = { set { prefs.summaryHour = h.toInt() } },
                        colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceAlt))
                }
                ToggleRow(Icons.Rounded.NotificationsActive, s.nWeekly, s.nWeeklySub, prefs.weeklyReport, { set { prefs.weeklyReport = it } }, c.sky)
                ToggleRow(Icons.Rounded.Bedtime, s.nBedtime, s.nBedtimeSub(Fmt.clockMin(prefs.bedtimeMin)), prefs.bedtimeReminder, { set { prefs.bedtimeReminder = it } }, c.primaryDeep)
                if (prefs.bedtimeReminder) {
                    var b by remember { mutableFloatStateOf(prefs.bedtimeMin.toFloat()) }
                    Slider(b, { b = (it / 15f).toInt() * 15f }, valueRange = (20 * 60f)..(25 * 60f), onValueChangeFinished = { set { prefs.bedtimeMin = b.toInt() % (24 * 60) } },
                        colors = SliderDefaults.colors(thumbColor = c.primaryDeep, activeTrackColor = c.primaryDeep, inactiveTrackColor = c.surfaceAlt))
                }
                ToggleRow(Icons.Rounded.Visibility, s.nMonitor, s.nMonitorSub, prefs.monitoringNotification, { set { prefs.monitoringNotification = it } }, c.muted)
            }
        }
        item { Header(s.blocking) }
        item {
            if (v < 0) return@item
            RCard {
                ToggleRow(Icons.Rounded.Lock, s.strictMode, s.strictModeSub, prefs.strict, { set { prefs.strict = it } }, c.coral)
                ToggleRow(Icons.Rounded.Block, s.blockAdult, s.blockAdultSub, prefs.blockAdult, {
                    set { prefs.blockAdult = it }
                    if (it && !a11yOk) nav.setupBlocking()
                }, c.coral)
                var ps by remember { mutableFloatStateOf(prefs.pauseSeconds.toFloat()) }
                Spacer(Modifier.height(6.dp))
                Label(Icons.Rounded.Schedule, s.pauseLength(ps.toInt()))
                Slider(ps, { ps = it.toInt().toFloat() }, valueRange = 3f..30f, onValueChangeFinished = { prefs.pauseSeconds = ps.toInt() },
                    colors = SliderDefaults.colors(thumbColor = c.mint, activeTrackColor = c.mint, inactiveTrackColor = c.surfaceAlt))
            }
        }
        item { Header(s.lists) }
        item {
            if (v < 0) return@item
            RCard {
                var mm by remember { mutableFloatStateOf(prefs.minListMinutes.toFloat()) }
                Label(Icons.Rounded.FilterList, s.minListValue(mm.toInt()))
                Slider(mm, { mm = it.toInt().toFloat() }, valueRange = 0f..15f, onValueChangeFinished = { set { prefs.minListMinutes = mm.toInt() } },
                    colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceAlt))
            }
        }
        item { Header(s.backupData) }
        item {
            RCard {
                Text(s.backupExplain, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Spacer(Modifier.height(6.dp))
                NavRow(Icons.Rounded.FileDownload, s.saveBackup, s.saveBackupSub) { saveLauncher.launch(Backup.fileName()) }
                NavRow(Icons.Rounded.Share, s.shareBackup, s.shareBackupSub) {
                    scope.launch {
                        val intent = withContext(Dispatchers.IO) { runCatching { Backup.shareIntent(context) }.getOrNull() }
                        if (intent != null) context.startActivity(Intent.createChooser(intent, s.shareBackup))
                    }
                }
                NavRow(Icons.Rounded.FileUpload, s.restoreBackup, s.restoreBackupSub) { openLauncher.launch(arrayOf("application/json", "*/*")) }
                NavRow(Icons.Rounded.DeleteForever, s.deleteAll, s.deleteAllSub, iconTint = c.coral) { confirmWipe = true }
            }
        }
        item { Header(s.about) }
        item {
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Shield, c.mint.copy(alpha = 0.14f), c.mint)
                    Spacer(Modifier.width(14.dp))
                    Text(s.privacyNote, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                NavRow(Icons.Rounded.Code, s.sourceCode, "github.com/filipembcruz/rumo", value = null) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/filipembcruz/rumo"))) }
                }
                Text("Rumo ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall, color = c.muted,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
    }

    if (confirmWipe) ConfirmDialog(s.deleteAllConfirmTitle, s.deleteAllConfirmBody, s.delete,
        onConfirm = {
            confirmWipe = false
            scope.launch { withContext(Dispatchers.IO) { Actions.wipe(context) }; nav.refresh(); toast(s.deleted) }
        },
        onDismiss = { confirmWipe = false })
}

@Composable
private fun Header(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = R2.colors.muted, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
}

@Composable
private fun Label(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = R2.colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = R2.colors.text)
    }
}

@Composable
private fun PermRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, ok: Boolean, onClick: () -> Unit) {
    val c = R2.colors
    val s = LocalStrings.current
    NavRow(icon, title, sub, iconTint = if (ok) c.mint else c.amber, trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null, tint = if (ok) c.mint else c.amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (ok) s.on else s.off, style = MaterialTheme.typography.labelMedium, color = if (ok) c.mint else c.amber)
        }
    }, onClick = onClick)
}

