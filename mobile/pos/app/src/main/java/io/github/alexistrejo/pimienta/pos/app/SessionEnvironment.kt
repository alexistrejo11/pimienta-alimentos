package io.github.alexistrejo.pimienta.pos.app

import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import kotlinx.coroutines.flow.Flow

// Reads and observes the active database the session screens are routed from.
interface SessionDataSource {
    fun syncState(): SyncStateEntity?
    fun users(): List<LocalUserEntity>
    fun products(): List<ProductEntity>
    fun activeShift(): ShiftEntity?
    fun observeProducts(): Flow<List<ProductEntity>>
    fun observeUsers(): Flow<List<LocalUserEntity>>
    fun observePolicy(): Flow<PosPolicyEntity?>
    fun observeSyncState(): Flow<SyncStateEntity?>
    fun observePendingEvents(): Flow<Int>
    fun observeActiveShift(): Flow<ShiftEntity?>
}

/**
 * Existing application services the [SessionCoordinator] orchestrates.
 * Each method delegates to the service that already owns the behavior; the port exists so
 * the coordinator's state machine can be unit-tested without Room, Keystore, or WorkManager.
 * Blocking methods are called by the coordinator on its IO dispatcher.
 */
interface SessionEnvironment {
    val isDebug: Boolean
    fun currentMode(): RuntimeMode
    fun dataSource(mode: RuntimeMode): SessionDataSource
    suspend fun awaitActiveDatabaseReady()

    fun hasAccessToken(): Boolean
    fun hasRefreshToken(): Boolean
    fun isProductionEnrolled(): Boolean

    suspend fun enroll(code: String, name: String)
    fun needsEnrollment(): Boolean
    fun resetForReenrollment(reason: String)

    fun enqueueSync()
    suspend fun runForegroundSync(): String

    fun enterTrainingMode()
    fun exitTrainingMode()
    fun resetTrainingPlayground()
    fun modeSwitchAuthorizers(): List<LocalUserEntity>
    fun authenticateModeSwitch(userId: String, pin: String): Boolean

    fun cachedUpdateVersionName(): String?
    suspend fun checkForUpdate(): ReleaseCheckOutcome
}
