package io.github.alexistrejo.pimienta.pos.data.sync

import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Confirms enroll/refresh and authenticated sync clients send the device zone.
class DeviceApiFactoryTimezoneTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authenticatedClientSendsDeviceZoneAndBearer() = runBlocking {
        server.enqueue(changesOk())
        val api = DeviceApiFactory.create(server.url("/").toString()) { "access-token" }

        api.changes("cursor")

        val recorded = server.takeRequest()
        assertEquals(ZoneId.systemDefault().id, recorded.getHeader("X-Timezone"))
        assertEquals("Bearer access-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun unauthenticatedClientSendsDeviceZoneWithoutAuthorization() = runBlocking {
        server.enqueue(changesOk())
        val api = DeviceApiFactory.createUnauthenticated(server.url("/").toString())

        api.changes("cursor")

        val recorded = server.takeRequest()
        assertEquals(ZoneId.systemDefault().id, recorded.getHeader("X-Timezone"))
        assertNull(recorded.getHeader("Authorization"))
    }

    private fun changesOk(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"schemaVersion":1,"nextCursor":"next","operations":[]}""")
            .addHeader("Content-Type", "application/json")
}
