package io.github.filipembcruz.rumo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.R
import io.github.filipembcruz.rumo.ui.UiFlags
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Mood { HAPPY, CALM, WORRIED, FOCUS, SLEEPY, CELEBRATE }

private fun frame(m: Mood) = when (m) {
    Mood.HAPPY -> R.drawable.rumi_happy
    Mood.CALM -> R.drawable.rumi_calm
    Mood.WORRIED -> R.drawable.rumi_worried
    Mood.FOCUS -> R.drawable.rumi_focus
    Mood.SLEEPY -> R.drawable.rumi_sleepy
    Mood.CELEBRATE -> R.drawable.rumi_celebrate
}

private fun glow(m: Mood) = when (m) {
    Mood.HAPPY -> Color(0xFFFF9AD5)
    Mood.CALM -> Color(0xFFB9A8FF)
    Mood.WORRIED -> Color(0xFFFFB86B)
    Mood.FOCUS -> Color(0xFF7FB2FF)
    Mood.SLEEPY -> Color(0xFF7C6BFF)
    Mood.CELEBRATE -> Color(0xFFFFD36B)
}

/**
 * Rumi, the bubble companion: floats, breathes, blinks, sparkles and changes expression
 * with the user's day. Tap it for a little celebration.
 */
@Composable
fun MoodOrb(mood: Mood, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    if (!UiFlags.animations) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Image(painterResource(frame(mood)), null, Modifier.fillMaxSize(0.9f))
        }
        return
    }
    var tapped by remember { mutableStateOf(false) }
    val shown = if (tapped) Mood.CELEBRATE else mood
    var blinking by remember { mutableStateOf(false) }

    LaunchedEffect(shown) {
        blinking = false
        if (shown == Mood.HAPPY || shown == Mood.CALM) {
            while (true) {
                delay(Random.nextLong(2600, 5200))
                blinking = true
                delay(140)
                blinking = false
                if (Random.nextInt(4) == 0) { delay(160); blinking = true; delay(120); blinking = false }
            }
        }
    }
    LaunchedEffect(tapped) { if (tapped) { delay(1700); tapped = false } }

    val pop = remember { Animatable(1f) }
    LaunchedEffect(shown) {
        pop.snapTo(0.86f)
        pop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 320f))
    }

    val t = rememberInfiniteTransition(label = "rumi")
    val float by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(if (shown == Mood.SLEEPY) 4200 else 3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val breathe by t.animateFloat(0.975f, 1.025f, infiniteRepeatable(tween(if (shown == Mood.SLEEPY) 3600 else 2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val twinkle by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "twinkle")
    val glowColor by animateColorAsState(glow(shown), tween(600), label = "glow")
    val bounce = if (shown == Mood.CELEBRATE) 1.6f else 1f
    val boxSize = size

    Box(
        modifier.size(size).clickable(remember { MutableInteractionSource() }, indication = null) { tapped = true },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2 + float * this.size.height * 0.02f)
            val r = this.size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.42f * breathe), Color.Transparent), c, r), r, c)
            if (this.size.minDimension > 40.dp.toPx()) {
                for (i in 0 until 5) {
                    val phase = (twinkle + i / 5f) % 1f
                    val a = (sin(phase * 2 * PI).toFloat()).coerceAtLeast(0f)
                    if (a <= 0.02f) continue
                    val ang = (i * 72 + 20).toDouble() * PI / 180
                    val p = Offset(c.x + cos(ang).toFloat() * r * 0.9f, c.y + sin(ang).toFloat() * r * 0.88f)
                    val sz = r * 0.07f * (0.6f + 0.4f * a)
                    val star = Path().apply {
                        moveTo(p.x, p.y - sz); lineTo(p.x + sz * 0.28f, p.y - sz * 0.28f); lineTo(p.x + sz, p.y)
                        lineTo(p.x + sz * 0.28f, p.y + sz * 0.28f); lineTo(p.x, p.y + sz); lineTo(p.x - sz * 0.28f, p.y + sz * 0.28f)
                        lineTo(p.x - sz, p.y); lineTo(p.x - sz * 0.28f, p.y - sz * 0.28f); close()
                    }
                    drawPath(star, Color.White.copy(alpha = 0.85f * a))
                }
            }
        }
        Crossfade(
            targetState = if (blinking) R.drawable.rumi_blink else frame(shown),
            animationSpec = tween(if (blinking) 70 else 280),
            modifier = Modifier.fillMaxSize(0.86f).graphicsLayer {
                translationY = float * boxSize.toPx() * 0.035f * bounce
                scaleX = breathe * pop.value
                scaleY = (2f - breathe) * pop.value
            },
            label = "face",
        ) { res ->
            Image(painterResource(res), null, Modifier.fillMaxSize())
        }
    }
}
