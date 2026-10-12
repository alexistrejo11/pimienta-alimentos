package io.github.alexistrejo.pimienta.pos.app

import io.github.alexistrejo.pimienta.pos.BuildConfig
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.sync.PRODUCTION_API_URL
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
import io.github.alexistrejo.pimienta.pos.data.update.PosAppUpdater
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.domain.TrainingModePolicy

// Production wiring of the session ports onto the existing application services.
internal class AndroidSessionEnvironment(private val app: PosApplication) : SessionEnvironment {
    private val provider get() = app.databaseProvider
    private val session get() = app.deviceSession
    private val provisioning by lazy { ProvisioningRepository(app, provider) }
    private val updater by lazy { PosAppUpdater(app) }

    override val isDebug: Boolean = BuildConfig.DEBUG
    override fun currentMode(): RuntimeMode = provider.modes.mode()
    override fun dataSource(mode: RuntimeMode): SessionDataSource = RepositorySessionDataSource(PosRepository(provider, mode))
    override suspend fun awaitActiveDatabaseReady() = app.awaitActiveDatabaseReady()

    override fun hasAccessToken(): Boolean = session.access() != null
    override fun hasRefreshToken(): Boolean = session.refresh() != null
    override fun isProductionEnrolled(): Boolean = TrainingModePolicy.isEnrolled(provider)

    override suspend fun enroll(code: String, name: String) {
        provisioning.enroll(PRODUCTION_API_URL, code, name)
    }
    override fun needsEnrollment(): Boolean = provisioning.needsEnrollment()
    override fun resetForReenrollment(reason: String) = provisioning.resetForReenrollment(reason)

    override fun enqueueSync() = SyncWorker.enqueue(app)
    override suspend fun runForegroundSync(): String = runForegroundSync(app)

    override fun enterTrainingMode() = app.enterTrainingMode()
    override fun exitTrainingMode() = app.exitTrainingMode()
    override fun resetTrainingPlayground() = app.resetTrainingPlayground()

    override fun modeSwitchAuthorizers(): List<LocalUserEntity> {
        val prodUsers = PosRepository(provider, RuntimeMode.PRODUCTION).users()
        val activeUsers = PosRepository(provider).users()
        return (prodUsers + activeUsers)
            .filter { it.active && it.isManagerOrAdmin }
            .distinctBy { it.id }
    }

    override fun authenticateModeSwitch(userId: String, pin: String): Boolean =
        PosRepository(provider, RuntimeMode.PRODUCTION).authenticate(userId, pin) ||
            PosRepository(provider).authenticate(userId, pin)

    override fun cachedUpdateVersionName(): String? = updater.cachedUpdateVersionName()
    override suspend fun checkForUpdate(): ReleaseCheckOutcome = updater.check(force = false)
}

// Adapts the active-mode PosRepository reads/flows to the coordinator's data port.
private class RepositorySessionDataSource(private val repository: PosRepository) : SessionDataSource {
    override fun syncState() = repository.syncState()
    override fun users() = repository.users()
    override fun products() = repository.products()
    override fun activeShift() = repository.activeShift()
    override fun observeProducts() = repository.observeProducts()
    override fun observeUsers() = repository.observeUsers()
    override fun observePolicy() = repository.observePolicy()
    override fun observeSyncState() = repository.observeSyncState()
    override fun observePendingEvents() = repository.observePendingEvents()
    override fun observeActiveShift() = repository.observeActiveShift()
}
