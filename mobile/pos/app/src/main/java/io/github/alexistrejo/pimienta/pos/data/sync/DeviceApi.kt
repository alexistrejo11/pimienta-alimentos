package io.github.alexistrejo.pimienta.pos.data.sync

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Defines the Spring Boot POS Device API used by the background worker.
interface DeviceApi {
    @POST("api/v1/pos/devices/enroll") suspend fun enroll(@Body body: EnrollRequest): EnrollResponse
    @POST("api/v1/pos/devices/refresh") suspend fun refresh(@Body body: RefreshRequest): TokenPair
    @GET("api/v1/pos/devices/me") suspend fun me(): Response<Unit>
    @GET("api/v1/pos/sync/bootstrap") suspend fun bootstrap(): BootstrapResponse
    @GET("api/v1/pos/sync/changes") suspend fun changes(@Query("cursor") cursor: String): ChangesResponse
    @POST("api/v1/pos/sync/events") suspend fun events(@Body body: EventsRequest): EventsResponse
    @POST("api/v1/pos/telemetry/events") suspend fun telemetry(@Body body: TelemetryBatchRequest): TelemetryAcceptedResponse
}
