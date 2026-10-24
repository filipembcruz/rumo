package io.github.filipembcruz.rumo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import io.github.filipembcruz.rumo.MainActivity
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Db
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.service.BlockReason
import io.github.filipembcruz.rumo.ui.components.GhostButton
import io.github.filipembcruz.rumo.ui.components.Mood
import io.github.filipembcruz.rumo.ui.components.MoodOrb
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.theme.RumoTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/** Full-screen "time's up" / "focus is on" / "take a breath" screen shown over a blocked app. */
class InterventionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun render(i: Intent) {
        val reason = runCatching { BlockReason.valueOf(i.getStringExtra(EXTRA_REASON) ?: "") }.getOrDefault(BlockReason.LIMIT)
        val kind = TargetKind.of(i.getStringExtra(EXTRA_KIND) ?: "app")
        val target = i.getStringExtra(EXTRA_TARGET) ?: ""
        val pkg = i.getStringExtra(EXTRA_PKG) ?: target
        val used = i.getLongExtra(EXTRA_USED, 0)
        val limitMin = i.getIntExtra(EXTRA_LIMIT, 0)
        val schedule = i.getStringExtra(EXTRA_SCHEDULE) ?: ""
        val prefs = Prefs.get(this)
        val strings = Strings(prefs.isPt)
        val opensToday = runCatching {
            val d = LocalDate.now().toString()
            Db.get(this).appDaily(pkg, d, d)[d]?.second ?: 0
        }.getOrDefault(0)
        val goal = runCatching { Db.get(this).goals().firstOrNull()?.title }.getOrNull()
        setContent {
            CompositionLocalProvider(LocalStrings provides strings, LocalLocale provides if (prefs.isPt) Locale("pt", "BR") else Locale.ENGLISH) {
                RumoTheme("dark") {
                    InterventionScreen(
                        reason, kind, target, pkg, used, limitMin, schedule, opensToday, goal, prefs.strict, prefs.pauseSeconds, prefs.focusUntil,
                        onHome = { goHome() },
                        onContinue = {
                            prefs.grantPass("pause:$pkg", System.currentTimeMillis() + 10 * 60_000L)
                            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            finish()
                        },
                        onExtra = {
                            prefs.grantPass("extra:${kind.id}:$target", System.currentTimeMillis() + 5 * 60_000L)
                            prefs.markOnce("extra_used:${kind.id}:$target")
                            finish()
                        },
                        canExtra = !prefs.strict && reason == BlockReason.LIMIT && prefs.passUntil("extra:${kind.id}:$target") == 0L,
                        onOpenRumo = {
                            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("tab", "growth"))
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_REASON = "reason"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TARGET = "target"
        const val EXTRA_PKG = "pkg"
        const val EXTRA_USED = "used"
        const val EXTRA_LIMIT = "limit"
        const val EXTRA_SCHEDULE = "schedule"
    }
}

@Composable
fun InterventionScreen(
    reason: BlockReason, kind: TargetKind, target: String, pkg: String, usedMs: Long, limitMin: Int, schedule: String,
    opensToday: Int, goal: String?, strict: Boolean, pauseSeconds: Int, focusUntil: Long,
    onHome: () -> Unit, onContinue: () -> Unit, onExtra: () -> Unit, canExtra: Boolean, onOpenRumo: () -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val name = when (kind) {
        TargetKind.APP -> remember(target) { AppNames.label(context, target) }
        TargetKind.SITE -> target
        TargetKind.CATEGORY -> s.cat(Cat.of(target))
    }
    BackHandler { onHome() }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1F6B), Color(0xFF130F33))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x557B5CFF), Color.Transparent), Offset(size.width * 0.8f, size.height * 0.15f), size.width * 0.7f),
                size.width * 0.7f, Offset(size.width * 0.8f, size.height * 0.15f))
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))
            when (reason) {
                BlockReason.PAUSE -> BreathOrb(pauseSeconds)
                BlockReason.FOCUS -> MoodOrb(Mood.FOCUS, size = 150.dp)
                BlockReason.SCHEDULE -> MoodOrb(Mood.SLEEPY, size = 150.dp)
                BlockReason.LIMIT -> MoodOrb(Mood.WORRIED, size = 150.dp)
                BlockReason.ADULT -> MoodOrb(Mood.CALM, size = 150.dp)
            }
            Spacer(Modifier.height(30.dp))
            val title = when (reason) {
                BlockReason.LIMIT -> s.ivLimitT(name)
                BlockReason.FOCUS -> s.ivFocusT
                BlockReason.SCHEDULE -> s.ivScheduleT(schedule)
                BlockReason.PAUSE -> s.ivPauseT
                BlockReason.ADULT -> s.ivAdultT
            }
            val body = when (reason) {
                BlockReason.LIMIT -> s.ivLimitB(Fmt.dur(usedMs), Fmt.dur(limitMin * 60_000L))
                BlockReason.FOCUS -> s.ivFocusB(name, Fmt.clock(focusUntil))
                BlockReason.SCHEDULE -> s.ivScheduleB(name)
                BlockReason.PAUSE -> s.ivPauseB(name, opensToday)
                BlockReason.ADULT -> s.ivAdultB(name)
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.TipsAndUpdates, null, tint = Color(0xFFC4F25C), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Text(s.ivSuggestion(goal), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (reason == BlockReason.PAUSE) {
                var left by remember { mutableIntStateOf(pauseSeconds) }
                LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
                PrimaryButton(s.ivNotNow, Modifier.fillMaxWidth(), Icons.Rounded.Home, bg = Color(0xFFC4F25C), fg = Color(0xFF1E2A05), onClick = onHome)
                Spacer(Modifier.height(8.dp))
                GhostButton(if (left > 0) s.ivWait(left) else s.ivOpenAnyway(name), Modifier.fillMaxWidth(), Color.White.copy(alpha = if (left > 0) 0.4f else 0.8f)) {
                    if (left <= 0) onContinue()
                }
            } else {
                PrimaryButton(s.ivGoHome, Modifier.fillMaxWidth(), Icons.Rounded.Home, bg = Color(0xFFC4F25C), fg = Color(0xFF1E2A05), onClick = onHome)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(s.ivOpenRumo, Modifier.weight(1f), Color.White.copy(alpha = 0.8f), onOpenRumo)
                    if (canExtra) GhostButton(s.ivFiveMore, Modifier.weight(1f), Color.White.copy(alpha = 0.6f), onExtra)
                }
            }
        }
    }
}

@Composable
private fun BreathOrb(seconds: Int) {
    val t = rememberInfiniteTransition(label = "breath")
    val scale by t.animateFloat(0.75f, 1.1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(190.dp)) {
            val r = size.minDimension / 2 * scale
            drawCircle(Brush.radialGradient(listOf(Color(0x8834DBAA), Color.Transparent), center, r * 1.2f), r * 1.2f)
            drawCircle(Color(0x3334DBAA), r)
        }
        MoodOrb(Mood.CALM, size = 110.dp)
    }
}
