package io.github.filipembcruz.rumo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.rumo.R

@Immutable
data class RumoColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val lime: Color,
    val limeSoft: Color,
    val onLime: Color,
    val coral: Color,
    val coralSoft: Color,
    val amber: Color,
    val mint: Color,
    val sky: Color,
    val navy: Color,
    val onNavy: Color,
    val hatch: Color,
)

val LightColors = RumoColors(
    dark = false,
    bg = Color(0xFFF4F3FB),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEFECFB),
    outline = Color(0xFFE5E2F2),
    text = Color(0xFF1B1734),
    muted = Color(0xFF7B7894),
    primary = Color(0xFF6B4EFF),
    primaryDeep = Color(0xFF4A2FD9),
    primarySoft = Color(0xFFE6E0FF),
    onPrimary = Color.White,
    lime = Color(0xFFC4F25C),
    limeSoft = Color(0xFFEFFBD2),
    onLime = Color(0xFF1E2A05),
    coral = Color(0xFFFF6B6B),
    coralSoft = Color(0xFFFFE5E5),
    amber = Color(0xFFFFB238),
    mint = Color(0xFF22C99A),
    sky = Color(0xFF4FA8FF),
    navy = Color(0xFF1D1937),
    onNavy = Color(0xFFF4F2FF),
    hatch = Color(0xFFD9D4F0),
)

val DarkColors = RumoColors(
    dark = true,
    bg = Color(0xFF0E0C1B),
    surface = Color(0xFF19172B),
    surfaceAlt = Color(0xFF231F3B),
    outline = Color(0xFF2E2A49),
    text = Color(0xFFF3F1FF),
    muted = Color(0xFF9D99BA),
    primary = Color(0xFF8F7BFF),
    primaryDeep = Color(0xFF6B4EFF),
    primarySoft = Color(0xFF2D2659),
    onPrimary = Color.White,
    lime = Color(0xFFC4F25C),
    limeSoft = Color(0xFF2B3515),
    onLime = Color(0xFF1E2A05),
    coral = Color(0xFFFF7A7A),
    coralSoft = Color(0xFF3B1F2A),
    amber = Color(0xFFFFBE55),
    mint = Color(0xFF34DBAA),
    sky = Color(0xFF63B4FF),
    navy = Color(0xFF2A2547),
    onNavy = Color(0xFFF4F2FF),
    hatch = Color(0xFF332E52),
)

val LocalRumoColors = staticCompositionLocalOf { LightColors }

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_medium, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val RumoType = Typography(
    displayLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun RumoTheme(themePref: String = "system", content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, secondary = c.lime, onSecondary = c.onLime,
        background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.outline, error = c.coral,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceAlt, surfaceContainerLow = c.surface,
        surfaceContainerHighest = c.surfaceAlt, primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
    ) else lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, secondary = c.lime, onSecondary = c.onLime,
        background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.outline, error = c.coral,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceAlt, surfaceContainerLow = c.surface,
        surfaceContainerHighest = c.surfaceAlt, primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
    )
    CompositionLocalProvider(LocalRumoColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = RumoType, content = content)
    }
}

object R2 {
    val colors: RumoColors
        @Composable get() = LocalRumoColors.current
}
