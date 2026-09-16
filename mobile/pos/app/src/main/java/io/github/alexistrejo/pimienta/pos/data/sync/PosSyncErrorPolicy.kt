package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

// Identifies the server response that requires rebuilding the downstream cache.
object PosSyncErrorPolicy {
    private val json = Json { ignoreUnknownKeys = true }

    fun requiresBootstrap(error: HttpException): Boolean {
        if (error.code() != 409) return false
        val body = error.response()?.errorBody()?.string() ?: return false
        return runCatching {
            json.parseToJsonElement(body).jsonObject["errorCode"]?.jsonPrimitive?.content ==
                "POS_SYNC_CURSOR_INVALID"
        }.getOrDefault(false)
    }
}
