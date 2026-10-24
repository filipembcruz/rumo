package io.github.filipembcruz.rumo.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF0F5C55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFECE4),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF9A6A12),
    secondaryContainer = Color(0xFFFFDEA6),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8DD6CB),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0F5C55),
    onPrimaryContainer = Color(0xFFBFECE4),
    secondary = Color(0xFFFFD27A),
    secondaryContainer = Color(0xFF5C3F00),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
)

@Composable
fun RumoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
