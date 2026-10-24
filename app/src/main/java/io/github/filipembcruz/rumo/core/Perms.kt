package io.github.filipembcruz.rumo.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.filipembcruz.rumo.data.Collector
import io.github.filipembcruz.rumo.service.RumoAccessibilityService

object Perms {
    fun usage(context: Context) = Collector.hasPermission(context)

    fun notifications(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun accessibility(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(context, RumoAccessibilityService::class.java)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.let { c -> c.packageName == me.packageName && c.className == me.className } == true
        }
    }

    /** Reads the app-op directly: Settings.canDrawOverlays can stay true after the user revokes it. */
    fun overlay(context: Context): Boolean = runCatching {
        val ops = context.getSystemService(android.app.AppOpsManager::class.java)
        val uid = android.os.Process.myUid()
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow("android:system_alert_window", uid, context.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow("android:system_alert_window", uid, context.packageName)
        when (mode) {
            android.app.AppOpsManager.MODE_ALLOWED -> true
            android.app.AppOpsManager.MODE_DEFAULT -> Settings.canDrawOverlays(context)
            else -> false
        }
    }.getOrElse { Settings.canDrawOverlays(context) }

    fun openOverlaySettings(context: Context) = start(
        context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    )

    /** Some way to interrupt apps is available. */
    fun canBlock(context: Context) = accessibility(context)

    fun openUsageSettings(context: Context) = start(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openAccessibilitySettings(context: Context) = start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openNotificationSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    fun openAppSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )

    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
