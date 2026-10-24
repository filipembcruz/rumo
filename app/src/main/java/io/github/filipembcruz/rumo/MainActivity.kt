package io.github.filipembcruz.rumo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.ui.CrashDialog
import io.github.filipembcruz.rumo.ui.LocalLocale
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.RumoRoot
import io.github.filipembcruz.rumo.ui.Strings
import io.github.filipembcruz.rumo.ui.Tab
import io.github.filipembcruz.rumo.ui.theme.RumoTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)
    private val startTab = mutableStateOf<Tab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startTab.value = tabFrom(intent)
        val prefs = Prefs.get(this)
        setContent {
            val version by prefs.changes.collectAsState()
            val pt = remember(version) { prefs.isPt }
            val theme = remember(version) { prefs.theme }
            val strings = remember(pt) { Strings(pt) }
            CompositionLocalProvider(LocalStrings provides strings, LocalLocale provides if (pt) Locale("pt", "BR") else Locale.ENGLISH) {
                RumoTheme(theme) {
                    RumoRoot(resumeTick = resumeTick.intValue, startTab = startTab.value)
                    CrashDialog()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startTab.value = tabFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    private fun tabFrom(i: Intent?): Tab? = when (i?.getStringExtra("tab")) {
        "insights" -> Tab.INSIGHTS
        "focus" -> Tab.FOCUS
        "growth" -> Tab.GROWTH
        else -> null
    }
}
