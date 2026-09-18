package io.github.alexistrejo.pimienta.pos.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val UNIQUE_SYNC = "pos-sync"

// Background drain of the outbox and catalog pull; the sync button does not enqueue this.
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return when (PosSyncPipeline(applicationContext).run()) {
            PosSyncNowOutcome.SUCCESS, PosSyncNowOutcome.SKIPPED -> Result.success()
            PosSyncNowOutcome.RETRY -> Result.retry()
            PosSyncNowOutcome.FAILURE -> Result.failure()
        }
    }

    companion object {
        fun cancel(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(UNIQUE_SYNC)
            wm.cancelUniqueWork(UNIQUE_SYNC + "-periodic")
        }

        fun enqueue(context: Context) {
            val wm = WorkManager.getInstance(context)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            wm.enqueueUniqueWork(UNIQUE_SYNC, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
            // Background periodic sync every 15 minutes when app is idle or closed.
            wm.enqueueUniquePeriodicWork(UNIQUE_SYNC + "-periodic", ExistingPeriodicWorkPolicy.UPDATE, PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
        }
    }
}

typealias SyncSalesWorker = SyncWorker
