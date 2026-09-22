package io.github.alexistrejo.pimienta.pos.data.update

/** Result of comparing the installed APK to the published release. */
sealed class ReleaseCheckOutcome {
    data class UpToDate(val remote: PosApkReleaseDto) : ReleaseCheckOutcome()

    data class UpdateAvailable(val remote: PosApkReleaseDto) : ReleaseCheckOutcome()

    data class Failed(val message: String) : ReleaseCheckOutcome()
}

/** Result of downloading and handing the APK to the system installer. */
sealed class ApkInstallOutcome {
    data object Started : ApkInstallOutcome()

    data object NeedsInstallPermission : ApkInstallOutcome()

    data class Failed(val message: String) : ApkInstallOutcome()
}
