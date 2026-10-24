package io.github.filipembcruz.rumo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.ui.AppNames
import io.github.filipembcruz.rumo.ui.theme.R2

fun Cat.color(): Color = Color(color)

/** Press feedback: a gentle scale-down while pressed. */
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.965f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun RCard(
    modifier: Modifier = Modifier,
    color: Color = R2.colors.surface,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = R2.colors
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.bounceClick(onClick = onClick)
    m = m.clip(shape).background(color)
    if (c.dark && color == c.surface) m = m.border(1.dp, c.outline, shape)
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = R2.colors.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = R2.colors.primary)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = R2.colors.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun IconBadge(icon: ImageVector, bg: Color, fg: Color, size: Dp = 40.dp, iconSize: Dp = 20.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.36f)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, accent: Color, accentBg: Color, modifier: Modifier = Modifier, sub: String? = null, subColor: Color = accent) {
    RCard(modifier, padding = PaddingValues(16.dp), shape = RoundedCornerShape(24.dp)) {
        IconBadge(icon, accentBg, accent, 34.dp, 18.dp)
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = R2.colors.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = R2.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val icon = remember(pkg) { AppNames.icon(context, pkg) }
    if (icon != null) {
        Image(icon, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)))
    } else {
        val label = remember(pkg) { AppNames.label(context, pkg) }
        LetterAvatar(label, R2.colors.primary, size)
    }
}

@Composable
fun LetterAvatar(text: String, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.65f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp, style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun SiteAvatar(domain: String, cat: Cat, size: Dp = 40.dp) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { io.github.filipembcruz.rumo.core.Prefs.get(ctx) }
    val enabled = prefs.siteIcons
    val icon by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
        if (enabled) io.github.filipembcruz.rumo.data.SiteIcons.cached(ctx, domain) else null, domain, enabled,
    ) {
        if (enabled && value == null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { io.github.filipembcruz.rumo.data.SiteIcons.get(ctx, domain) }.getOrNull()
            }
        }
    }
    val bmp = icon
    if (bmp == null) {
        LetterAvatar(domain.removePrefix("www."), cat.color(), size)
    } else {
        val img = remember(bmp) { bmp.asImageBitmap() }
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(img, null, Modifier.size(size * 0.78f).clip(RoundedCornerShape(size * 0.16f)))
        }
    }
}

@Composable
fun CatDot(cat: Cat, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(cat.color()))
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true,
                  bg: Color = R2.colors.primary, fg: Color = R2.colors.onPrimary, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.45f, label = "btn")
    Row(
        modifier.height(56.dp).bounceClick(enabled = enabled, onClick = onClick).graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    PrimaryButton(text, modifier, icon, bg = R2.colors.surfaceAlt, fg = R2.colors.text, onClick = onClick)
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = R2.colors.muted, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun ToggleRow(icon: ImageVector?, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, iconTint: Color = R2.colors.primary) {
    val c = R2.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon, iconTint.copy(alpha = 0.14f), iconTint, 38.dp, 19.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.primary, checkedThumbColor = Color.White,
                uncheckedTrackColor = c.surfaceAlt, uncheckedThumbColor = c.muted, uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun NavRow(icon: ImageVector?, title: String, subtitle: String? = null, value: String? = null,
           iconTint: Color = R2.colors.primary, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val c = R2.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon, iconTint.copy(alpha = 0.14f), iconTint, 38.dp, 19.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = c.muted)
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

@Composable
fun Banner(icon: ImageVector, text: String, action: String?, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick(onClick = onClick).clip(RoundedCornerShape(22.dp)).background(bg).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Text(action, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyNote(icon: ImageVector, title: String, body: String) {
    val c = R2.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, c.primarySoft, c.primary, 52.dp, 26.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
    }
}

/** Animated pill segmented control. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = R2.colors
    BoxWithConstraints(modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50)).background(c.surfaceAlt).padding(4.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(50)).background(c.surface))
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val col by animateColorAsState(if (i == selected) c.text else c.muted, tween(200), label = "segc")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = col, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
        }
    }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null, accent: Color = R2.colors.primary) {
    val c = R2.colors
    val bg by animateColorAsState(if (selected) accent else c.surface, tween(180), label = "chipbg")
    val fg by animateColorAsState(if (selected) Color.White else c.text, tween(180), label = "chipfg")
    Row(
        Modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(50)).background(bg)
            .border(1.dp, if (selected) Color.Transparent else c.outline, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = R2.colors.surfaceAlt, height: Dp = 8.dp) {
    val target = fraction.coerceIn(0f, 1f)
    val anim = remember { androidx.compose.animation.core.Animatable(if (io.github.filipembcruz.rumo.ui.UiFlags.animations) 0f else target) }
    androidx.compose.runtime.LaunchedEffect(target) {
        anim.animateTo(target, tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    val f = anim.value
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}
