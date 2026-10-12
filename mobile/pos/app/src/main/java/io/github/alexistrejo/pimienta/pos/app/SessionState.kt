package io.github.alexistrejo.pimienta.pos.app

import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.sync.DeviceSessionPolicy

// Snapshot of application/session state the Compose root renders from.
data class SessionState(
    val mode: RuntimeMode,
    val isDebug: Boolean = false,
    val initialized: Boolean = false,
    val syncState: SyncStateEntity? = null,
    val users: List<LocalUserEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val policy: PosPolicyEntity? = null,
    val shift: ShiftEntity? = null,
    val pendingEventsCount: Int = 0,
    val notice: String? = null,
    val enrolling: Boolean = false,
    val enrollError: String? = null,
    val productionEnrolled: Boolean = false,
    val hasAccessToken: Boolean = false,
    val switchAuthorizers: List<LocalUserEntity> = emptyList(),
    val availableUpdateVersionName: String? = null,
) {
    // Release builds enrolled in production need a manager PIN to change mode.
    val requiresPinForSwitch: Boolean get() = !isDebug && productionEnrolled

    // Bootstrap moves ENROLLED to ONLINE. Treating only ENROLLED as enrolled sends the tablet back to the form.
    val showEnrollment: Boolean
        get() = mode == RuntimeMode.PRODUCTION && DeviceSessionPolicy.showsEnrollmentForm(
            syncState?.status,
            !syncState?.baseUrl.isNullOrBlank(),
            hasAccessToken,
        )

    val canResetTrainingDemo: Boolean get() = isDebug && mode == RuntimeMode.SANDBOX

    val enrollmentErrorMessage: String? get() = enrollError ?: syncState?.lastError

    // Which root screen the session is in; manager/device overlays are decided by the UI.
    val destination: SessionDestination
        get() = when {
            !initialized -> SessionDestination.Loading(loadingMessage(mode))
            showEnrollment -> SessionDestination.Enrollment
            users.isEmpty() || products.isEmpty() -> SessionDestination.WaitingForCatalog(
                message = waitingForCatalogMessage(),
                canResetEnrollment = mode == RuntimeMode.PRODUCTION,
            )
            shift == null -> SessionDestination.Access
            else -> SessionDestination.Sale(shift)
        }

    private fun waitingForCatalogMessage(): String? = when {
        !notice.isNullOrBlank() -> notice
        !syncState?.lastError.isNullOrBlank() -> syncState?.lastError
        users.isEmpty() && products.isNotEmpty() ->
            "Catálogo recibido (${products.size} producto(s)), pero falta: operadores POS con PIN en esta sede."
        users.isNotEmpty() && products.isEmpty() ->
            "Operadores recibidos (${users.size}), pero falta: productos en el catálogo POS de la sede (o hay que resincronizar bootstrap)."
        else -> loadingMessage(mode)
    }
}

// Root screens selected from session state.
sealed interface SessionDestination {
    data class Loading(val message: String) : SessionDestination
    data object Enrollment : SessionDestination
    data class WaitingForCatalog(val message: String?, val canResetEnrollment: Boolean) : SessionDestination
    data object Access : SessionDestination
    data class Sale(val shift: ShiftEntity) : SessionDestination
}

internal fun loadingMessage(mode: RuntimeMode): String = when (mode) {
    RuntimeMode.SANDBOX -> "Preparando entorno de capacitación…"
    RuntimeMode.PRODUCTION -> "Cargando datos de venta…"
}
