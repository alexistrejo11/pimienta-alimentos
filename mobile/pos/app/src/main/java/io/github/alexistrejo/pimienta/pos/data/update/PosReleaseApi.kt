package io.github.alexistrejo.pimienta.pos.data.update

import retrofit2.http.GET

/** Public POS APK release metadata (no JWT). */
interface PosReleaseApi {
    @GET("api/v1/pos/releases/android/latest")
    suspend fun latestAndroid(): PosApkReleaseDto
}
