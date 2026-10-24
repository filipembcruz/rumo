package io.github.filipembcruz.rumo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.filipembcruz.rumo.data.UsageCollector
import java.util.concurrent.TimeUnit

/** Saves usage history every few hours, before Android forgets old events. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { UsageCollector.sync(applicationContext) }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("usage-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
