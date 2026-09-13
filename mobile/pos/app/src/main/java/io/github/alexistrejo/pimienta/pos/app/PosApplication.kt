package io.github.alexistrejo.pimienta.pos.app

import android.app.Application
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.Executors

// Owns the two isolated Room databases and starts only the services for the active mode.
class PosApplication : Application() {
    lateinit var databaseProvider: PosDatabaseProvider
        private set

    val database: PosDatabase
        get() = databaseProvider.database()

    // Signals when a cold-start training wipe/import has finished (or is not needed).
    private val trainingReady = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        databaseProvider = PosDatabaseProvider(this)
        when (databaseProvider.modes.mode()) {
            RuntimeMode.SANDBOX -> {
                SyncWorker.cancel(this)
                PrintWorker.cancel(this)
                // Room cannot run on the main thread; bootstrap off-UI then open printers.
                Executors.newSingleThreadExecutor().execute {
                    try {
                        resetTrainingScratch()
                        PrintWorker.enqueue(this)
                    } finally {
                        trainingReady.complete(Unit)
                    }
                }
            }
            RuntimeMode.PRODUCTION -> {
                trainingReady.complete(Unit)
                SyncWorker.enqueue(this)
                PrintWorker.enqueue(this)
            }
        }
    }

    // Blocks until sandbox bootstrap is safe to read (no-op in production).
    suspend fun awaitActiveDatabaseReady() {
        if (databaseProvider.modes.mode() == RuntimeMode.SANDBOX) {
            trainingReady.await()
        }
    }

    fun enterTrainingMode() {
        databaseProvider.modes.setMode(RuntimeMode.SANDBOX)
        SyncWorker.cancel(this)
        PrintWorker.cancel(this)
        resetTrainingScratch()
        PrintWorker.enqueue(this)
        if (!trainingReady.isCompleted) trainingReady.complete(Unit)
    }

    fun exitTrainingMode() {
        databaseProvider.modes.setMode(RuntimeMode.PRODUCTION)
        PrintWorker.cancel(this)
        wipeTrainingScratch()
        SyncWorker.enqueue(this)
        PrintWorker.enqueue(this)
    }

    fun resetTrainingPlayground() {
        if (databaseProvider.modes.mode() != RuntimeMode.SANDBOX) return
        PrintWorker.cancel(this)
        resetTrainingScratch()
        PrintWorker.enqueue(this)
    }

    private fun resetTrainingScratch() {
        val database = databaseProvider.resetTrainingDatabase()
        TrainingBootstrapImporter(this).resetFromTemplate(database)
    }

    private fun wipeTrainingScratch() {
        databaseProvider.resetTrainingDatabase()
    }
}
