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
     * True when a base URL was saved and both device tokens are gone.
     * A row already marked for re-enrollment must not be wiped again.
     */
    fun orphanAccessRequiresReset(
        status: String?,
        hasBaseUrl: Boolean,
        hasAccess: Boolean,
        hasRefresh: Boolean,
    ): Boolean = hasBaseUrl && !hasAccess && !hasRefresh && status != "REQUIRES_REENROLLMENT"

    /**
     * Enrollment is only the intermediate status written before bootstrap.
     * ONLINE and RETRYING still have a session; sending those back to the form
     * makes the tablet re-submit a code the server already consumed.
     */
    fun showsEnrollmentForm(status: String?, hasBaseUrl: Boolean, hasAccessToken: Boolean): Boolean {
        if (!hasBaseUrl || !hasAccessToken) return true
        return status == "REQUIRES_REENROLLMENT"
    }

    /** False after reset cleared the URL, so a late sync cannot erase the reason shown on screen. */
    fun syncMayUpdateSession(status: String?, baseUrl: String?): Boolean =
        !baseUrl.isNullOrBlank() && status != "REQUIRES_REENROLLMENT"

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
