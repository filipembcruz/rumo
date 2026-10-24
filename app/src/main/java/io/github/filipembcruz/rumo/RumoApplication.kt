package io.github.filipembcruz.rumo

import android.app.Application
import io.github.filipembcruz.rumo.service.Notifier
import io.github.filipembcruz.rumo.work.RumoWorker
import java.io.File
import java.time.LocalDateTime

class RumoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLog()
        runCatching { Notifier.createChannels(this) }
        runCatching { RumoWorker.schedule(this) }
    }

    /** Writes any crash to crash.txt so it can be shown (and copied) on the next launch. */
    private fun installCrashLog() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(filesDir, CRASH_FILE).writeText(
                    "Rumo ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) — ${LocalDateTime.now()}\n" +
                        "Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
                        error.stackTraceToString()
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val CRASH_FILE = "crash.txt"
    }
}
