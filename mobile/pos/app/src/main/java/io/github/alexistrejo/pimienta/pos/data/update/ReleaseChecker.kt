package io.github.alexistrejo.pimienta.pos.data.update

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/**
 * Fetches the public latest APK release and compares it to the installed versionCode.
 * [force] bypasses the throttle window used for silent checks.
 */
class ReleaseChecker(
    private val cache: ReleaseCheckCache,
    private val localVersionCode: Int,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val apiFactory: (String) -> PosReleaseApi = ::defaultApi,
) {
    suspend fun check(baseUrl: String, force: Boolean = false): ReleaseCheckOutcome {
        val now = nowMillis()
        if (!force && now - cache.lastCheckAtMillis() < ReleaseCheckCache.THROTTLE_MS) {
            val cachedName = cache.availableVersionName()
            val cachedCode = cache.availableVersionCode()
            if (cachedName != null && VersionCompare.isNewer(cachedCode, localVersionCode)) {
                return ReleaseCheckOutcome.UpdateAvailable(
                    PosApkReleaseDto(
                        versionName = cachedName,
                        versionCode = cachedCode,
                        url = "",
                    ),
                )
            }
            if (cache.lastCheckAtMillis() > 0L && !cache.hasCachedUpdate(localVersionCode)) {
                return ReleaseCheckOutcome.UpToDate(
                    PosApkReleaseDto(
                        versionName = "",
                        versionCode = localVersionCode,
                        url = "",
                    ),
                )
            }
        }

        return try {
            val remote = apiFactory(normalizeBaseUrl(baseUrl)).latestAndroid()
            cache.rememberCheck(now, remote, localVersionCode)
            if (VersionCompare.isNewer(remote.versionCode, localVersionCode)) {
                ReleaseCheckOutcome.UpdateAvailable(remote)
            } else {
                ReleaseCheckOutcome.UpToDate(remote)
            }
        } catch (e: HttpException) {
            val message =
                when (e.code()) {
                    404 -> "Aún no hay una APK publicada en el servidor."
                    else -> "No se pudo consultar la versión remota (${e.code()})."
                }
            ReleaseCheckOutcome.Failed(message)
        } catch (_: IOException) {
            ReleaseCheckOutcome.Failed("Sin conexión para consultar actualizaciones.")
        } catch (e: Exception) {
            ReleaseCheckOutcome.Failed(e.message?.takeIf { it.isNotBlank() } ?: "Error al consultar actualizaciones.")
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun normalizeBaseUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim()
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }

        private fun defaultApi(baseUrl: String): PosReleaseApi =
            Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(PosReleaseApi::class.java)
    }
}
