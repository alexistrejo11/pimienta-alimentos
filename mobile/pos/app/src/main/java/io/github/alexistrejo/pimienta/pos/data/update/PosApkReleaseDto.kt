package io.github.alexistrejo.pimienta.pos.data.update

import kotlinx.serialization.Serializable

/** Public API payload from GET /api/v1/pos/releases/android/latest. */
@Serializable
data class PosApkReleaseDto(
    val versionName: String,
    val versionCode: Int,
    val url: String,
    val expiresInSeconds: Long = 86_400L,
    val uploadedAt: String? = null,
)
