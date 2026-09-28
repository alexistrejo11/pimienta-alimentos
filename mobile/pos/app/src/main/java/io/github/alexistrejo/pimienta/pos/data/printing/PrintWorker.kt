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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UNIQUE_PRINT = "pos-print"

// Drains local print jobs so a failed device cannot block the UI.
class PrintWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val provider = appContext.posDatabaseProvider()

    override suspend fun doWork(): Result {
        val mode = provider.modes.mode()
        val database = provider.database(mode)
        val printer = PrinterFactory.create(applicationContext, mode)
        val processor = PrintJobProcessor(database, printer)
        repeat(50) {
            when (processor.processNext()) {
                PrintDrain.IDLE -> return Result.success()
                PrintDrain.DONE -> Unit
                PrintDrain.RETRY_LATER -> return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        // Drains pending print jobs directly on IO to bypass WorkManager scheduling delay when the printer is ready.
        suspend fun drainDirectly(context: Context): Boolean = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val provider = app.posDatabaseProvider()
            val mode = provider.modes.mode()
            val database = provider.database(mode)
            val printer = PrinterFactory.create(app, mode)
            val processor = PrintJobProcessor(database, printer)
            var processedAny = false
            repeat(50) {
                when (processor.processNext()) {
                    PrintDrain.IDLE -> return@withContext processedAny
                    PrintDrain.DONE -> processedAny = true
                    PrintDrain.RETRY_LATER -> return@withContext processedAny
                }
            }
            processedAny
        }

        // Prints in-process now, and keeps a single delayed WorkManager retry if the link is still down.
        fun enqueue(context: Context) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { drainDirectly(app) }
            }
            WorkManager.getInstance(app).enqueueUniqueWork(
                UNIQUE_PRINT,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PrintWorker>()
                    .setInitialDelay(30, TimeUnit.SECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }

        // Waits for the serial channel to come up after ACL before the first connect.
        fun enqueueAfterLink(context: Context) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                delay(LINK_SETTLE_MS)
                enqueue(app)
            }
        }

        // Stops a queued print drain before the training database is deleted or replaced.
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PRINT)
        }

        private const val LINK_SETTLE_MS = 1_000L
    }
}
