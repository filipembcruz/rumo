package io.github.filipembcruz.rumo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.ui.components.bounceClick
import io.github.filipembcruz.rumo.ui.screens.DetailRoute
import io.github.filipembcruz.rumo.ui.screens.FocusRoute
import io.github.filipembcruz.rumo.ui.screens.GrowthRoute
import io.github.filipembcruz.rumo.ui.screens.HomeRoute
import io.github.filipembcruz.rumo.ui.screens.InsightsRoute
import io.github.filipembcruz.rumo.ui.screens.OnboardingRoute
import io.github.filipembcruz.rumo.ui.screens.SettingsRoute
import io.github.filipembcruz.rumo.ui.theme.R2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class Tab(val icon: ImageVector) {
    HOME(Icons.Rounded.Home), INSIGHTS(Icons.Rounded.Insights), FOCUS(Icons.Rounded.SelfImprovement), GROWTH(Icons.Rounded.Spa)
}

sealed interface Dest {
    data object Settings : Dest
    data class Detail(val kind: TargetKind, val id: String) : Dest
}

class Nav(
    val openDetail: (TargetKind, String) -> Unit,
    val openSettings: () -> Unit,
    val back: () -> Unit,
    val goTab: (Tab) -> Unit,
    val refresh: () -> Unit,
    val tick: Int,
    val setupBlocking: () -> Unit = {},
)

val LocalNav = staticCompositionLocalOf<Nav> { error("no nav") }
val LocalStrings = staticCompositionLocalOf { Strings(false) }
val LocalLocale = staticCompositionLocalOf { Locale.ENGLISH }

/** Loads a model off the main thread; keeps the previous value while reloading. */
@Composable
fun <T> rememberModel(vararg keys: Any?, load: () -> T): T? {
    var value by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(*keys) {
        val v = withContext(Dispatchers.IO) { runCatching { load() }.getOrNull() }
        if (v != null) value = v
    }
    return value
}

@Composable
fun RumoRoot(resumeTick: Int, startTab: Tab? = null) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    var onboarded by remember { mutableStateOf(prefs.onboarded) }
    var tab by rememberSaveable { mutableStateOf(startTab ?: Tab.HOME) }
    LaunchedEffect(startTab) { if (startTab != null) tab = startTab }
    val stack = remember { mutableStateListOf<Dest>() }
    var refresh by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var showSetup by remember { mutableStateOf(false) }

    LaunchedEffect(resumeTick, refresh) {
        withContext(Dispatchers.IO) { runCatching { Collector.sync(context, full = refresh == 0 && resumeTick <= 1) } }
    }

    val nav = Nav(
        openDetail = { k, id -> forward = true; stack.add(Dest.Detail(k, id)) },
        openSettings = { forward = true; stack.add(Dest.Settings) },
        back = { if (stack.isNotEmpty()) { forward = false; stack.removeAt(stack.lastIndex) } },
        goTab = { stack.clear(); tab = it },
        refresh = { refresh++ },
        tick = resumeTick * 1000 + refresh,
        setupBlocking = { showSetup = true },
    )

    CompositionLocalProvider(LocalNav provides nav) {
        if (showSetup) io.github.filipembcruz.rumo.ui.screens.BlockingSetupSheet(onDismiss = { showSetup = false; refresh++ })
        if (!onboarded) {
            OnboardingRoute(onDone = { prefs.onboarded = true; onboarded = true; refresh++ })
            return@CompositionLocalProvider
        }
        BackHandler(enabled = stack.isNotEmpty()) { nav.back() }
        BackHandler(enabled = stack.isEmpty() && tab != Tab.HOME) { tab = Tab.HOME }
        Box(Modifier.fillMaxSize().background(R2.colors.bg)) {
            AnimatedContent(
                targetState = stack.lastOrNull(),
                transitionSpec = {
                    if (forward) {
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(200)))
                    }
                },
                label = "stack",
            ) { dest ->
                when (dest) {
                    null -> Box(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                                (slideInHorizontally(tween(300)) { dir * it / 5 } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally(tween(300)) { -dir * it / 5 } + fadeOut(tween(180)))
                            },
                            label = "tabs",
                        ) { t ->
                            when (t) {
                                Tab.HOME -> HomeRoute()
                                Tab.INSIGHTS -> InsightsRoute()
                                Tab.FOCUS -> FocusRoute()
                                Tab.GROWTH -> GrowthRoute()
                            }
                        }
                        FloatingNavBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
                    }
                    is Dest.Settings -> SettingsRoute()
                    is Dest.Detail -> DetailRoute(dest.kind, dest.id)
                }
            }
        }
    }
}

@Composable
fun FloatingNavBar(selected: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    val c = R2.colors
    AnimatedVisibility(true, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Row(
            Modifier.navigationBarsPadding().padding(bottom = 14.dp)
                .shadow(24.dp, RoundedCornerShape(50), ambientColor = c.primary, spotColor = c.primary)
                .clip(RoundedCornerShape(50)).background(c.navy).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val sel = t == selected
                val bg = if (sel) c.lime else Color.Transparent
                val fg = if (sel) c.onLime else c.onNavy.copy(alpha = 0.7f)
                Box(
                    Modifier.size(width = if (sel) 64.dp else 52.dp, height = 52.dp).bounceClick { onSelect(t) }
                        .clip(CircleShape).background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(sel, transitionSpec = { scaleIn(tween(220)) + fadeIn() togetherWith fadeOut(tween(100)) }, label = "ic") {
                        Icon(t.icon, t.name, tint = fg, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

/** Space so content can scroll above the floating nav bar. */
val NavBarSpace = 120.dp

@Composable
fun BottomSpacer() = Box(Modifier.fillMaxWidth().navigationBarsPadding().height(NavBarSpace))

/** Test hook: screenshot tests turn entrance animations off so frames are complete. */
object UiFlags {
    @Volatile var animations = true
}
