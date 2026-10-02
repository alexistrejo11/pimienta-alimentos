package io.github.alexistrejo.pimienta.pos.data.sync

import android.content.Context
import io.github.alexistrejo.pimienta.pos.BuildConfig
import io.github.alexistrejo.pimienta.pos.app.posDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.telemetry.PosTelemetryLogger
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Outcome of a foreground or worker POS sync attempt. */
enum class PosSyncNowOutcome {
    SUCCESS,
    RETRY,
    FAILURE,
    SKIPPED,
}

/**
 * Pushes the outbox and pulls catalog/operators without WorkManager.
 * The sale screen never waits on this; only the explicit sync button does.
 */
class PosSyncPipeline(private val context: Context) {
    // Shared process provider so catalog writes invalidate the Flows the sale screen collects.
    private val provider = context.posDatabaseProvider()
    private val db get() = provider.database(RuntimeMode.PRODUCTION)
    private val credentials = DeviceCredentials(context)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val telemetryLogger = PosTelemetryLogger(db)

    // Access token this run started with, to detect that another run already refreshed it.
    private var currentRunAccess: String? = null

    // Process-wide so every pipeline instance (worker, foreground button) shares one refresh at a time.
    private companion object {
        val refreshMutex = Mutex()
        // Two overlapping runs would requeue each other's in-flight events and race on their status.
        val runMutex = Mutex()
    }

    suspend fun run(): PosSyncNowOutcome = runMutex.withLock { runLocked(refreshedOnce = false) }

