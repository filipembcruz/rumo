package io.github.filipembcruz.rumo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.rumo.data.Bucket

/**
 * Simple bar chart. The current day/month is highlighted, and a dashed line marks
 * the average of the bars that have data.
 */
@Composable
fun BarChart(buckets: List<Bucket>, modifier: Modifier = Modifier) {
    if (buckets.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val avgColor = MaterialTheme.colorScheme.secondary
    val maxMs = (buckets.maxOf { it.ms }).coerceAtLeast(60_000L)
    val withData = buckets.filter { it.ms > 0 }
    val avg = if (withData.isEmpty()) 0L else withData.sumOf { it.ms } / withData.size

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val n = buckets.size
            val slot = size.width / n
            val barW = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            buckets.forEachIndexed { i, b ->
                val h = (b.ms.toFloat() / maxMs) * size.height
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(
                    color = if (b.isCurrent) primary else muted,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h.coerceAtLeast(if (b.ms > 0) 2f else 0f)),
                    cornerRadius = CornerRadius(barW / 4, barW / 4),
                )
            }
            if (avg > 0) {
                val y = size.height - (avg.toFloat() / maxMs) * size.height
                drawLine(
                    color = avgColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        // Show at most ~12 labels so they never overlap.
        val step = ((buckets.size + 11) / 12).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            buckets.forEachIndexed { i, b ->
                Text(
                    text = if (i % step == 0) b.label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (avg > 0) {
            Text(
                "– – média ${Fmt.duration(avg)}",
                style = MaterialTheme.typography.labelSmall,
                color = avgColor,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
