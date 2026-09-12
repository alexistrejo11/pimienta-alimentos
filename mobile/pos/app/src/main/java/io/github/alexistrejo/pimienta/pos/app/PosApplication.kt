package io.github.alexistrejo.pimienta.pos.app

import android.app.Application
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker

// Owns the two isolated Room databases and starts only the services for the active mode.
class PosApplication : Application() {
    lateinit var databaseProvider: PosDatabaseProvider
        private set

    val database: PosDatabase
        get() = databaseProvider.database()

    override fun onCreate() {
        super.onCreate()
        databaseProvider = PosDatabaseProvider(this)
        when (databaseProvider.modes.mode()) {
            RuntimeMode.SANDBOX -> {
                SyncWorker.cancel(this)
                resetTrainingScratch()
                PrintWorker.enqueue(this)
            }
            RuntimeMode.PRODUCTION -> {
                SyncWorker.enqueue(this)
                PrintWorker.enqueue(this)
            }
        }
    }

    fun enterTrainingMode() {
        databaseProvider.modes.setMode(RuntimeMode.SANDBOX)
        SyncWorker.cancel(this)
        resetTrainingScratch()
        PrintWorker.enqueue(this)
    }

    fun exitTrainingMode() {
        databaseProvider.modes.setMode(RuntimeMode.PRODUCTION)
        wipeTrainingScratch()
        SyncWorker.enqueue(this)
        PrintWorker.enqueue(this)
    }

    fun resetTrainingPlayground() {
        if (databaseProvider.modes.mode() != RuntimeMode.SANDBOX) return
        resetTrainingScratch()
    }

    private fun resetTrainingScratch() {
        val database = databaseProvider.resetTrainingDatabase()
        TrainingBootstrapImporter(this).resetFromTemplate(database)
    }

    private fun wipeTrainingScratch() {
        databaseProvider.resetTrainingDatabase()
    }
}