    private suspend fun runLocked(refreshedOnce: Boolean): PosSyncNowOutcome {
        if (provider.modes.mode() != RuntimeMode.PRODUCTION) return PosSyncNowOutcome.SKIPPED
        val state = db.syncDao().state() ?: return PosSyncNowOutcome.SKIPPED
        val baseUrl = state.baseUrl?.trim()?.let { if (it.endsWith("/")) it else "$it/" }
            ?: return PosSyncNowOutcome.SKIPPED
        val device = db.operationsDao().device() ?: return PosSyncNowOutcome.SKIPPED
        currentRunAccess = credentials.access()
        if (currentRunAccess == null) {
            return refreshAccess(state, baseUrl, refreshedOnce)
        }
        val api = api(baseUrl)
        var inFlightIds = emptyList<String>()
        var acceptedResults = emptyList<EventResult>()
        var eventsError: Exception? = null

        // Pushes pending outbox events; handles rejections or temporary push failures gracefully.
        try {
            db.syncDao().recoverInFlight(System.currentTimeMillis())
            db.syncDao().resetBackoffForSync()
            val events = db.syncDao().eligible(System.currentTimeMillis(), 50)
            if (events.isNotEmpty()) {
                inFlightIds = events.map { it.id }
                db.syncDao().markInFlight(inFlightIds)
                val siteId = device.siteId ?: run {
                    uploadTelemetry(api, device.id, null, state)
                    return PosSyncNowOutcome.SUCCESS
                }
                try {
                    val response = api.events(EventsRequest(events.map { it.toEnvelope(device.id, siteId, json) }))
                    acceptedResults = applyResults(events, response)
                } catch (e: HttpException) {
                    if ((e.code() == 400 || e.code() == 422) && events.size > 1) {
                        // One bad event must not reject the whole batch; isolate it by resending one at a time.
                        val isolated = pushOneByOne(api, events, device.id, siteId)
                        acceptedResults = isolated.first
                        eventsError = isolated.second
                    } else if (e.code() == 400 || e.code() == 422) {
                        rejectAlone(events.single(), e)
                    } else {
                        inFlightIds.forEach { db.syncDao().markFailedRetryable(it, System.currentTimeMillis() + backoff(1), e.message()) }
                        eventsError = e
                    }
                } catch (e: Exception) {
                    inFlightIds.forEach { db.syncDao().markFailedRetryable(it, System.currentTimeMillis() + backoff(1), e.message ?: "events push failed") }
                    eventsError = e
                }
            }
        } catch (e: Exception) {
            eventsError = e
        }

        // Pulls catalog changes or bootstrap snapshot so catalog freshness is not blocked by outbox errors.
        return try {
            val current = db.syncDao().state() ?: state
            val localIncomplete = db.productDao().count() == 0 || db.userDao().count() == 0
            val provisioning = ProvisioningRepository(context, provider)
            if (current.changesCursor == null || localIncomplete) {
                provisioning.applyBootstrap(api.bootstrap(), acceptedResults)
            } else {
                provisioning.applyChanges(api.changes(current.changesCursor), acceptedResults)
            }
            uploadTelemetry(api, device.id, device.siteId, state)
            val latestState = db.syncDao().state() ?: state
            db.syncDao().saveState(
                latestState.copy(
                    lastSuccessfulAtEpochMillis = System.currentTimeMillis(),
                    lastError = eventsError?.message,
                    status = if (eventsError == null) "ONLINE" else "RETRYING",
                ),
            )
            if (eventsError != null) PosSyncNowOutcome.RETRY else PosSyncNowOutcome.SUCCESS
        } catch (e: HttpException) {
            recordDiagnostic("ERROR", "sync_http_failure", "POS sync HTTP ${e.code()}")
            if (e.code() != 401 && e.code() != 403) {
                val retrying = (db.syncDao().state() ?: state).copy(status = "RETRYING")
                uploadTelemetry(api, device.id, device.siteId, retrying)
            }
            when {
                e.code() == 401 -> refreshAccess(state, baseUrl, refreshedOnce)
                e.code() == 403 -> {
                    ProvisioningRepository(context, provider)
                        .resetForReenrollment(DeviceSessionPolicy.revokedDeviceMessage())
                    PosSyncNowOutcome.FAILURE
                }
                PosSyncErrorPolicy.requiresBootstrap(e) -> {
                    try {
                        ProvisioningRepository(context, provider).applyBootstrap(api(baseUrl).bootstrap(), acceptedResults)
                        val latestState = db.syncDao().state() ?: state
                        db.syncDao().saveState(latestState.copy(lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
                        PosSyncNowOutcome.SUCCESS
                    } catch (bootstrapError: Exception) {
                        val latestState = db.syncDao().state() ?: state
                        db.syncDao().saveState(latestState.copy(status = "RETRYING", lastError = bootstrapError.message ?: "bootstrap after cursor 409 failed"))
                        PosSyncNowOutcome.RETRY
                    }
                }
                else -> {
                    val latestState = db.syncDao().state() ?: state
                    db.syncDao().saveState(latestState.copy(status = "RETRYING", lastError = e.message()))
                    PosSyncNowOutcome.RETRY
                }
            }
        } catch (e: Exception) {
            recordDiagnostic("ERROR", "sync_failure", "POS sync ${e.javaClass.simpleName}")
            val retrying = (db.syncDao().state() ?: state).copy(status = "RETRYING")
            uploadTelemetry(api, device.id, device.siteId, retrying)
            val latestState = db.syncDao().state() ?: state
            db.syncDao().saveState(latestState.copy(status = "RETRYING", lastError = e.message ?: e.javaClass.simpleName))
            PosSyncNowOutcome.RETRY
        }
    }

    // Rotates access then continues this same run so the sync button can await a real pull.
    private suspend fun refreshAccess(state: SyncStateEntity, baseUrl: String, refreshedOnce: Boolean): PosSyncNowOutcome {
        val staleAccess = currentRunAccess
        return try {
            // Worker and foreground sync can both hit 401; only one may rotate the refresh token.
            val refreshed = refreshMutex.withLock {
                if (credentials.access() != null && credentials.access() != staleAccess) return@withLock true
                val refresh = credentials.refresh() ?: return@withLock false
                // A cancelled job must still persist a pair the server already rotated.
                withContext(NonCancellable) {
                    val token = refreshApi(baseUrl).refresh(RefreshRequest(refresh))
                    credentials.save(token.accessToken, token.refreshToken)
                }
                true
            }
            if (!refreshed) {
                ProvisioningRepository(context, provider)
                    .resetForReenrollment(DeviceSessionPolicy.missingRefreshTokenMessage())
                return PosSyncNowOutcome.FAILURE
            }
            if (refreshedOnce) PosSyncNowOutcome.RETRY else runLocked(refreshedOnce = true)
        } catch (error: Exception) {
            if (DeviceSessionPolicy.refreshFailureRequiresReenrollment(error)) {
                ProvisioningRepository(context, provider)
                    .resetForReenrollment(DeviceSessionPolicy.invalidRefreshTokenMessage())
                PosSyncNowOutcome.FAILURE
            } else {
                db.syncDao().saveState(
                    (db.syncDao().state() ?: state).copy(
                        status = "RETRYING",
                        lastError = DeviceSessionPolicy.syncRetryMessage(error),
                    ),
                )
                PosSyncNowOutcome.RETRY
            }
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

    private fun refreshApi(baseUrl: String): DeviceApi =
        Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(DeviceApi::class.java)

    // Marks each event by its server result and returns the ones the server kept.
    private fun applyResults(events: List<OutboxEventEntity>, response: EventsResponse): List<EventResult> {
        val received = response.results.map { it.eventId }.toSet()
        response.results.forEach { result ->
            when (result.status.uppercase()) {
                "ACCEPTED", "DUPLICATE", "REQUIRES_REVIEW" -> Unit
                "REJECTED" -> db.syncDao().markRejected(result.eventId, result.message ?: "event rejected")
                else -> db.syncDao().markFailedRetryable(result.eventId, System.currentTimeMillis() + backoff(1), result.message ?: "unknown result")
            }
        }
        events.filter { it.id !in received }.forEach {
            db.syncDao().markFailedRetryable(it.id, System.currentTimeMillis() + backoff(1), "server did not return a result")
        }
        return response.results.filter { it.status.uppercase() in setOf("ACCEPTED", "DUPLICATE", "REQUIRES_REVIEW") }
    }

    // Sends events in sequence order; on a non-validation failure the rest stay queued for the next run.
    private suspend fun pushOneByOne(
        api: DeviceApi,
        events: List<OutboxEventEntity>,
        deviceId: String,
        siteId: String,
    ): Pair<List<EventResult>, Exception?> {
        val accepted = mutableListOf<EventResult>()
        val ordered = events.sortedBy { it.sequence }
        ordered.forEachIndexed { index, event ->
            try {
                val response = api.events(EventsRequest(listOf(event.toEnvelope(deviceId, siteId, json))))
                accepted += applyResults(listOf(event), response)
            } catch (e: HttpException) {
                if (e.code() == 400 || e.code() == 422) {
                    rejectAlone(event, e)
                } else {
                    retryRemaining(ordered.drop(index), e.message())
                    return accepted to e
                }
            } catch (e: Exception) {
                retryRemaining(ordered.drop(index), e.message ?: "events push failed")
                return accepted to e
            }
        }
        return accepted to null
    }

    private fun rejectAlone(event: OutboxEventEntity, e: HttpException) {
        db.syncDao().markRejected(event.id, "HTTP ${e.code()}: ${e.message()}")
        recordDiagnostic("ERROR", "sync_event_rejected", "POS event ${event.type} seq=${event.sequence} id=${event.id} rejected with HTTP ${e.code()}")
    }

    private fun retryRemaining(events: List<OutboxEventEntity>, message: String) {
        events.forEach { db.syncDao().markFailedRetryable(it.id, System.currentTimeMillis() + backoff(1), message) }
    }

    private fun recordDiagnostic(level: String, type: String, message: String) {
        runCatching { telemetryLogger.record(level, type, message) }
    }

    private suspend fun uploadTelemetry(api: DeviceApi, deviceId: String, siteId: String?, state: SyncStateEntity) {
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
}

// Maps a stored outbox row to the wire envelope, preferring persisted device/site/shift metadata.
internal fun OutboxEventEntity.toEnvelope(defaultDeviceId: String, defaultSiteId: String, json: Json): EventEnvelope {
    val payload = payloadJson?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        ?: buildJsonObject { put("aggregateId", aggregateId) }
    return EventEnvelope(
        id, type, schemaVersion, deviceId ?: defaultDeviceId, siteId ?: defaultSiteId, sequence,
        aggregateId, shiftId, Instant.ofEpochMilli(occurredAtEpochMillis).toString(), payload
    )
}
