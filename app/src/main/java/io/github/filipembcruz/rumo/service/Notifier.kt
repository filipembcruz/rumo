package io.github.filipembcruz.rumo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.filipembcruz.rumo.MainActivity
import io.github.filipembcruz.rumo.R
import io.github.filipembcruz.rumo.core.Perms
import io.github.filipembcruz.rumo.core.Prefs
import io.github.filipembcruz.rumo.ui.Strings

object Notifier {
    const val CH_LIMITS = "limits"
    const val CH_NUDGES = "nudges"
    const val CH_REPORTS = "reports"
    const val CH_FOCUS = "focus"
    const val CH_MONITOR = "monitor"
    const val ID_MONITOR = 1

    fun strings(context: Context) = Strings(Prefs.get(context).isPt)

    fun createChannels(context: Context) {
        val s = strings(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            NotificationChannel(CH_LIMITS, s.chLimits, NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CH_NUDGES, s.chNudges, NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_REPORTS, s.chReports, NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_FOCUS, s.chFocus, NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_MONITOR, s.chMonitor, NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) },
        ).forEach { nm.createNotificationChannel(it) }
    }

    private fun open(context: Context, tab: String? = null): PendingIntent {
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (tab != null) i.putExtra("tab", tab)
        return PendingIntent.getActivity(context, tab.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun build(context: Context, channel: String, title: String, body: String, tab: String? = null, ongoing: Boolean = false): android.app.Notification =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_rumo)
            .setColor(0xFF6B4EFF.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open(context, tab))
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setSilent(ongoing)
            .setPriority(if (channel == CH_LIMITS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

    fun post(context: Context, channel: String, id: Int, title: String, body: String, tab: String? = null, ongoing: Boolean = false) {
        if (!Perms.notifications(context)) return
        val n = build(context, channel, title, body, tab, ongoing)
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)
}
