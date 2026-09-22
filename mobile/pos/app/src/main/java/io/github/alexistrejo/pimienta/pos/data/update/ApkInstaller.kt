package io.github.alexistrejo.pimienta.pos.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/** Hands a downloaded APK to the system package installer (updates in place, keeps app data). */
class ApkInstaller(private val context: Context) {
    fun canRequestInstalls(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun installPermissionSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun startInstall(apkFile: File): ApkInstallOutcome {
        if (!canRequestInstalls()) return ApkInstallOutcome.NeedsInstallPermission
        return try {
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            ApkInstallOutcome.Started
        } catch (e: Exception) {
            ApkInstallOutcome.Failed(
                e.message?.takeIf { it.isNotBlank() } ?: "No se pudo abrir el instalador.",
            )
        }
    }
}
