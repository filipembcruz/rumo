package io.github.filipembcruz.rumo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import io.github.filipembcruz.rumo.ui.RumoApp
import io.github.filipembcruz.rumo.ui.RumoTheme

class MainActivity : ComponentActivity() {
    /** Bumped on every resume so screens re-check permissions and refresh data. */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RumoTheme {
                RumoApp(resumeTick = resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}
