package io.github.filipembcruz.rumo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.rumo.ui.Fmt
import io.github.filipembcruz.rumo.ui.theme.Inter
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlin.math.max
import kotlin.math.min

private fun DrawScope.hatchedPill(left: Float, top: Float, right: Float, bottom: Float, track: Color, stripe: Color) {
    if (bottom - top <= 0f) return
    val r = (right - left) / 2f
    val path = Path().apply { addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(r, r))) }
    clipPath(path) {
        drawRect(track, Offset(left, top), Size(right - left, bottom - top))
        val step = 7.dp.toPx()
        val h = bottom - top
        var x = left - h
        while (x < right) {
            drawLine(stripe, Offset(x, bottom), Offset(x + h, top), strokeWidth = 1.6.dp.toPx())
            x += step
        }
    }
}

private fun DrawScope.tooltip(tm: TextMeasurer, text: String, cx: Float, bottomY: Float, bg: Color, fg: Color) {
    val layout = tm.measure(text, TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = fg))
    val padH = 10.dp.toPx()
    val padV = 6.dp.toPx()
    val w = layout.size.width + padH * 2
    val h = layout.size.height + padV * 2
    val left = (cx - w / 2).coerceIn(0f, size.width - w)
    val top = bottomY - h - 6.dp.toPx()
    drawRoundRect(bg, Offset(left, top), Size(w, h), CornerRadius(h / 2, h / 2))
    val tri = Path().apply {
        moveTo(cx - 5.dp.toPx(), top + h - 0.5f); lineTo(cx + 5.dp.toPx(), top + h - 0.5f); lineTo(cx, top + h + 5.dp.toPx()); close()
    }
    drawPath(tri, bg)
    drawText(layout, topLeft = Offset(left + padH, top + padV))
}

/**
 * Animated bar chart: pill bars over hatched tracks, tap to inspect, optional goal line.
 */
@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    highlight: Int = -1,
    goal: Long? = null,
    color: Color = R2.colors.primary,
    format: (Long) -> String = { Fmt.dur(it) },
) {
    if (values.isEmpty()) return
    val c = R2.colors
    val tm = rememberTextMeasurer()
    val anim = remember(values) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(values) { anim.animateTo(1f, tween(1200, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) }
    val initial = if (highlight in values.indices && values[highlight] > 0) highlight else values.indices.maxByOrNull { values[it] } ?: -1
    var sel by remember(values) { mutableIntStateOf(if ((values.maxOrNull() ?: 0L) > 0) initial else -1) }
    val labelStyle = TextStyle(fontFamily = Inter, fontSize = 10.sp, color = c.muted)
    val labelSel = labelStyle.copy(color = c.text, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(values) {
            detectTapGestures { o ->
                val slot = size.width / values.size.toFloat()
                val i = (o.x / slot).toInt().coerceIn(0, values.lastIndex)
                sel = if (sel == i) -1 else i
            }
        }
    ) {
        val n = values.size
        val labelH = 18.dp.toPx()
        val topPad = 34.dp.toPx()
        val chartH = size.height - labelH - topPad
        val slot = size.width / n
        val barW = min(slot * 0.58f, 24.dp.toPx())
        val maxV = max(values.maxOrNull() ?: 1L, goal ?: 0L).coerceAtLeast(60_000L).toFloat()
        val base = topPad + chartH
        for (i in 0 until n) {
            val cx = slot * i + slot / 2
            val left = cx - barW / 2
            val right = cx + barW / 2
            hatchedPill(left, topPad, right, base, c.surfaceAlt, c.hatch)
            val stagger = 0.5f / n
            val p = ((anim.value * (1f + 0.5f) - i * stagger) / 1f).coerceIn(0f, 1f)
            val eased = 1f - (1f - p) * (1f - p) * (1f - p)
            val h = (values[i] / maxV) * chartH * eased
            if (values[i] > 0 && eased > 0f) {
                val top = base - max(h, barW * 0.9f * eased)
                val brush = if (i == sel || (sel < 0 && i == highlight))
                    Brush.verticalGradient(listOf(color, color.copy(alpha = 0.85f)), top, base)
                else Brush.verticalGradient(listOf(color.copy(alpha = 0.42f), color.copy(alpha = 0.30f)), top, base)
                drawRoundRect(brush, Offset(left, top), Size(barW, base - top), CornerRadius(barW / 2, barW / 2))
            }
            val label = labels.getOrNull(i).orEmpty()
            if (label.isNotEmpty()) {
                val layout = tm.measure(label, if (i == sel || i == highlight) labelSel else labelStyle)
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, base + 5.dp.toPx()))
            }
        }
        if (goal != null && goal > 0) {
            val y = base - (goal / maxV) * chartH
            drawLine(
                c.lime.copy(alpha = if (c.dark) 0.9f else 1f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)), cap = StrokeCap.Round,
            )
        }
        if (sel in 0 until n && values[sel] > 0 && anim.value > 0.95f) {
            val cx = slot * sel + slot / 2
            val h = max((values[sel] / maxV) * chartH, barW * 0.9f)
            tooltip(tm, format(values[sel]), cx, base - h, c.navy, c.onNavy)
        }
    }
}

