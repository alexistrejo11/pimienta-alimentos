package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.ZoneId

/**
 * Builds [DeviceApi] clients with a single shared OkHttp/Retrofit/JSON configuration.
 *
 * This replaces the two slightly different Retrofit builders that lived inline in
 * [PosSyncPipeline] and [ProvisioningRepository]. All device API traffic goes through
 * one HTTP configuration so interceptor, converter, and base-URL normalization behavior
 * cannot drift between callers.
 *
 * Token management belongs to [DeviceSession]; this factory only knows how to build a
 * client that reads an Authorization header value from a `tokenProvider` on every call.
 * Dynamic token reads are intentional: once [DeviceSession] rotates the access token,
 * every subsequent request made through the same [DeviceApi] instance picks the new one
 * up automatically — matching the pre-existing behavior of the pipeline's own client.
 */
internal object DeviceApiFactory {

    // kotlinx.serialization configured identically to the historical pipeline/provisioning setup.
    // `explicitNulls = false` matched PosSyncPipeline; ProvisioningRepository omitted it. Both
    // code paths only ever deserialize server responses that never emitted explicit nulls, so
    // enabling it on the shared instance keeps behavior identical for every existing endpoint.
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Shared JSON configuration — exposed so callers (e.g. event envelope encoding) can reuse it. */
    fun json(): Json = json

    /** Builds an authenticated client whose Authorization header is read on every request. */
    fun create(baseUrl: String, tokenProvider: () -> String?): DeviceApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(timezoneInterceptor())
            .addInterceptor(Interceptor { chain ->
                val token = tokenProvider()
                val request = chain.request().newBuilder()
                    .apply { if (token != null) header("Authorization", "Bearer $token") }
                    .build()
                chain.proceed(request)
            })
            .build()
        return buildRetrofit(normalize(baseUrl), client).create(DeviceApi::class.java)
    }

    /** Builds a client with no Authorization interceptor; used for enroll/refresh endpoints. */
    fun createUnauthenticated(baseUrl: String): DeviceApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(timezoneInterceptor())
            .build()
        return buildRetrofit(normalize(baseUrl), client).create(DeviceApi::class.java)
    }

    // Sends the device IANA zone on every call, including enroll and refresh.
    // The value is read per request so it stays the zone used by local day bounds.
    private fun timezoneInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("X-Timezone", ZoneId.systemDefault().id)
            .build()
        chain.proceed(request)
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun normalize(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
}
