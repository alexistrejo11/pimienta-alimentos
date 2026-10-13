package io.github.alexistrejo.pimienta.pos.app

import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.sync.DeviceSessionPolicy
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns application/session state and transitions for the Compose root: runtime mode,
 * initial load, enrollment, reenrollment, catalog bootstrap trigger, mode switching,
 * and the update banner. It coordinates existing services through [SessionEnvironment]
 * and holds no POS business rules.
 *
 * Lives as long as the composition that created it (same lifetime as the former
 * `remember`/`LaunchedEffect` state in `PosApp`), so activity recreation still re-runs
 * the startup load exactly as before.
 */
class SessionCoordinator(
    private val scope: CoroutineScope,
    private val env: SessionEnvironment,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow(SessionState(mode = env.currentMode(), isDebug = env.isDebug))
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    private var data: SessionDataSource = env.dataSource(mutableState.value.mode)
    private var loadGeneration = 0
    private var productionCatalogSyncAttempted = false
    private var started = false

    private var observeJob: Job? = null
    // Keyed jobs replicate the former LaunchedEffect keys: a key change cancels and relaunches.
    private var catalogSyncKey: List<Any?>? = null
    private var catalogSyncJob: Job? = null
    private var authorizersKey: List<Any?>? = null
    private var authorizersJob: Job? = null

    // Runs the startup sequence once: enrollment flag, update check, first load, live observation.
    fun start() {
        if (started) return
        started = true
        if (!env.isDebug) {
            scope.launch {
                val enrolled = withContext(io) { env.isProductionEnrolled() }
                mutate { it.copy(productionEnrolled = enrolled) }
            }
        }
        scope.launch { checkForUpdate() }
        reload()
        observeActiveData()
        onStateChanged()
    }

    // Reloads the active database snapshot; stale loads from an older generation are dropped.
    fun reload(): Job = reload(orphanCheckMode = mutableState.value.mode)

    private fun reload(orphanCheckMode: RuntimeMode): Job {
        val generation = ++loadGeneration
        val source = data
        return scope.launch {
            val snapshot = withContext(io) {
                env.awaitActiveDatabaseReady()
                if (orphanCheckMode == RuntimeMode.PRODUCTION) reconcileOrphanSession(source)
                LoadSnapshot(source.syncState(), source.users(), source.products(), source.activeShift(), env.hasAccessToken())
            }
            if (generation != loadGeneration) return@launch
            mutate {
                it.copy(
                    syncState = snapshot.syncState,
                    users = snapshot.users,
                    products = snapshot.products,
                    shift = snapshot.shift,
                    hasAccessToken = snapshot.hasAccessToken,
                    initialized = true,
                )
            }
        }
    }

    // Only force re-enrollment when explicitly revoked or no credentials remain.
    private fun reconcileOrphanSession(source: SessionDataSource) {
        val sync = source.syncState()
        val hasUrl = !sync?.baseUrl.isNullOrBlank()
        val hasAccess = env.hasAccessToken()
        val hasRefresh = env.hasRefreshToken()
        if (DeviceSessionPolicy.orphanAccessRequiresReset(sync?.status, hasUrl, hasAccess, hasRefresh)) {
            requireReenrollment(sync?.lastError ?: DeviceSessionPolicy.orphanAccessTokenMessage())
        } else if (hasUrl && !hasAccess && hasRefresh) {
            env.enqueueSync()
        }
    }

    // Single UI-side entry point that turns an invalid device session into a reenrollment transition.
    private fun requireReenrollment(reason: String) {
        env.resetForReenrollment(reason)
    }

    // Room flows keep catalog and policy state live after every committed sync transaction.
    private fun observeActiveData() {
        observeJob?.cancel()
        val source = data
        observeJob = scope.launch {
            launch { source.observeProducts().collect { products -> mutate { it.copy(products = products) } } }
            launch { source.observeUsers().collect { users -> mutate { it.copy(users = users) } } }
            launch { source.observePolicy().collect { policy -> mutate { it.copy(policy = policy) } } }
            launch {
                source.observeSyncState().collect { sync ->
                    val hasAccess = withContext(io) { env.hasAccessToken() }
                    mutate { it.copy(syncState = sync, hasAccessToken = hasAccess) }
                }
            }
            launch { source.observePendingEvents().collect { count -> mutate { it.copy(pendingEventsCount = count) } } }
            // Sale vs Access follows Room: a CLOSED shift must not keep the register on screen.
            launch { source.observeActiveShift().collect { shift -> mutate { it.copy(shift = shift) } } }
        }
    }

    // Applies a state change and restarts keyed work whose inputs changed.
    private fun mutate(transform: (SessionState) -> SessionState) {
        mutableState.update(transform)
        onStateChanged()
    }

    private fun onStateChanged() {
        val current = mutableState.value
        val syncKey = listOf(current.initialized, current.mode, current.syncState?.baseUrl, current.syncState?.status)
        if (syncKey != catalogSyncKey) {
            catalogSyncKey = syncKey
            catalogSyncJob?.cancel()
            catalogSyncJob = scope.launch { pullCatalogIfMissing() }
        }
        val authKey = listOf(current.users, current.mode, current.requiresPinForSwitch)
        if (authKey != authorizersKey) {
            authorizersKey = authKey
            authorizersJob?.cancel()
            authorizersJob = scope.launch { refreshSwitchAuthorizers() }
        }
    }

    // When production is enrolled but catalog/operators are empty, pull bootstrap once.
    private suspend fun pullCatalogIfMissing() {
        val current = mutableState.value
        if (!current.initialized) return
        if (current.mode != RuntimeMode.PRODUCTION) return
        if (current.syncState?.baseUrl.isNullOrBlank() || current.syncState?.status == "REQUIRES_REENROLLMENT") return
        if (productionCatalogSyncAttempted) return
        if (current.users.isNotEmpty() && current.products.isNotEmpty()) return
        // URL can arrive before tokens; syncing then makes reload treat the session as orphan and undo enrollment.
        val hasDeviceSession = withContext(io) { env.hasAccessToken() }
        if (!hasDeviceSession) return
        productionCatalogSyncAttempted = true
        showNotice("Sincronizando con el servidor…")
        showNotice(withContext(io) { env.runForegroundSync() })
        reload()
    }

    // Fetch active managers across DBs for mode switch authorization.
    private suspend fun refreshSwitchAuthorizers() {
        val authorizers = if (mutableState.value.requiresPinForSwitch) {
            withContext(io) { env.modeSwitchAuthorizers() }
        } else {
            emptyList()
        }
        mutate { it.copy(switchAuthorizers = authorizers) }
    }

    // Throttled silent release check for the mode-banner notice (no auto-install).
    private suspend fun checkForUpdate() {
        val cached = env.cachedUpdateVersionName()
        mutate { it.copy(availableUpdateVersionName = cached) }
        when (val outcome = withContext(io) { env.checkForUpdate() }) {
            is ReleaseCheckOutcome.UpdateAvailable ->
                mutate { it.copy(availableUpdateVersionName = outcome.remote.versionName) }
            is ReleaseCheckOutcome.UpToDate -> mutate { it.copy(availableUpdateVersionName = null) }
            is ReleaseCheckOutcome.Failed -> Unit
        }
    }

    // Switches between training and production; returns false when the switch is refused.
    suspend fun switchMode(target: RuntimeMode, authorizer: LocalUserEntity?, pin: String?): Boolean {
        val current = mutableState.value
        if (current.shift != null) {
            showNotice("Cierra el turno antes de cambiar de modo")
            return false
        }
        if (current.requiresPinForSwitch) {
            if (authorizer == null || pin.isNullOrBlank()) {
                showNotice("Selecciona un perfil de Gerente o Administrador")
                return false
            }
            val valid = withContext(io) { env.authenticateModeSwitch(authorizer.id, pin) }
            if (!valid) return false
        }
        // The first reload after a switch keeps the orphan-session check of the mode being left.
        val previousMode = current.mode
        mutate { it.copy(initialized = false) }
        withContext(io) {
            if (target == RuntimeMode.SANDBOX) env.enterTrainingMode() else env.exitTrainingMode()
        }
        val newMode = env.currentMode()
        data = env.dataSource(newMode)
        mutate { it.copy(mode = newMode, shift = null, notice = null) }
        observeActiveData()
        reload(orphanCheckMode = previousMode)
        return true
    }

    // Restores the training template; refused while a shift is open.
    fun resetTrainingDemo(): Job = scope.launch {
        if (mutableState.value.shift != null) {
            showNotice("Cierra el turno antes de reiniciar los datos demo")
            return@launch
        }
        mutate { it.copy(initialized = false) }
        try {
            withContext(io) { env.resetTrainingPlayground() }
            showNotice("✓ Datos de capacitación reiniciados correctamente")
        } catch (_: Exception) {
            showNotice("No se pudieron reiniciar los datos de capacitación")
        } finally {
            mutate { it.copy(shift = null) }
            reload()
        }
    }

    // Runs push+pull now and shows its outcome; the waiting screen also marks bootstrap as attempted.
    fun syncNow(markCatalogSyncAttempted: Boolean = false): Job = scope.launch {
        if (markCatalogSyncAttempted) productionCatalogSyncAttempted = true
        showNotice("Sincronizando con el servidor…")
        showNotice(withContext(io) { env.runForegroundSync() })
        reload()
    }

    // Operator asked to wipe the session and enroll this tablet again.
    fun requestReenrollment(): Job = scope.launch {
        withContext(io) { requireReenrollment("Reenrolamiento solicitado desde la tablet.") }
        mutate { it.copy(enrollError = null, notice = null) }
        productionCatalogSyncAttempted = false
        reload()
    }

    // Enrolls with a one-time code; ignored while a previous attempt is still running.
    fun enroll(code: String, name: String): Job? {
        if (mutableState.value.enrolling) return null
        mutate { it.copy(enrolling = true) }
        return scope.launch {
            mutate { it.copy(enrollError = null) }
            try {
                withContext(io) { env.enroll(code, name) }
                productionCatalogSyncAttempted = false
                env.enqueueSync()
                reload()
            } catch (e: Exception) {
                val userMessage = PosApiUserMessages.from(e)
                val stillNeedsEnrollment = withContext(io) { env.needsEnrollment() }
                if (stillNeedsEnrollment) {
                    // Code rejected or session wiped: stay on the form and say why.
                    mutate { it.copy(enrollError = userMessage) }
                } else {
                    // Tokens are saved; bootstrap can retry off this screen and the message stays visible.
                    showNotice(userMessage)
                    env.enqueueSync()
                    reload()
                }
            } finally {
                mutate { it.copy(enrolling = false) }
            }
        }
    }

    fun onShiftOpened(shift: ShiftEntity) = mutate { it.copy(shift = shift) }

    fun onShiftClosed() = mutate { it.copy(shift = null) }

    fun showNotice(message: String?) = mutate { it.copy(notice = message) }

    private data class LoadSnapshot(
        val syncState: SyncStateEntity?,
        val users: List<LocalUserEntity>,
        val products: List<ProductEntity>,
        val shift: ShiftEntity?,
        val hasAccessToken: Boolean,
    )
}
