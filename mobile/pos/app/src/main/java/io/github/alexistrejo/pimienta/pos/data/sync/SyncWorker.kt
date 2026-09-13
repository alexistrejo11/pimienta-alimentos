package io.github.alexistrejo.pimienta.pos.data.sync

import android.content.Context
import io.github.alexistrejo.pimienta.pos.BuildConfig
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
import io.github.alexistrejo.pimienta.pos.data.telemetry.PosTelemetryLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import java.time.Instant
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

private const val UNIQUE_SYNC = "pos-sync"

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val provider = PosDatabaseProvider(appContext)
    private val db get() = provider.database(RuntimeMode.PRODUCTION)
    private val credentials = DeviceCredentials(appContext)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val telemetryLogger = PosTelemetryLogger(db)

    override suspend fun doWork(): Result {
        if (provider.modes.mode() != RuntimeMode.PRODUCTION) return Result.success()
        val state = db.syncDao().state() ?: return Result.success()
        val baseUrl = state.baseUrl?.trim()?.let { if (it.endsWith("/")) it else "$it/" } ?: return Result.success()
        val device = db.operationsDao().device() ?: return Result.success()
        // Tokens gone but URL still set: force enrollment UI instead of a silent no-op.
        if (credentials.access() == null) {
            ProvisioningRepository(applicationContext, provider)
                .resetForReenrollment("Sesión del dispositivo inválida. Vuelve a enrolar.")
            return Result.failure()
        }
        val api = api(baseUrl)
        return try {
            val events = db.syncDao().eligible(System.currentTimeMillis(), 50)
            if (events.isNotEmpty()) {
                val siteId = device.siteId ?: return Result.success()
                val response = api.events(EventsRequest(events.map { it.toEnvelope(device.id, siteId, json) }))
                response.results.forEach { result ->
                    when (result.status.uppercase()) {
                        "ACCEPTED", "DUPLICATE", "REQUIRES_REVIEW" -> db.syncDao().terminal(result.eventId, "ACKNOWLEDGED", result.status, result.incidentId, result.message)
                        "REJECTED" -> db.syncDao().terminal(result.eventId, "BLOCKED", result.status, result.incidentId, result.message)
                        else -> db.syncDao().retry(result.eventId, System.currentTimeMillis() + backoff(1), result.message ?: "unknown result")
                    }
                }
            }
            val current = db.syncDao().state() ?: state
            val localIncomplete = db.productDao().count() == 0 || db.userDao().count() == 0
            if (current.changesCursor == null || localIncomplete) {
                ProvisioningRepository(applicationContext, provider).applyBootstrap(api.bootstrap())
            } else {
                val changes = api.changes(current.changesCursor)
                ProvisioningRepository(applicationContext, provider).applyChanges(changes)
            }
            uploadTelemetry(api, device.id, device.siteId, state)
            db.syncDao().saveState((db.syncDao().state() ?: state).copy(lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
            Result.success()
        } catch (e: HttpException) {
            recordDiagnostic("ERROR", "sync_http_failure", "POS sync HTTP ${e.code()}")
            if (e.code() == 401) {
                val refresh = credentials.refresh()
                if (refresh == null) {
                    ProvisioningRepository(applicationContext, provider)
                        .resetForReenrollment("Sesión expirada. Vuelve a enrolar el dispositivo.")
                    return Result.failure()
                }
                try {
                    val token = refreshApi(baseUrl).refresh(RefreshRequest(refresh))
                    credentials.save(token.accessToken, token.refreshToken)
                    Result.retry()
                } catch (_: Exception) {
                    ProvisioningRepository(applicationContext, provider)
                        .resetForReenrollment("La sesión del dispositivo expiró o fue revocada. Vuelve a enrolar.")
                    Result.failure()
                }
            } else if (e.code() == 403) {
                ProvisioningRepository(applicationContext, provider)
                    .resetForReenrollment("Este dispositivo fue revocado. Genera un código nuevo en la Web Central.")
                Result.failure()
            } else if (e.code() == 409) {
                // Invalid sync cursor: replace local catalog snapshot from a fresh bootstrap.
                try {
                    ProvisioningRepository(applicationContext, provider).applyBootstrap(api(baseUrl).bootstrap())
                    db.syncDao().saveState((db.syncDao().state() ?: state).copy(lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
                    Result.success()
                } catch (bootstrapError: Exception) {
                    db.syncDao().saveState(state.copy(status = "RETRYING", lastError = bootstrapError.message ?: "bootstrap after cursor 409 failed"))
                    Result.retry()
                }
            } else {
                db.syncDao().saveState(state.copy(status = "RETRYING", lastError = e.message()))
                Result.retry()
            }
        } catch (e: Exception) {
            recordDiagnostic("ERROR", "sync_failure", "POS sync ${e.javaClass.simpleName}")
            db.syncDao().saveState(state.copy(status = "RETRYING", lastError = e.message ?: e.javaClass.simpleName))
            Result.retry()
        }
    }

    private fun api(baseUrl: String): DeviceApi {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val token = credentials.access()
            val request = chain.request().newBuilder().apply { if (token != null) header("Authorization", "Bearer $token") }.build()
            chain.proceed(request)
        }).build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(DeviceApi::class.java)
    }
    private fun refreshApi(baseUrl: String): DeviceApi = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(DeviceApi::class.java)

    // Records a diagnostic only when the local telemetry database is healthy.
    private fun recordDiagnostic(level: String, type: String, message: String) {
        runCatching { telemetryLogger.record(level, type, message) }
    }

    // Uploads diagnostics separately from business events so telemetry cannot block sync.
    private suspend fun uploadTelemetry(api: DeviceApi, deviceId: String, siteId: String?, state: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity) {
        try {
            val rows = db.operationsDao().pendingTelemetry(50)
            val now = System.currentTimeMillis()
            val oldest = db.operationsDao().oldestPendingEventAt()?.let { ((now - it).coerceAtLeast(0) / 1000) } ?: 0
            val batch = TelemetryBatchRequest(
                events = rows.map {
                    TelemetryLogDto(it.schemaVersion, it.eventType, it.level, it.message, it.stack, Instant.ofEpochMilli(it.occurredAtEpochMillis).toString())
                },
                health = TelemetryHealthDto(state.status, db.operationsDao().pendingEventCount(), oldest, BuildConfig.VERSION_NAME),
            )
            api.telemetry(batch)
            if (rows.isNotEmpty()) db.operationsDao().deleteTelemetry(rows.map { it.id })
        } catch (_: Exception) {
            // Keep the local queue for a later connected worker attempt.
        }
    }

    private fun backoff(attempt: Int) = minOf(TimeUnit.HOURS.toMillis(6), TimeUnit.MINUTES.toMillis(1L shl minOf(attempt, 8)))

    companion object {
        fun cancel(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(UNIQUE_SYNC)
            wm.cancelUniqueWork(UNIQUE_SYNC + "-periodic")
        }

        fun enqueue(context: Context) {
            val wm = WorkManager.getInstance(context)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            wm.enqueueUniqueWork(UNIQUE_SYNC, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
            wm.enqueueUniquePeriodicWork(UNIQUE_SYNC + "-periodic", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
        }
    }
}

// Maps a stored outbox row to the wire envelope, preferring persisted device/site/shift metadata.
private fun OutboxEventEntity.toEnvelope(defaultDeviceId: String, defaultSiteId: String, json: Json): EventEnvelope {
    val payload = payloadJson?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        ?: buildJsonObject { put("aggregateId", aggregateId) }
    return EventEnvelope(
        id, type, schemaVersion, deviceId ?: defaultDeviceId, siteId ?: defaultSiteId, sequence,
        aggregateId, shiftId, Instant.ofEpochMilli(occurredAtEpochMillis).toString(), payload
    )
}
