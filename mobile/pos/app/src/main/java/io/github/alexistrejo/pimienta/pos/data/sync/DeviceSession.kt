package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Single source of truth for the device's API session.
 * Obtain the process-wide instance with `context.deviceSession()`; do not construct ad hoc.
 *
 * Owns:
 *   - Access/refresh token read/write (via [DeviceTokenStore]).
 *   - Construction of authenticated/unauthenticated [DeviceApi] clients (via [DeviceApiFactory]).
 *   - The 401 → refresh → persist new tokens → retry flow.
 *   - Process-wide serialization of the refresh call so a worker and the foreground
 *     sync button can both encounter a 401 without racing on refresh-token rotation.
 *
 * Explicitly does NOT own:
 *   - Reenrollment side-effects (clearing catalog, writing `REQUIRES_REENROLLMENT`).
 *     Those remain in [ProvisioningRepository.resetForReenrollment] because they
 *     cross the sync/catalog boundary; the session only reports the classification.
 *   - POS synchronization (outbox push, delta cursor, bootstrap). That belongs to
 *     [PosSyncPipeline] / [ProvisioningRepository].
 */
class DeviceSession(
    private val tokens: DeviceTokenStore,
) {
    // -------- Token accessors ------------------------------------------------

    fun access(): String? = tokens.access()
    fun refresh(): String? = tokens.refresh()
    fun saveTokens(access: String, refresh: String) = tokens.save(access, refresh)
    fun clearTokens() = tokens.clear()

    // -------- API client construction ---------------------------------------

    /**
     * Authenticated client. The Authorization header is pulled from [access] on every
     * request, so the same instance can keep being used after a token rotation.
     */
    fun authenticatedApi(baseUrl: String): DeviceApi =
        DeviceApiFactory.create(baseUrl) { tokens.access() }

    /** Client with no Authorization header; use for enroll and refresh calls. */
    fun unauthenticatedApi(baseUrl: String): DeviceApi =
        DeviceApiFactory.createUnauthenticated(baseUrl)

    // -------- Refresh / retry -----------------------------------------------

    /**
     * Rotates the access token using the stored refresh token.
     *
     * Process-wide [refreshMutex] guarantees that a worker and a foreground sync
     * encountering 401 simultaneously will issue exactly one refresh request between
     * them. The second caller observes the rotation via [TokenRotationOutcome.AlreadyFresh]
     * and can retry its original request against the freshly rotated access token.
     *
     * Persistence happens inside [NonCancellable]: once the server has rotated the
     * refresh token on its side, this process must save the new pair even if the
     * caller's coroutine is cancelled right now — otherwise we would leak a dead refresh.
     *
     * Classification of the final error mirrors [DeviceSessionPolicy]:
     *   - no refresh token stored        → [TokenRotationOutcome.MissingRefresh]
     *   - refresh HTTP 401/403           → [TokenRotationOutcome.ReenrollmentRequired]
     *   - any other refresh failure      → [TokenRotationOutcome.Transient]
     *
     * [staleAccess] lets callers tell "I started my work with access X" from "someone
     * else already rotated past X"; passing `null` disables the already-fresh shortcut.
     */
    suspend fun rotateAccessToken(baseUrl: String, staleAccess: String?): TokenRotationOutcome {
        return try {
            refreshMutex.withLock {
                val current = tokens.access()
                if (staleAccess != null && current != null && current != staleAccess) {
                    return@withLock TokenRotationOutcome.AlreadyFresh
                }
                val refresh = tokens.refresh() ?: return@withLock TokenRotationOutcome.MissingRefresh
                // Cancellation after the server rotated the refresh token must still persist the pair.
                withContext(NonCancellable) {
                    val token = unauthenticatedApi(baseUrl).refresh(RefreshRequest(refresh))
                    tokens.save(token.accessToken, token.refreshToken)
                }
                TokenRotationOutcome.Rotated
            }
        } catch (error: Exception) {
            if (DeviceSessionPolicy.refreshFailureRequiresReenrollment(error)) {
                TokenRotationOutcome.ReenrollmentRequired(error)
            } else {
                TokenRotationOutcome.Transient(error)
            }
        }
    }

    /**
     * Runs [block] against an authenticated [DeviceApi] and, on a 401, rotates the token
     * once and retries exactly once. This is the only "one-shot" auth wrapper intended
     * for request paths that are a single logical call (catalog edits, provisioning
     * utility calls). [PosSyncPipeline]'s multi-step run uses [rotateAccessToken] directly
     * so it can re-enter its full outbox-push + catalog-pull loop after a refresh.
     *
     * Does NOT clear the local session or write `REQUIRES_REENROLLMENT` on refresh
     * failure; it surfaces [AuthenticatedCallOutcome.RefreshFailed] with
     * `requiresReenrollment=true` and lets the caller decide whether its particular
     * code path should trigger a reset (e.g. catalog sync does, individual product
     * edits do not — matching pre-Phase-2 behavior).
     */
    suspend fun <T> withAuth(
        baseUrl: String,
        block: suspend (DeviceApi) -> T,
    ): AuthenticatedCallOutcome<T> {
        if (tokens.access() == null) return AuthenticatedCallOutcome.MissingAccess
        val api = authenticatedApi(baseUrl)
        return try {
            AuthenticatedCallOutcome.Success(block(api))
        } catch (e: HttpException) {
            if (e.code() != 401) return AuthenticatedCallOutcome.CallFailed(e)
            val stale = tokens.access()
            when (val rotation = rotateAccessToken(baseUrl, stale)) {
                TokenRotationOutcome.Rotated,
                TokenRotationOutcome.AlreadyFresh -> try {
                    AuthenticatedCallOutcome.Success(block(authenticatedApi(baseUrl)))
                } catch (retry: Exception) {
                    AuthenticatedCallOutcome.CallFailed(retry)
                }
                TokenRotationOutcome.MissingRefresh -> AuthenticatedCallOutcome.MissingRefresh
                is TokenRotationOutcome.ReenrollmentRequired ->
                    AuthenticatedCallOutcome.RefreshFailed(rotation.error, requiresReenrollment = true)
                is TokenRotationOutcome.Transient ->
                    AuthenticatedCallOutcome.RefreshFailed(rotation.error, requiresReenrollment = false)
            }
        } catch (e: Exception) {
            AuthenticatedCallOutcome.CallFailed(e)
        }
    }

    companion object {
        /**
         * Process-wide so every pipeline/repository instance (worker, foreground
         * button, UI-triggered catalog edit) shares one refresh request at a time.
         * Public to [internal] callers only in tests via same-package visibility.
         */
        internal val refreshMutex = Mutex()
    }
}

