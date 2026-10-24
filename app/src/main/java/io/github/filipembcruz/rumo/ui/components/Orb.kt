package io.github.filipembcruz.rumo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class Mood { HAPPY, CALM, WORRIED, FOCUS, SLEEPY, CELEBRATE }

private fun moodColors(m: Mood): Triple<Color, Color, Color> = when (m) {
    Mood.HAPPY -> Triple(Color(0xFF8B6CFF), Color(0xFFC4F25C), Color(0xFF5BD6FF))
    Mood.CALM -> Triple(Color(0xFF7C6BFF), Color(0xFF7FE3D0), Color(0xFFB7A8FF))
    Mood.WORRIED -> Triple(Color(0xFFFF6B8A), Color(0xFFFFB238), Color(0xFF9B6BFF))
    Mood.FOCUS -> Triple(Color(0xFF4F7BFF), Color(0xFF7C4DFF), Color(0xFF3FE0C5))
    Mood.SLEEPY -> Triple(Color(0xFF3E3A8C), Color(0xFF7E6BFF), Color(0xFF2B6CB0))
    Mood.CELEBRATE -> Triple(Color(0xFFFFC24B), Color(0xFFC4F25C), Color(0xFFFF7AC6))
}

/**
 * Placeholder companion until the mascot art arrives: a softly breathing, swirling
 * gradient orb whose colors follow the user's day.
 */
@Composable
fun MoodOrb(mood: Mood, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val (a0, b0, c0) = moodColors(mood)
    val a by animateColorAsState(a0, tween(800), label = "oa")
    val b by animateColorAsState(b0, tween(800), label = "ob")
    val cc by animateColorAsState(c0, tween(800), label = "oc")
    var spin = 0.8f
    var breath = 1f
    if (io.github.filipembcruz.rumo.ui.UiFlags.animations) {
        val t = rememberInfiniteTransition(label = "orb")
        val sp by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
        val br by t.animateFloat(
            0.94f, 1.04f,
            infiniteRepeatable(tween(if (mood == Mood.SLEEPY) 3200 else 2200), RepeatMode.Reverse), label = "breath",
        )
        spin = sp
        breath = br
    }
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2 * breath
        val center = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(Brush.radialGradient(listOf(a.copy(alpha = 0.35f), Color.Transparent), center, r * 1.25f), r * 1.25f, center)
        drawCircle(a, r * 0.92f, center)
        val p1 = Offset(center.x + cos(spin) * r * 0.35f, center.y + sin(spin) * r * 0.35f)
        val p2 = Offset(center.x + cos(spin + 2.3f) * r * 0.4f, center.y + sin(spin + 2.3f) * r * 0.4f)
        drawCircle(Brush.radialGradient(listOf(b, b.copy(alpha = 0f)), p1, r * 0.85f), r * 0.92f, center)
        drawCircle(Brush.radialGradient(listOf(cc, cc.copy(alpha = 0f)), p2, r * 0.75f), r * 0.92f, center)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)),
                Offset(center.x - r * 0.35f, center.y - r * 0.4f), r * 0.45f),
            r * 0.92f, center,
        )
    }
}
