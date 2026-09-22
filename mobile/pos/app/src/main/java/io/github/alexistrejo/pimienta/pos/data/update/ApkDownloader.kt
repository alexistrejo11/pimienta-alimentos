package io.github.alexistrejo.pimienta.pos.data.update

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Downloads a release APK from a pre-signed URL into the app cache. */
class ApkDownloader(
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .build(),
) {
    fun download(url: String, targetFile: File): Result<File> =
        runCatching {
            if (url.isBlank()) error("URL de descarga vacía.")
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            if (!response.isSuccessful) {
                throw IOException("Descarga fallida (${response.code}).")
            }
            response.body?.byteStream()?.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Respuesta de descarga vacía.")
            if (targetFile.length() <= 0L) throw IOException("APK descargada vacía.")
            targetFile
        }
}
