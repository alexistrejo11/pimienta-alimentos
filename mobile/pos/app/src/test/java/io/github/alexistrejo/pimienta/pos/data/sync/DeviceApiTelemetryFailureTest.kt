package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

// Verifies that transport failures remain visible to the worker for retry handling.
class DeviceApiTelemetryFailureTest {
    private lateinit var server: MockWebServer
    private lateinit var api: DeviceApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeviceApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun unauthorizedTelemetryIsReportedAs401() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(401))

            val error = assertThrows(HttpException::class.java) {
                runBlocking { api.telemetry(TelemetryBatchRequest(emptyList())) }
            }

            assertEquals(401, error.code())
        }
    }

    @Test
    fun unavailableTelemetryEndpointRemainsAnIOException() {
        runBlocking {
            server.shutdown()

            assertThrows(IOException::class.java) {
                runBlocking { api.telemetry(TelemetryBatchRequest(emptyList())) }
            }
        }
    }
}
