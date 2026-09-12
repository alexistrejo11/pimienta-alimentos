package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.BuildConfig
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode

// Centralizes when training mode switches require a production manager PIN.
object TrainingModePolicy {
    fun isEnrolled(provider: PosDatabaseProvider): Boolean =
        provider.database(RuntimeMode.PRODUCTION).syncDao().state()?.baseUrl != null

    fun requiresPinForModeSwitch(
        isDebug: Boolean = BuildConfig.DEBUG,
        isEnrolled: Boolean,
    ): Boolean = !isDebug && isEnrolled
}
