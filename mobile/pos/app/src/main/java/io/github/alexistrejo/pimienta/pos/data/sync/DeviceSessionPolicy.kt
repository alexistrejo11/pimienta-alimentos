package io.github.alexistrejo.pimienta.pos.data.sync

import retrofit2.HttpException

/**
 * Decides when a device must re-enroll vs keep selling locally and retry sync later.
 * Only definitive auth failures (revoke / invalid refresh) wipe the session.
 */
object DeviceSessionPolicy {
    /** Device JWT rejected on a sync call and no refresh token is stored. */
    fun missingRefreshTokenMessage(): String =
        "Sesión expirada. Vuelve a enrolar el dispositivo."

    /** Refresh endpoint returned 401/403 — credentials are no longer valid. */
    fun invalidRefreshTokenMessage(): String =
        "La sesión del dispositivo expiró o fue revocada. Vuelve a enrolar."

    /** Sync call returned 403 — device was revoked in admin. */
    fun revokedDeviceMessage(): String =
        "Este dispositivo fue revocado. Genera un código nuevo en la Web Central."

    /** Access missing while URL is still configured but refresh may still exist. */
    fun orphanAccessTokenMessage(): String =
        "Sesión del dispositivo inválida. Vuelve a enrolar."

    /**
     * True when refresh failed because the server rejected device credentials.
     * Network, timeout, and 5xx must return false so bootstrap/catalog stay intact.
     */
    fun refreshFailureRequiresReenrollment(error: Throwable): Boolean =
        error is HttpException && (error.code() == 401 || error.code() == 403)

    /** True when a non-refresh HTTP response proves the device is no longer authorized. */
    fun httpResponseRequiresReenrollment(code: Int): Boolean = code == 403

    fun syncRetryMessage(error: Throwable): String = PosApiUserMessages.from(error)
}
