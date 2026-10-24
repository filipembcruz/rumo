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
