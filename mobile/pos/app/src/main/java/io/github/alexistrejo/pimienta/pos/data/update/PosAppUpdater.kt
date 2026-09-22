package io.github.alexistrejo.pimienta.pos.data.update

import android.content.Context
import io.github.alexistrejo.pimienta.pos.BuildConfig
import io.github.alexistrejo.pimienta.pos.app.posDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.sync.PRODUCTION_API_URL
import java.io.File

/**
 * UI-facing coordinator: throttled check, forced refresh, download + system install.
 * Always re-fetches a fresh pre-signed URL before download (URLs expire).
 */
class PosAppUpdater(
    private val context: Context,
    private val cache: ReleaseCheckCache = ReleaseCheckCache(context),
    private val checker: ReleaseChecker =
        ReleaseChecker(cache = cache, localVersionCode = BuildConfig.VERSION_CODE),
    private val downloader: ApkDownloader = ApkDownloader(),
    private val installer: ApkInstaller = ApkInstaller(context),
) {
    fun cachedUpdateVersionName(): String? =
        cache.availableVersionName()?.takeIf { cache.hasCachedUpdate(BuildConfig.VERSION_CODE) }

    fun hasCachedUpdate(): Boolean = cache.hasCachedUpdate(BuildConfig.VERSION_CODE)

    suspend fun check(baseUrl: String = resolveBaseUrl(), force: Boolean = false): ReleaseCheckOutcome =
        checker.check(baseUrl, force)

    suspend fun downloadAndInstall(baseUrl: String = resolveBaseUrl()): ApkInstallOutcome {
        if (!installer.canRequestInstalls()) return ApkInstallOutcome.NeedsInstallPermission
        val outcome = checker.check(baseUrl, force = true)
        val remote =
            when (outcome) {
                is ReleaseCheckOutcome.UpdateAvailable -> outcome.remote
                is ReleaseCheckOutcome.UpToDate ->
                    return ApkInstallOutcome.Failed(
                        "Ya tienes la última versión (${outcome.remote.versionName.ifBlank { BuildConfig.VERSION_NAME }}).",
                    )
                is ReleaseCheckOutcome.Failed ->
                    return ApkInstallOutcome.Failed(outcome.message)
            }
        if (remote.url.isBlank()) {
            return ApkInstallOutcome.Failed("El servidor no devolvió URL de descarga.")
        }
        val target = File(File(context.cacheDir, APK_DIR), APK_NAME)
        val downloaded =
            downloader.download(remote.url, target).getOrElse {
                return ApkInstallOutcome.Failed(
                    it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Error al descargar la APK.",
                )
            }
        return installer.startInstall(downloaded)
    }

    fun installPermissionSettingsIntent() = installer.installPermissionSettingsIntent()

    fun resolveBaseUrl(): String {
        val fromProduction =
            runCatching {
                context.posDatabaseProvider()
                    .database(RuntimeMode.PRODUCTION)
                    .syncDao()
                    .state()
                    ?.baseUrl
            }.getOrNull()
        val raw = fromProduction?.trim()?.takeIf { it.isNotEmpty() } ?: PRODUCTION_API_URL
        return ReleaseChecker.normalizeBaseUrl(raw)
    }

    companion object {
        private const val APK_DIR = "apk-updates"
        private const val APK_NAME = "pimienta-pos-update.apk"
    }
}
