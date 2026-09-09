package io.github.alexistrejo.pimienta.pos.app

import android.app.Application
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.seed.DebugBootstrapImporter
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker

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
                DebugBootstrapImporter(this, databaseProvider.database(RuntimeMode.SANDBOX)).importIfNeeded()
            }
            RuntimeMode.PRODUCTION -> SyncWorker.enqueue(this)
        }
    }

    fun switchMode(mode: RuntimeMode) {
        databaseProvider.modes.setMode(mode)
        if (mode == RuntimeMode.PRODUCTION) SyncWorker.enqueue(this) else SyncWorker.cancel(this)
        databaseProvider.close()
        if (mode == RuntimeMode.SANDBOX) DebugBootstrapImporter(this, databaseProvider.database(RuntimeMode.SANDBOX)).importIfNeeded()
    }
}