/** Outcome of [DeviceSession.rotateAccessToken]. */
sealed class TokenRotationOutcome {
    /** This caller performed the refresh and persisted the new tokens. */
    object Rotated : TokenRotationOutcome()
    /** Another coroutine already rotated past the stale access token this caller held. */
    object AlreadyFresh : TokenRotationOutcome()
    /** No refresh token is stored; caller must trigger reenrollment. */
    object MissingRefresh : TokenRotationOutcome()
    /** Refresh endpoint rejected the token (401/403); caller must trigger reenrollment. */
    data class ReenrollmentRequired(val error: Throwable) : TokenRotationOutcome()
    /** Network/5xx/other transient failure; caller should retry later without wiping state. */
    data class Transient(val error: Throwable) : TokenRotationOutcome()
}

/** Outcome of [DeviceSession.withAuth]. */
sealed class AuthenticatedCallOutcome<out T> {
    data class Success<T>(val value: T) : AuthenticatedCallOutcome<T>()
    /** `block` raised a non-401 exception, or raised after a successful refresh retry. */
    data class CallFailed(val error: Throwable) : AuthenticatedCallOutcome<Nothing>()
    /** No access token was stored before the call; caller must prompt enrollment. */
    object MissingAccess : AuthenticatedCallOutcome<Nothing>()
    /** 401 happened and no refresh token was stored; caller must prompt enrollment. */
    object MissingRefresh : AuthenticatedCallOutcome<Nothing>()
    /** Refresh itself failed; `requiresReenrollment` is true for 401/403 refresh responses. */
    data class RefreshFailed(
        val error: Throwable,
        val requiresReenrollment: Boolean,
    ) : AuthenticatedCallOutcome<Nothing>()
}
