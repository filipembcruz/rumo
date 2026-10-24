@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.filipembcruz.rumo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.ui.LocalNav
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.components.IconBadge
import io.github.filipembcruz.rumo.ui.components.PrimaryButton
import io.github.filipembcruz.rumo.ui.components.SecondaryButton
import io.github.filipembcruz.rumo.ui.theme.R2

/** Guides the user through Android's "restricted setting" for sideloaded apps, with a fallback. */
@Composable
fun BlockingSetupSheet(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val c = R2.colors
    val context = LocalContext.current
    val nav = LocalNav.current
    val a11y = remember(nav.tick) { Perms.accessibility(context) }
    RumoSheet(onDismiss, s.setupTitle) {
        Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
            Text(s.setupIntro, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            Spacer(Modifier.height(16.dp))
            OptionCard(Icons.Rounded.AccessibilityNew, s.setupA11yTitle, s.setupA11yBody, a11y, s.recommended) {
                if (!a11y) {
                    Step(1, s.setupStep1)
                    Step(2, s.setupStep2)
                    Step(3, s.setupStep3)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(s.openAccessibility, Modifier.fillMaxWidth()) { Perms.openAccessibilitySettings(context) }
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(s.openAppInfo, Modifier.fillMaxWidth()) { Perms.openAppSettings(context) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionCard(icon: ImageVector, title: String, body: String, on: Boolean, badge: String?, extra: @Composable () -> Unit) {
    val c = R2.colors
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, if (on) c.mint.copy(alpha = 0.15f) else c.primarySoft, if (on) c.mint else c.primary, 44.dp, 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
                if (badge != null && !on) Text(badge, style = MaterialTheme.typography.labelSmall, color = c.primary)
            }
            if (on) {
                Icon(Icons.Rounded.CheckCircle, null, tint = c.mint)
                Spacer(Modifier.width(4.dp))
                Text(s.on, style = MaterialTheme.typography.labelMedium, color = c.mint)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(10.dp))
        extra()
    }
}

@Composable
private fun Step(n: Int, text: String) {
    val c = R2.colors
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
            Text("$n", color = c.onPrimary, style = MaterialTheme.typography.labelMedium)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
    }
}