/** Animated donut. [slices] are (fraction, color). */
@Composable
fun Donut(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier, size: Dp = 168.dp, thickness: Dp = 20.dp,
          center: @Composable () -> Unit = {}) {
    val c = R2.colors
    val anim = remember(slices) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(slices) { anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(c.surfaceAlt, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val total = slices.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
            var start = -90f
            val gap = if (slices.size > 1) 4f else 0f
            for ((f, col) in slices) {
                val sweep = (f / total) * 360f * anim.value
                val drawn = (sweep - gap).coerceAtLeast(0.5f)
                drawArc(col, start + gap / 2, drawn, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                start += sweep
            }
        }
        center()
    }
}

/** Smooth area chart with gradient fill, animated reveal and tap-to-inspect. */
@Composable
fun AreaChart(values: List<Long>, labels: List<String>, modifier: Modifier = Modifier, height: Dp = 170.dp,
              color: Color = R2.colors.primary, format: (Long) -> String = { Fmt.dur(it) }) {
    if (values.size < 2) return
    val c = R2.colors
    val tm = rememberTextMeasurer()
    val anim = remember(values) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(values) { anim.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    var sel by remember(values) { mutableIntStateOf(values.lastIndex) }
    val labelStyle = TextStyle(fontFamily = Inter, fontSize = 10.sp, color = c.muted)
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(values) {
            detectTapGestures { o ->
                val step = size.width / (values.size - 1).toFloat()
                sel = (o.x / step + 0.5f).toInt().coerceIn(0, values.lastIndex)
            }
        }
    ) {
        val labelH = 18.dp.toPx()
        val topPad = 34.dp.toPx()
        val chartH = size.height - labelH - topPad
        val base = topPad + chartH
        val maxV = (values.maxOrNull() ?: 1L).coerceAtLeast(60_000L).toFloat()
        val step = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * step, base - (v / maxV) * chartH) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]; val p1 = pts[i]
                val mx = (p0.x + p1.x) / 2
                cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, base); lineTo(pts.first().x, base); close()
        }
        for (k in 0..3) {
            val y = topPad + chartH * k / 3f
            drawLine(c.outline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        }
        clipRect(right = size.width * anim.value) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.0f)), topPad, base))
            drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        labels.forEachIndexed { i, l ->
            if (l.isNotEmpty()) {
                val layout = tm.measure(l, labelStyle)
                val x = (i * step - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, base + 5.dp.toPx()))
            }
        }
        if (sel in values.indices && anim.value > 0.95f) {
            val p = pts[sel]
            drawLine(color.copy(alpha = 0.35f), Offset(p.x, topPad), Offset(p.x, base), 1.5.dp.toPx())
            drawCircle(c.surface, 7.dp.toPx(), p)
            drawCircle(color, 7.dp.toPx(), p, style = Stroke(3.dp.toPx()))
            tooltip(tm, format(values[sel]), p.x, p.y - 8.dp.toPx(), c.navy, c.onNavy)
        }
    }
}

/** Weekday × hour heatmap (values in minutes). */
@Composable
fun Heatmap(rows: List<List<Float>>, dayLabels: List<String>, modifier: Modifier = Modifier, color: Color = R2.colors.primary) {
    val c = R2.colors
    val tm = rememberTextMeasurer()
    val anim = remember(rows) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(rows) { anim.animateTo(1f, tween(900)) }
    val maxV = rows.maxOfOrNull { r -> r.maxOrNull() ?: 0f }?.coerceAtLeast(1f) ?: 1f
    val labelStyle = TextStyle(fontFamily = Inter, fontSize = 10.sp, color = c.muted)
    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        val labelW = 22.dp.toPx()
        val bottomH = 16.dp.toPx()
        val gap = 2.5.dp.toPx()
        val cw = (size.width - labelW) / 24f
        val ch = (size.height - bottomH) / 7f
        rows.forEachIndexed { r, row ->
            val l = tm.measure(dayLabels.getOrNull(r).orEmpty(), labelStyle)
            drawText(l, topLeft = Offset(0f, r * ch + (ch - l.size.height) / 2))
            row.forEachIndexed { h, v ->
                val a = if (v <= 0f) 0f else (0.14f + 0.86f * (v / maxV)) * anim.value
                val col = if (v <= 0f) c.surfaceAlt else color.copy(alpha = a)
                drawRoundRect(col, Offset(labelW + h * cw + gap / 2, r * ch + gap / 2), Size(cw - gap, ch - gap), CornerRadius(4.dp.toPx()))
            }
        }
        for (h in listOf(0, 6, 12, 18, 23)) {
            val l = tm.measure("%02d".format(h), labelStyle)
            drawText(l, topLeft = Offset(labelW + h * cw + (cw - l.size.width) / 2, size.height - l.size.height))
        }
    }
}

