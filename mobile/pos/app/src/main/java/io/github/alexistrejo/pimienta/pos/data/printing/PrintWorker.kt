package io.github.alexistrejo.pimienta.pos.data.printing

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.hardware.FakeTicketPrinter
import io.github.alexistrejo.pimienta.pos.hardware.PeripheralStatus
import io.github.alexistrejo.pimienta.pos.hardware.PrintFailure
import io.github.alexistrejo.pimienta.pos.hardware.PrintResult
import io.github.alexistrejo.pimienta.pos.hardware.PrinterProfile
import io.github.alexistrejo.pimienta.pos.hardware.PrinterProfiles
import io.github.alexistrejo.pimienta.pos.hardware.TicketPrinter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

private const val UNIQUE_PRINT = "pos-print"

// Drains one local print job per run so a failed device cannot block the UI.
class PrintWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val provider = PosDatabaseProvider(appContext)

    override suspend fun doWork(): Result {
        val mode = provider.modes.mode()
        val database = provider.database(mode)
        val printer = if (mode == RuntimeMode.SANDBOX) FakeTicketPrinter() else UnavailableTicketPrinter
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
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PrintWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}

// Makes production failures explicit until a physical printer adapter is installed.
private object UnavailableTicketPrinter : TicketPrinter {
    override val profile: PrinterProfile = PrinterProfiles.genericEscPos58
    override val status: Flow<PeripheralStatus> = MutableStateFlow(PeripheralStatus.DISCONNECTED)
    override suspend fun print(bytes: ByteArray): PrintResult = PrintResult.Failed(PrintFailure.NO_PRINTER)
}
