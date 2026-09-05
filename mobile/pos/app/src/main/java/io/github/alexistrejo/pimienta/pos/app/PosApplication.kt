package io.github.alexistrejo.pimienta.pos.app

import android.app.Application
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.seed.DebugBootstrapImporter

// Creates the local database and imports the debug catalog on development builds.
class PosApplication : Application() {
    lateinit var database: PosDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = PosDatabase.create(this)
        DebugBootstrapImporter(this, database).importIfNeeded()
    }
}