/** Ring progress with gradient; overflow past 100% is drawn in coral. */
@Composable
fun Ring(fraction: Float, modifier: Modifier = Modifier, size: Dp = 150.dp, thickness: Dp = 14.dp,
         track: Color = Color.White.copy(alpha = 0.18f), colors: List<Color> = listOf(R2.colors.lime, R2.colors.lime),
         over: Color = R2.colors.coral, content: @Composable () -> Unit = {}) {
    val target = fraction.coerceIn(0f, 2f)
    val anim = remember { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else target) }
    LaunchedEffect(target) { anim.animateTo(target, tween(1400, easing = FastOutSlowInEasing)) }
    val f = anim.value
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = thickness.toPx()
            val inset = s / 2
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(s))
            val main = min(f, 1f)
            if (main > 0f) drawArc(
                Brush.sweepGradient(colors + colors.first()), -90f, 360f * main, false, Offset(inset, inset), arc,
                style = Stroke(s, cap = StrokeCap.Round),
            )
            if (f > 1f) drawArc(over, -90f, 360f * (f - 1f), false, Offset(inset, inset), arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Horizontal stacked bar of (fraction, color). */
@Composable
fun StackedBar(parts: List<Pair<Float, Color>>, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val total = parts.sumOf { it.first.toDouble() }.toFloat()
    val anim = remember(parts) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(parts) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (total <= 0f) {
            Box(Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(50)).background(R2.colors.surfaceAlt))
        } else parts.filter { it.first > 0f }.forEach { (f, col) ->
            Box(Modifier.weight(f / total).fillMaxHeight().clip(RoundedCornerShape(50)).background(col.copy(alpha = 0.3f + 0.7f * anim.value)))
        }
    }
}

/** Two series per slot (e.g. productive vs distracting). */
@Composable
fun DualBars(a: List<Long>, b: List<Long>, labels: List<String>, colorA: Color, colorB: Color, modifier: Modifier = Modifier, height: Dp = 170.dp) {
    val c = R2.colors
    val tm = rememberTextMeasurer()
    val anim = remember(a, b) { Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else 1f) }
    LaunchedEffect(a, b) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val labelStyle = TextStyle(fontFamily = Inter, fontSize = 10.sp, color = c.muted)
    Canvas(modifier.fillMaxWidth().height(height)) {
        val n = max(a.size, b.size)
        if (n == 0) return@Canvas
        val labelH = 18.dp.toPx()
        val chartH = size.height - labelH - 6.dp.toPx()
        val slot = size.width / n
        val w = min(slot * 0.26f, 12.dp.toPx())
        val maxV = max(a.maxOrNull() ?: 0L, b.maxOrNull() ?: 0L).coerceAtLeast(60_000L).toFloat()
        for (i in 0 until n) {
            val cx = slot * i + slot / 2
            listOf(Triple(a.getOrElse(i) { 0L }, colorA, cx - w * 0.65f), Triple(b.getOrElse(i) { 0L }, colorB, cx + w * 0.65f)).forEach { (v, col, x) ->
                hatchedPill(x - w / 2, 0f, x + w / 2, chartH, c.surfaceAlt, c.hatch)
                if (v > 0) {
                    val h = max((v / maxV) * chartH * anim.value, w)
                    drawRoundRect(col, Offset(x - w / 2, chartH - h), Size(w, h), CornerRadius(w / 2, w / 2))
                }
            }
            val l = tm.measure(labels.getOrNull(i).orEmpty(), labelStyle)
            drawText(l, topLeft = Offset(cx - l.size.width / 2f, chartH + 6.dp.toPx()))
        }
    }
}

@Composable
fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    Canvas(modifier) {
        val maxV = (values.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * step, size.height - (v / maxV) * size.height * 0.9f - 1f)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
