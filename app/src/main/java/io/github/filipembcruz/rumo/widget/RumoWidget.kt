package io.github.filipembcruz.rumo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.filipembcruz.rumo.MainActivity
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.data.Repo
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

private val Violet = Color(0xFF6B4EFF)
private val Coral = Color(0xFFFF6B6B)
private val Amber = Color(0xFFFFB238)
private val Mint = Color(0xFF22C99A)

private val Bg = androidx.glance.color.ColorProvider(day = Color(0xF7FFFFFF), night = Color(0xF01B1931))
private val Card = androidx.glance.color.ColorProvider(day = Color(0xFFF3F1FC), night = Color(0xFF26223F))
private val TextMain = androidx.glance.color.ColorProvider(day = Color(0xFF1B1734), night = Color(0xFFF3F1FF))
private val TextMuted = androidx.glance.color.ColorProvider(day = Color(0xFF7B7894), night = Color(0xFFA19DC0))
private val Track = androidx.glance.color.ColorProvider(day = Color(0xFFE9E5F8), night = Color(0xFF332E52))

data class WidgetRow(val title: String, val ms: Long, val icon: Bitmap?, val color: Color, val fraction: Float)
data class WidgetLimit(val name: String, val used: Long, val limit: Long)

data class WidgetData(
    val s: Strings,
    val total: Long,
    val goal: Long,
    val ring: Bitmap,
    val apps: List<WidgetRow>,
    val sites: List<WidgetRow>,
    val limits: List<WidgetLimit>,
    val streak: Int,
) {
    companion object {
        fun load(context: Context): WidgetData {
            val prefs = Prefs.get(context)
            val s = Strings(prefs.isPt)
            runCatching { Collector.sync(context, full = false) }
            val m = Repo.today(context)
            val pm = context.packageManager
            val minMs = prefs.minListMinutes * 60_000L
            val apps = m.apps.filter { it.ms >= minMs }.take(4)
            val maxA = apps.firstOrNull()?.ms ?: 1L
            val sites = m.sites.filter { it.ms >= minMs }.take(3)
            val maxS = sites.firstOrNull()?.ms ?: 1L
            return WidgetData(
                s = s, total = m.total, goal = m.goalMs,
                ring = ring(if (m.goalMs > 0) m.total.toFloat() / m.goalMs else 0f, 240),
                apps = apps.map { a ->
                    WidgetRow(AppNames.label(context, a.pkg), a.ms,
                        runCatching { pm.getApplicationIcon(a.pkg).toBitmap(72, 72) }.getOrNull(),
                        Color(a.cat.color), a.ms.toFloat() / maxA)
                },
                sites = sites.map { st -> WidgetRow(st.domain.removePrefix("www."), st.ms, null, Color(st.cat.color), st.ms.toFloat() / maxS) },
                limits = m.limits.sortedByDescending { it.fraction }.take(3).map { ls ->
                    val name = when (ls.limit.kind) {
                        TargetKind.APP -> AppNames.label(context, ls.limit.target)
                        TargetKind.SITE -> ls.limit.target
                        TargetKind.CATEGORY -> s.cat(Cat.of(ls.limit.target))
                    }
                    WidgetLimit(name, ls.usedMs, ls.limit.minutes * 60_000L)
                },
                streak = m.streak,
            )
        }

        /** Gradient progress ring drawn into a bitmap (Glance can't draw arcs itself). */
        fun ring(fraction: Float, px: Int): Bitmap {
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val cv = android.graphics.Canvas(bmp)
            val stroke = px * 0.12f
            val r = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
            p.color = 0x2E6B4EFF
            cv.drawArc(r, 0f, 360f, false, p)
            val main = min(fraction, 1f)
            if (main > 0f) {
                p.shader = SweepGradient(px / 2f, px / 2f, intArrayOf(0xFF8B6CFF.toInt(), 0xFFB8EA4A.toInt(), 0xFF8B6CFF.toInt()), null)
                cv.save(); cv.rotate(-90f, px / 2f, px / 2f)
                cv.drawArc(r, 0f, 360f * main, false, p)
                cv.restore()
                p.shader = null
            }
            if (fraction > 1f) {
                p.color = 0xFFFF6B6B.toInt()
                cv.drawArc(r, -90f, 360f * min(fraction - 1f, 1f), false, p)
            }
            return bmp
        }
    }
}

class RumoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { runCatching { WidgetData.load(context) }.getOrNull() }
        provideContent { WidgetContent(data) }
    }

    companion object {
        val SMALL = DpSize(120.dp, 120.dp)
        val MEDIUM = DpSize(250.dp, 120.dp)
        val LARGE = DpSize(250.dp, 260.dp)

        suspend fun refresh(context: Context) {
            runCatching { RumoWidget().updateAll(context) }
        }
    }
}

class RumoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RumoWidget()
}

private fun style(size: Int, color: ColorProvider = TextMain, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

@Composable
private fun WidgetContent(d: WidgetData?) {
    val size = LocalSize.current
    Box(
        GlanceModifier.fillMaxSize().cornerRadius(28.dp).background(Bg).padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (d == null) {
            Text("Rumo", style = style(16, bold = true))
        } else if (size.width >= 250.dp && size.height >= 240.dp) {
            Large(d)
        } else if (size.width >= 250.dp) {
            Medium(d)
        } else {
            Small(d)
        }
    }
}

@Composable
private fun RingBlock(d: WidgetData, ringDp: Int) {
    val pct = if (d.goal > 0) (d.total * 100 / d.goal).toInt() else 0
    Box(GlanceModifier.size(ringDp.dp), contentAlignment = Alignment.Center) {
        Image(ImageProvider(d.ring), contentDescription = null, modifier = GlanceModifier.size(ringDp.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", style = style(if (ringDp > 90) 20 else 15, bold = true))
            Text(d.s.ofGoal, style = style(10, TextMuted))
        }
    }
}

@Composable
private fun Small(d: WidgetData) {
    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        RingBlock(d, 76)
        Spacer(GlanceModifier.height(6.dp))
        Text(Fmt.dur(d.total), style = style(18, bold = true))
        Text(d.s.today, style = style(11, TextMuted))
    }
}

@Composable
private fun Header(d: WidgetData) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RingBlock(d, 64)
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(d.s.screenTimeToday, style = style(12, TextMuted))
            Text(Fmt.dur(d.total), style = style(26, bold = true))
            val left = d.goal - d.total
            Text(
                if (left >= 0) d.s.leftOfGoal(Fmt.dur(left)) else d.s.overGoal(Fmt.dur(-left)),
                style = style(11, ColorProvider(if (left >= 0) Mint else Coral)),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Medium(d: WidgetData) {
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RingBlock(d, 72)
            Spacer(GlanceModifier.height(4.dp))
            Text(Fmt.dur(d.total), style = style(16, bold = true))
        }
        Spacer(GlanceModifier.width(14.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (d.apps.isEmpty()) Text(d.s.noAppsYet, style = style(12, TextMuted), maxLines = 2)
            d.apps.take(3).forEach { r ->
                UsageRow(r)
                Spacer(GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun Large(d: WidgetData) {
    Column(GlanceModifier.fillMaxSize()) {
        Header(d)
        Spacer(GlanceModifier.height(10.dp))
        Section(d.s.topApps) {
            d.apps.take(3).forEach { r -> UsageRow(r); Spacer(GlanceModifier.height(6.dp)) }
        }
        if (d.sites.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            Section(d.s.topSites) {
                d.sites.take(2).forEach { r -> UsageRow(r); Spacer(GlanceModifier.height(6.dp)) }
            }
        }
        if (d.limits.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            Section(d.s.limitsToday) {
                d.limits.take(2).forEach { l -> LimitRow(l); Spacer(GlanceModifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(GlanceModifier.fillMaxWidth().cornerRadius(18.dp).background(Card).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(title, style = style(11, TextMuted, bold = true))
        Spacer(GlanceModifier.height(6.dp))
        content()
    }
}

@Composable
private fun UsageRow(r: WidgetRow) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (r.icon != null) {
            Image(ImageProvider(r.icon), contentDescription = null, modifier = GlanceModifier.size(22.dp).cornerRadius(6.dp))
        } else {
            Box(GlanceModifier.size(22.dp).cornerRadius(11.dp).background(ColorProvider(r.color)), contentAlignment = Alignment.Center) {
                Text(r.title.take(1).uppercase(), style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Row(GlanceModifier.fillMaxWidth()) {
                Text(r.title, style = style(12), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Text(Fmt.dur(r.ms), style = style(12, bold = true))
            }
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = r.fraction.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                color = ColorProvider(r.color),
                backgroundColor = Track,
            )
        }
    }
}

@Composable
private fun LimitRow(l: WidgetLimit) {
    val f = if (l.limit > 0) l.used.toFloat() / l.limit else 0f
    val col = when { f >= 1f -> Coral; f >= 0.8f -> Amber; else -> Violet }
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(l.name, style = style(12), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text("${Fmt.dur(l.used)} / ${Fmt.dur(l.limit)}", style = style(11, ColorProvider(col), bold = true))
        }
        Spacer(GlanceModifier.height(3.dp))
        LinearProgressIndicator(
            progress = f.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
            color = ColorProvider(col),
            backgroundColor = Track,
        )
    }
}
