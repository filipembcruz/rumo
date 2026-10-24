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
data class WidgetLimit(val name: String, val used: Long, val limit: Long, val ring: Bitmap? = null)

data class WidgetData(
    val s: Strings,
    val total: Long,
    val goal: Long,
    val ring: Bitmap,
    val apps: List<WidgetRow>,
    val sites: List<WidgetRow>,
    val limits: List<WidgetLimit>,
    val streak: Int,
    val pickups: Int = 0,
    val avg7: Long = 0,
    val focusScore: Int = 0,
    val hourly: Bitmap? = null,
    val peakHour: Int = -1,
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
                sites = sites.map { st -> WidgetRow(st.domain.removePrefix("www."), st.ms, runCatching { io.github.filipembcruz.rumo.data.SiteIcons.cached(context, st.domain) }.getOrNull(), Color(st.cat.color), st.ms.toFloat() / maxS) },
                limits = m.limits.filter { it.limit.active }.sortedByDescending { it.fraction }.take(3).map { ls ->
                    val name = when (ls.limit.kind) {
                        TargetKind.APP -> AppNames.label(context, ls.limit.target)
                        TargetKind.SITE -> ls.limit.target
                        TargetKind.CATEGORY -> s.cat(Cat.of(ls.limit.target))
                    }
                    val lim = ls.limit.todayMs
                    val f = if (lim > 0) ls.usedMs.toFloat() / lim else 0f
                    WidgetLimit(name, ls.usedMs, lim, miniRing(f, 96))
                },
                streak = m.streak,
                pickups = m.pickups,
                avg7 = m.avg7,
                focusScore = m.focusScore,
                hourly = hourBars(m.hourly, 720, 200),
                peakHour = m.hourly.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index ?: -1,
            )
        }

        /** Small single-color ring for a limit. */
        fun miniRing(fraction: Float, px: Int): Bitmap {
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val cv = android.graphics.Canvas(bmp)
            val stroke = px * 0.16f
            val r = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
            val col = when { fraction >= 1f -> 0xFFFF6B6B.toInt(); fraction >= 0.8f -> 0xFFFFB238.toInt(); else -> 0xFF8B6CFF.toInt() }
            p.color = (col and 0x00FFFFFF) or 0x33000000
            cv.drawArc(r, 0f, 360f, false, p)
            p.color = col
            if (fraction > 0f) cv.drawArc(r, -90f, 360f * min(fraction, 1f), false, p)
            return bmp
        }

        /** 24 rounded bars for today's usage by hour; the current hour is highlighted. */
        fun hourBars(hours: List<Long>, w: Int, h: Int): Bitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = android.graphics.Canvas(bmp)
            val max = (hours.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
            val now = java.time.LocalTime.now().hour
            val slot = w / 24f
            val bw = slot * 0.62f
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            for (i in 0 until 24) {
                val v = hours.getOrElse(i) { 0L } / max
                val x = i * slot + (slot - bw) / 2
                p.color = 0x226B4EFF
                cv.drawRoundRect(RectF(x, 0f, x + bw, h.toFloat()), bw / 2, bw / 2, p)
                if (v > 0f) {
                    val top = h - (h * v).coerceAtLeast(bw)
                    p.color = when {
                        i == now -> 0xFFFF7AB6.toInt()
                        i > now -> 0x556B4EFF
                        else -> 0xFF8B6CFF.toInt()
                    }
                    cv.drawRoundRect(RectF(x, top, x + bw, h.toFloat()), bw / 2, bw / 2, p)
                }
            }
            return bmp
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
        Image(
            ImageProvider(if (d.total > d.goal) io.github.filipembcruz.rumo.R.drawable.rumi_worried else io.github.filipembcruz.rumo.R.drawable.rumi_happy),
            contentDescription = null, modifier = GlanceModifier.size(52.dp),
        )
    }
}

