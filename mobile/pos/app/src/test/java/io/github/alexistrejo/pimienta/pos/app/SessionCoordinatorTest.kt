package io.github.alexistrejo.pimienta.pos.app

import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.sync.DeviceSessionPolicy
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.update.PosApkReleaseDto
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// State-machine tests for SessionCoordinator against an in-memory environment.
class SessionCoordinatorTest {

    @Test
    fun startupLoadsSnapshotAndRoutesEnrolledProductionToAccess() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        coordinator.start()
        settle()

        val state = coordinator.state.value
        assertTrue(state.initialized)
        assertEquals(SessionDestination.Access, state.destination)
        assertTrue(env.resets.isEmpty())
        assertEquals(0, env.foregroundSyncs)
    }

    @Test
    fun configuredUrlWithoutAnyTokenResetsForReenrollment() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.access = null
        env.refresh = null
        env.production.sync.value = SyncStateEntity(baseUrl = "https://api", status = "ONLINE", lastError = null)

        coordinator.start()
        settle()

        assertEquals(listOf(DeviceSessionPolicy.orphanAccessTokenMessage()), env.resets)
        assertEquals(SessionDestination.Enrollment, coordinator.state.value.destination)
    }

    @Test
    fun refreshTokenWithoutAccessEnqueuesSyncInsteadOfResetting() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.access = null

        coordinator.start()
        settle()

        assertTrue(env.resets.isEmpty())
        assertTrue(env.syncEnqueued >= 1)
    }

    @Test
    fun enrolledProductionWithEmptyCatalogPullsBootstrapOnlyOnce() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.production.users.value = emptyList()

        coordinator.start()
        settle()
        coordinator.reload()
        settle()

        assertEquals(1, env.foregroundSyncs)
        assertEquals(env.foregroundSyncResult, coordinator.state.value.notice)
        val destination = coordinator.state.value.destination
        assertTrue(destination is SessionDestination.WaitingForCatalog)
        assertEquals(env.foregroundSyncResult, (destination as SessionDestination.WaitingForCatalog).message)
    }

    @Test
    fun sandboxModeSkipsOrphanCheckAndCatalogPull() = withCoordinator(FakeEnvironment().apply { mode = RuntimeMode.SANDBOX; access = null; refresh = null }) { env, coordinator ->
        env.sandbox.sync.value = SyncStateEntity(baseUrl = "https://api", status = "ONLINE")

        coordinator.start()
        settle()

        assertTrue(env.resets.isEmpty())
        assertEquals(0, env.foregroundSyncs)
        assertTrue(coordinator.state.value.destination is SessionDestination.WaitingForCatalog)
    }

    @Test
    fun successfulEnrollmentEnqueuesSyncAndLeavesTheForm() = withCoordinator(FakeEnvironment().notEnrolled()) { env, coordinator ->
        coordinator.start()
        settle()
        assertEquals(SessionDestination.Enrollment, coordinator.state.value.destination)

        coordinator.enroll("CODE", "Caja 1")
        settle()

        val state = coordinator.state.value
        assertFalse(state.enrolling)
        assertNull(state.enrollError)
        assertFalse(state.showEnrollment)
        assertTrue(env.syncEnqueued >= 1)
    }

    @Test
    fun rejectedEnrollmentStaysOnFormWithUserMessage() = withCoordinator(FakeEnvironment().notEnrolled()) { env, coordinator ->
        env.enrollFailure = IllegalStateException("Código inválido")
        coordinator.start()
        settle()

        coordinator.enroll("BAD", "Caja 1")
        settle()

        val state = coordinator.state.value
        assertFalse(state.enrolling)
        assertEquals(PosApiUserMessages.from(env.enrollFailure!!), state.enrollError)
        assertEquals(SessionDestination.Enrollment, state.destination)
    }

    @Test
    fun secondEnrollWhileBusyIsIgnored() = withCoordinator(FakeEnvironment().notEnrolled()) { env, coordinator ->
        coordinator.start()
        settle()

        val first = coordinator.enroll("CODE", "Caja 1")
        val second = coordinator.enroll("CODE", "Caja 1")
        settle()

        assertTrue(first != null)
        assertNull(second)
        assertEquals(1, env.enrollCalls)
    }

    @Test
    fun manualReenrollmentResetsSessionAndClearsMessages() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.production.users.value = emptyList()
        coordinator.start()
        settle()

        coordinator.requestReenrollment()
        settle()

        assertEquals(listOf("Reenrolamiento solicitado desde la tablet."), env.resets)
        val state = coordinator.state.value
        assertNull(state.notice)
        assertNull(state.enrollError)
        assertEquals(SessionDestination.Enrollment, state.destination)
    }

    @Test
    fun modeSwitchIsRefusedWhileShiftIsOpen() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.production.shift.value = shift()
        coordinator.start()
        settle()

        val switched = coordinator.switchMode(RuntimeMode.SANDBOX, null, null)

        assertFalse(switched)
        assertEquals("Cierra el turno antes de cambiar de modo", coordinator.state.value.notice)
        assertEquals(RuntimeMode.PRODUCTION, coordinator.state.value.mode)
        assertEquals(0, env.modeChanges)
    }

    @Test
    fun releaseBuildRequiresManagerPinToSwitchMode() = withCoordinator(FakeEnvironment().enrolledWithCatalog().apply { isDebug = false }) { env, coordinator ->
        coordinator.start()
        settle()
        assertTrue(coordinator.state.value.requiresPinForSwitch)
        assertEquals(listOf(manager), coordinator.state.value.switchAuthorizers)

        assertFalse(coordinator.switchMode(RuntimeMode.SANDBOX, null, null))
        assertEquals("Selecciona un perfil de Gerente o Administrador", coordinator.state.value.notice)

        env.pinAccepted = false
        assertFalse(coordinator.switchMode(RuntimeMode.SANDBOX, manager, "0000"))
        assertEquals(RuntimeMode.PRODUCTION, coordinator.state.value.mode)
        assertEquals(0, env.modeChanges)
    }

    @Test
    fun successfulSwitchMovesToTheOtherDatabase() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.sandbox.users.value = listOf(cashier)
        env.sandbox.products.value = emptyList()
        coordinator.start()
        settle()

        val switched = coordinator.switchMode(RuntimeMode.SANDBOX, null, null)
        settle()

        val state = coordinator.state.value
        assertTrue(switched)
        assertEquals(RuntimeMode.SANDBOX, state.mode)
        assertTrue(state.initialized)
        assertNull(state.notice)
        assertEquals(listOf(cashier), state.users)
        assertTrue(state.products.isEmpty())
    }

    @Test
    fun availableReleaseIsSurfacedForTheBanner() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        env.updateOutcome = ReleaseCheckOutcome.UpdateAvailable(PosApkReleaseDto("2.3.0", 230, ""))

        coordinator.start()
        settle()

        assertEquals("2.3.0", coordinator.state.value.availableUpdateVersionName)
    }

    @Test
    fun liveShiftUpdatesMoveBetweenAccessAndSale() = withCoordinator(FakeEnvironment().enrolledWithCatalog()) { env, coordinator ->
        coordinator.start()
        settle()

        val open = shift()
        env.production.shift.value = open
        settle()
        assertEquals(SessionDestination.Sale(open), coordinator.state.value.destination)

        coordinator.onShiftClosed()
        assertEquals(SessionDestination.Access, coordinator.state.value.destination)
    }

    // --- harness ---------------------------------------------------------------

    private fun withCoordinator(
        env: FakeEnvironment,
        block: suspend CoroutineScope.(FakeEnvironment, SessionCoordinator) -> Unit,
    ) = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val coordinator = SessionCoordinator(scope, env, io = Dispatchers.Unconfined)
        try {
            block(env, coordinator)
        } finally {
            scope.cancel()
        }
    }

    private suspend fun settle() = repeat(50) { yield() }

    private class FakeData : SessionDataSource {
        val sync = MutableStateFlow<SyncStateEntity?>(null)
        val users = MutableStateFlow<List<LocalUserEntity>>(emptyList())
        val products = MutableStateFlow<List<ProductEntity>>(emptyList())
        val policy = MutableStateFlow<PosPolicyEntity?>(null)
        val pending = MutableStateFlow(0)
        val shift = MutableStateFlow<ShiftEntity?>(null)

        override fun syncState() = sync.value
        override fun users() = users.value
        override fun products() = products.value
        override fun activeShift() = shift.value
        override fun observeProducts(): Flow<List<ProductEntity>> = products
        override fun observeUsers(): Flow<List<LocalUserEntity>> = users
        override fun observePolicy(): Flow<PosPolicyEntity?> = policy
        override fun observeSyncState(): Flow<SyncStateEntity?> = sync
        override fun observePendingEvents(): Flow<Int> = pending
        override fun observeActiveShift(): Flow<ShiftEntity?> = shift
    }

    private class FakeEnvironment : SessionEnvironment {
        override var isDebug = true
        var mode = RuntimeMode.PRODUCTION
        val production = FakeData()
        val sandbox = FakeData()
        var access: String? = "access"
        var refresh: String? = "refresh"
        var productionEnrolled = true
        val resets = mutableListOf<String>()
        var syncEnqueued = 0
        var foregroundSyncs = 0
        val foregroundSyncResult = "Sincronización completada."
        var enrollFailure: Exception? = null
        var enrollCalls = 0
        var pinAccepted = true
        var modeChanges = 0
        var updateOutcome: ReleaseCheckOutcome = ReleaseCheckOutcome.Failed("offline")

        fun enrolledWithCatalog() = apply {
            production.sync.value = SyncStateEntity(baseUrl = "https://api", status = "ONLINE")
            production.users.value = listOf(manager, cashier)
            production.products.value = listOf(product)
        }

        fun notEnrolled() = apply {
            access = null
            refresh = null
            productionEnrolled = false
        }

        private fun data(mode: RuntimeMode) = if (mode == RuntimeMode.PRODUCTION) production else sandbox

        override fun currentMode() = mode
        override fun dataSource(mode: RuntimeMode): SessionDataSource = data(mode)
        override suspend fun awaitActiveDatabaseReady() = Unit
        override fun hasAccessToken() = access != null
        override fun hasRefreshToken() = refresh != null
        override fun isProductionEnrolled() = productionEnrolled

        override suspend fun enroll(code: String, name: String) {
            enrollCalls++
            yield()
            enrollFailure?.let { throw it }
            access = "access"
            refresh = "refresh"
            production.sync.value = SyncStateEntity(baseUrl = "https://api", status = "ENROLLED")
        }

        override fun needsEnrollment(): Boolean {
            val state = production.sync.value
            return state?.baseUrl.isNullOrBlank() || state?.status == "REQUIRES_REENROLLMENT"
        }

        override fun resetForReenrollment(reason: String) {
            resets += reason
            access = null
            refresh = null
            production.users.value = emptyList()
            production.products.value = emptyList()
            production.sync.value = SyncStateEntity(lastError = reason, status = "REQUIRES_REENROLLMENT")
        }

        override fun enqueueSync() { syncEnqueued++ }
        override suspend fun runForegroundSync(): String {
            foregroundSyncs++
            return foregroundSyncResult
        }

        override fun enterTrainingMode() { modeChanges++; mode = RuntimeMode.SANDBOX }
        override fun exitTrainingMode() { modeChanges++; mode = RuntimeMode.PRODUCTION }
        override fun resetTrainingPlayground() = Unit
        override fun modeSwitchAuthorizers() = (production.users.value + sandbox.users.value)
            .filter { it.active && it.isManagerOrAdmin }
            .distinctBy { it.id }
        override fun authenticateModeSwitch(userId: String, pin: String) = pinAccepted

        override fun cachedUpdateVersionName(): String? = null
        override suspend fun checkForUpdate() = updateOutcome
    }

    private companion object {
        val manager = LocalUserEntity("m1", "Gerente", "MANAGER", "hash", true)
        val cashier = LocalUserEntity("c1", "Cajero", "CASHIER", "hash", true)
        val product = ProductEntity("p1", "SKU", null, "Agua", "Bebidas", "PZA", "12.50", "5.00", true, "10", "0", "UNLIMITED", null)

        fun shift() = ShiftEntity("s1", "d1", "site", "c1", 0, 0, "OPEN", 1)
    }
}
