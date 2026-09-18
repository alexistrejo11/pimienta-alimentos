package io.github.alexistrejo.pimienta.pos.data.printing

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.alexistrejo.pimienta.pos.app.posDatabaseProvider
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import java.util.concurrent.TimeUnit

private const val UNIQUE_PRINT = "pos-print"

// Drains one local print job per run so a failed device cannot block the UI.
class PrintWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val provider = appContext.posDatabaseProvider()

    override suspend fun doWork(): Result {
        val mode = provider.modes.mode()
        val database = provider.database(mode)
        val printer = PrinterFactory.create(applicationContext, mode)
        val processor = PrintJobProcessor(database, printer)
        repeat(50) {
            if (!processor.processNext()) return@repeat
        }
        return Result.success()
    }

    companion object {
        // Schedules one non-blocking attempt while preserving a single queue drainer.
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_PRINT,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<PrintWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }

        // Stops a queued print drain before the training database is deleted or replaced.
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PRINT)
        }
    }
}
