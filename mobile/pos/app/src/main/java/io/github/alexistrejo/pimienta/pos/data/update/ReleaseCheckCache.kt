package io.github.alexistrejo.pimienta.pos.data.update

import android.content.Context

/**
 * Remembers the last throttled release check and a version hint for the mode banner.
 * Does not cache the pre-signed download URL (it expires).
 */
class ReleaseCheckCache(
    private val store: ReleaseCheckStore,
) {
    constructor(context: Context) : this(
        PrefsReleaseCheckStore(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        ),
    )

    fun lastCheckAtMillis(): Long = store.getLong(KEY_LAST_CHECK_AT, 0L)

    fun availableVersionCode(): Int = store.getInt(KEY_AVAILABLE_CODE, 0)

    fun availableVersionName(): String? = store.getString(KEY_AVAILABLE_NAME)

    fun hasCachedUpdate(localVersionCode: Int): Boolean =
        VersionCompare.isNewer(availableVersionCode(), localVersionCode)

    fun rememberCheck(
        checkedAtMillis: Long,
        remote: PosApkReleaseDto?,
        localVersionCode: Int,
    ) {
        store.edit {
            putLong(KEY_LAST_CHECK_AT, checkedAtMillis)
            if (remote != null && VersionCompare.isNewer(remote.versionCode, localVersionCode)) {
                putInt(KEY_AVAILABLE_CODE, remote.versionCode)
                putString(KEY_AVAILABLE_NAME, remote.versionName)
            } else {
                remove(KEY_AVAILABLE_CODE)
                remove(KEY_AVAILABLE_NAME)
            }
            apply()
        }
    }

    fun clearAvailable() {
        store.edit {
            remove(KEY_AVAILABLE_CODE)
            remove(KEY_AVAILABLE_NAME)
            apply()
        }
    }

    companion object {
        const val PREFS = "pos_apk_update"
        private const val KEY_LAST_CHECK_AT = "last_check_at_ms"
        private const val KEY_AVAILABLE_CODE = "available_version_code"
        private const val KEY_AVAILABLE_NAME = "available_version_name"

        /** Skip network when a recent successful/attempted check exists. */
        const val THROTTLE_MS: Long = 6L * 60L * 60L * 1000L
    }
}
