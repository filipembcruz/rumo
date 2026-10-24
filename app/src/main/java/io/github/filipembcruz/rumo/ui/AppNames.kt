package io.github.filipembcruz.rumo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/** Cached app names and icons, plus the list of launchable apps for pickers. */
object AppNames {
    private val labels = ConcurrentHashMap<String, String>()
    private val icons = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    fun label(context: Context, pkg: String): String = labels.getOrPut(pkg) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(fallback(pkg))
    }

    private val generic = setOf("com", "org", "net", "io", "br", "android", "app", "apps", "mobile", "www", "github", "google", "client", "lite", "id", "app", "free", "pro")

    fun fallback(pkg: String): String {
        val parts = pkg.split('.').filter { it.isNotBlank() && it.lowercase() !in generic }
        return (parts.lastOrNull() ?: pkg).replaceFirstChar { it.uppercase() }
    }

    fun icon(context: Context, pkg: String): ImageBitmap? {
        val cached = icons[pkg]
        if (cached != null) return cached
        if (pkg in missing) return null
        val bmp = runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap() }.getOrNull()
        if (bmp == null) missing.add(pkg) else icons[pkg] = bmp
        return bmp
    }

    /** Launchable apps (package, label), sorted by label. */
    fun launchable(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != context.packageName }
            .distinct()
            .map { it to label(context, it) }
            .sortedBy { it.second.lowercase() }
    }
}
