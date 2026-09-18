package io.github.alexistrejo.pimienta.pos.data.sync

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Runs push+pull on the calling coroutine so the sync button can await a result.
suspend fun runForegroundSync(context: Context): String {
    val outcome = PosSyncPipeline(context).run()
    val lastError = PosDatabaseProvider(context).database(RuntimeMode.PRODUCTION).syncDao().state()?.lastError
    return when (outcome) {
        PosSyncNowOutcome.SUCCESS -> "Sincronización completada."
        PosSyncNowOutcome.SKIPPED -> "No hay una sesión de producción para sincronizar."
        PosSyncNowOutcome.RETRY -> lastError?.let { PosApiUserMessages.from(IllegalStateException(it)) }
            ?: "No se pudo sincronizar ahora. Revisa la red."
        PosSyncNowOutcome.FAILURE -> lastError?.let { PosApiUserMessages.from(IllegalStateException(it)) }
            ?: "No se pudo sincronizar. Vuelve a enrolar si el dispositivo fue revocado."
    }
}
