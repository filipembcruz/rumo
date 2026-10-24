package io.github.filipembcruz.rumo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.UiFlags
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.delay
import java.util.Collections

/** Keys that already played their entrance this session, so scrolling back never re-animates or shifts. */
private val seen: MutableSet<String> = Collections.synchronizedSet(HashSet())

/**
 * Entrance: fade + rise, once per session and key. Only draws differently (graphicsLayer),
 * never changes layout size, so the list never jumps while scrolling.
 */
@Composable
fun Appear(index: Int, key: String, content: @Composable () -> Unit) {
    val first = remember(key) { UiFlags.animations && seen.add(key) }
    val a = remember(key) { Animatable(if (first) 0f else 1f) }
    LaunchedEffect(key) {
        if (first) {
            delay(55L * index.coerceAtMost(8))
            a.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 36.dp.toPx()
        val sc = 0.97f + 0.03f * a.value
        scaleX = sc; scaleY = sc
    }) { content() }
}

/** Counts up from 0 to [target] the first time it's shown. */
@Composable
fun rememberCountUp(target: Int, key: String): Int {
    val first = remember(key) { UiFlags.animations && seen.add("count:$key") }
    val a = remember(key) { Animatable(if (first) 0f else target.toFloat()) }
    LaunchedEffect(target) { a.animateTo(target.toFloat(), tween(if (first) 1100 else 500, easing = FastOutSlowInEasing)) }
    return a.value.toInt()
}

/** Pop-in with a springy overshoot, staggered by [index]. */
@Composable
fun PopIn(index: Int, key: String, content: @Composable () -> Unit) {
    val first = remember(key) { UiFlags.animations && seen.add(key) }
    val a = remember(key) { Animatable(if (first) 0f else 1f) }
    LaunchedEffect(key) {
        if (first) {
            delay(70L * index)
            a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
        }
    }
    Box(Modifier.graphicsLayer {
        alpha = a.value.coerceIn(0f, 1f)
        val sc = 0.5f + 0.5f * a.value
        scaleX = sc; scaleY = sc
    }) { content() }
}

/**
 * Shows the first [page] items; "Load more" reveals the next page with a short loader.
 * Rows fade in as they're revealed.
 */
@Composable
fun <T> PagedColumn(items: List<T>, listKey: String, page: Int = 10, hidden: Int = 0, row: @Composable (T) -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    var count by remember(listKey) { mutableIntStateOf(page) }
    var loading by remember(listKey) { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) { delay(450); count += page; loading = false }
    }
    Column {
        items.take(count).forEachIndexed { i, item ->
            Appear(i % page, "$listKey:$i") { row(item) }
        }
        if (items.size > count) {
            Spacer(Modifier.height(6.dp))
            AnimatedContent(loading, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "more") { busy ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                        .bounceClick(enabled = !busy) { loading = true },
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = c.primary, strokeWidth = 2.5.dp)
                    } else {
                        Text("${s.loadMore} (${items.size - count})", style = MaterialTheme.typography.labelLarge, color = c.primary)
                    }
                }
            }
        }
        if (hidden > 0 && items.size <= count) {
            Text(s.hiddenSmall(hidden), style = MaterialTheme.typography.labelSmall, color = c.muted,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