@Composable
private fun Medium(d: WidgetData) {
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RingBlock(d, 72)
            Spacer(GlanceModifier.height(4.dp))
            Text(Fmt.dur(d.total), style = style(17, bold = true))
        }
        Spacer(GlanceModifier.width(12.dp))
        if (d.apps.isEmpty()) {
            Text(d.s.noAppsYet, style = style(12, TextMuted), maxLines = 2, modifier = GlanceModifier.defaultWeight())
        } else {
            AppTiles(d.apps.take(3), GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun Large(d: WidgetData) {
    val tall = LocalSize.current.height >= 330.dp
    Column(GlanceModifier.fillMaxSize()) {
        Header(d)
        Spacer(GlanceModifier.height(10.dp))
        Stats(d)
        Spacer(GlanceModifier.height(8.dp))
        if (d.apps.isEmpty()) {
            Text(d.s.noAppsYet, style = style(12, TextMuted), maxLines = 2)
        } else {
            AppTiles(d.apps.take(4), GlanceModifier.fillMaxWidth())
        }
        if (tall && d.hourly != null) {
            Spacer(GlanceModifier.height(8.dp))
            HourChart(d, GlanceModifier.fillMaxWidth().defaultWeight())
        }
        if (d.sites.isNotEmpty() || d.limits.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                if (d.sites.isNotEmpty()) {
                    Box(GlanceModifier.defaultWeight()) { SitesCard(d) }
                }
                if (d.sites.isNotEmpty() && d.limits.isNotEmpty()) Spacer(GlanceModifier.width(8.dp))
                if (d.limits.isNotEmpty()) {
                    Box(GlanceModifier.defaultWeight()) { LimitsCard(d) }
                }
            }
        }
    }
}

@Composable
private fun Stats(d: WidgetData) {
    val diff = if (d.avg7 > 0) ((d.total - d.avg7) * 100 / d.avg7).toInt() else 0
    Row(GlanceModifier.fillMaxWidth()) {
        StatCard(d.pickups.toString(), d.s.pickups, TextMain, GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(8.dp))
        StatCard(d.focusScore.toString(), d.s.focusScore, ColorProvider(if (d.focusScore >= 60) Mint else if (d.focusScore >= 40) Amber else Coral), GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(8.dp))
        StatCard(
            if (d.avg7 > 0) (if (diff > 0) "+$diff%" else "$diff%") else "–",
            d.s.vsAvg7,
            ColorProvider(if (diff > 0) Coral else Mint),
            GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, color: ColorProvider, modifier: GlanceModifier) {
    Column(modifier.cornerRadius(16.dp).background(Card).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(value, style = style(18, color, bold = true), maxLines = 1)
        Text(label, style = style(10, TextMuted), maxLines = 1)
    }
}

@Composable
private fun AppTiles(apps: List<WidgetRow>, modifier: GlanceModifier) {
    Row(modifier) {
        apps.forEachIndexed { i, r ->
            if (i > 0) Spacer(GlanceModifier.width(8.dp))
            Column(
                GlanceModifier.defaultWeight().cornerRadius(16.dp).background(Card).padding(horizontal = 6.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Dot(r, 30)
                Spacer(GlanceModifier.height(5.dp))
                Text(Fmt.dur(r.ms), style = style(15, bold = true), maxLines = 1)
                Text(r.title, style = style(10, TextMuted), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Dot(r: WidgetRow, sizeDp: Int) {
    if (r.icon != null) {
        Image(ImageProvider(r.icon), contentDescription = null, modifier = GlanceModifier.size(sizeDp.dp).cornerRadius((sizeDp / 4).dp))
    } else {
        Box(GlanceModifier.size(sizeDp.dp).cornerRadius((sizeDp / 2).dp).background(ColorProvider(r.color)), contentAlignment = Alignment.Center) {
            Text(r.title.take(1).uppercase(), style = TextStyle(color = ColorProvider(Color.White), fontSize = (sizeDp * 0.45f).sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun HourChart(d: WidgetData, modifier: GlanceModifier) {
    Column(modifier.cornerRadius(18.dp).background(Card).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(d.s.todayByHour, style = style(11, TextMuted, bold = true), modifier = GlanceModifier.defaultWeight())
            if (d.peakHour >= 0) Text(d.s.peakAt(d.peakHour), style = style(11, ColorProvider(Violet), bold = true))
        }
        Spacer(GlanceModifier.height(6.dp))
        Image(
            ImageProvider(d.hourly!!), contentDescription = null,
            contentScale = androidx.glance.layout.ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            listOf("0h", "6h", "12h", "18h").forEach { h ->
                Text(h, style = style(9, TextMuted), modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun SitesCard(d: WidgetData) {
    Column(GlanceModifier.fillMaxWidth().cornerRadius(18.dp).background(Card).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(d.s.sitesShort, style = style(11, TextMuted, bold = true))
        d.sites.take(2).forEach { r ->
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Dot(r, 22)
                Spacer(GlanceModifier.width(6.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(Fmt.dur(r.ms), style = style(14, bold = true), maxLines = 1)
                    Text(r.title, style = style(10, TextMuted), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun LimitsCard(d: WidgetData) {
    Column(GlanceModifier.fillMaxWidth().cornerRadius(18.dp).background(Card).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(d.s.limitsShort, style = style(11, TextMuted, bold = true))
        d.limits.take(2).forEach { l ->
            val f = if (l.limit > 0) l.used.toFloat() / l.limit else 0f
            val col = when { f >= 1f -> Coral; f >= 0.8f -> Amber; else -> TextMainColor }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (l.ring != null) Image(ImageProvider(l.ring), contentDescription = null, modifier = GlanceModifier.size(22.dp))
                Spacer(GlanceModifier.width(6.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Row {
                        Text(Fmt.dur(l.used), style = style(14, if (col == TextMainColor) TextMain else ColorProvider(col), bold = true), maxLines = 1)
                        Text(" / ${Fmt.dur(l.limit)}", style = style(10, TextMuted), maxLines = 1)
                    }
                    Text(l.name, style = style(10, TextMuted), maxLines = 1)
                }
            }
        }
    }
}

private val TextMainColor = Color.Unspecified
