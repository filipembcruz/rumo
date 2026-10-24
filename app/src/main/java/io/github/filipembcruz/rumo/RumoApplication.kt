package io.github.filipembcruz.rumo

import android.app.Application
import io.github.filipembcruz.rumo.work.SyncWorker
import java.io.File
import java.time.LocalDateTime

class RumoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLog()
        SyncWorker.schedule(this)
    }

    /** Writes any crash to crash.txt so it can be shown on the next launch. */
    private fun installCrashLog() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(filesDir, CRASH_FILE).writeText(
                    "Rumo ${BuildConfigInfo.version(this)} — ${LocalDateTime.now()}\n\n" +
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

object BuildConfigInfo {
    fun version(app: Application): String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")
}
