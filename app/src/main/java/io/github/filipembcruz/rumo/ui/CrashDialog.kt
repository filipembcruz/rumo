package io.github.filipembcruz.rumo.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import io.github.filipembcruz.rumo.RumoApplication
import io.github.filipembcruz.rumo.ui.theme.R2
import java.io.File

/** If the app crashed last time, show what happened so it can be copied and reported. */
@Composable
fun CrashDialog() {
    val context = LocalContext.current
    val s = LocalStrings.current
    val file = remember { File(context.filesDir, RumoApplication.CRASH_FILE) }
    var text by remember { mutableStateOf(if (file.exists()) runCatching { file.readText() }.getOrNull() else null) }
    val clipboard = LocalClipboardManager.current
    val report = text ?: return
    fun close() { file.delete(); text = null }
    AlertDialog(
        onDismissRequest = { close() },
        title = { Text(s.crashTitle) },
        text = { Text(report.take(5000), style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton({ clipboard.setText(AnnotatedString(report)); close() }) { Text(s.copyAndClose) } },
        dismissButton = { TextButton({ close() }) { Text(s.close) } },
        containerColor = R2.colors.surface,
    )
}
