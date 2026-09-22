package io.github.alexistrejo.pimienta.pos.data.update

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ReleaseCheckerTest {
    private lateinit var server: MockWebServer
    private lateinit var cache: ReleaseCheckCache
    private var now = 1_000_000L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cache = ReleaseCheckCache(MemoryReleaseCheckStore())
        now = 1_000_000L
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun checker(localCode: Int = 3): ReleaseChecker {
        val json = Json { ignoreUnknownKeys = true }
        return ReleaseChecker(
            cache = cache,
            localVersionCode = localCode,
            nowMillis = { now },
            apiFactory = {
                Retrofit.Builder()
                    .baseUrl(server.url("/"))
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                    .create(PosReleaseApi::class.java)
            },
        )
    }

    @Test
    fun forceCheckReportsUpdateAvailable() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """{"versionName":"2.2.9","versionCode":5,"url":"https://example.test/app.apk","expiresInSeconds":86400,"uploadedAt":"2026-03-21T00:00:00Z"}""",
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val outcome = checker(localCode = 4).check(server.url("/").toString(), force = true)
            assertTrue(outcome is ReleaseCheckOutcome.UpdateAvailable)
            assertEquals("2.2.9", (outcome as ReleaseCheckOutcome.UpdateAvailable).remote.versionName)
            assertEquals(5, cache.availableVersionCode())
        }

    @Test
    fun forceCheckReportsUpToDate() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """{"versionName":"2.2.8","versionCode":4,"url":"https://example.test/app.apk","expiresInSeconds":86400}""",
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val outcome = checker(localCode = 4).check(server.url("/").toString(), force = true)
            assertTrue(outcome is ReleaseCheckOutcome.UpToDate)
            assertEquals(0, cache.availableVersionCode())
        }

    @Test
    fun throttledCheckUsesCacheWithoutSecondRequest() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """{"versionName":"3.0.0","versionCode":10,"url":"https://example.test/app.apk","expiresInSeconds":86400}""",
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val c = checker(localCode = 4)
            c.check(server.url("/").toString(), force = true)
            assertEquals(1, server.requestCount)
            now += 60_000L
            val second = c.check(server.url("/").toString(), force = false)
            assertEquals(1, server.requestCount)
            assertTrue(second is ReleaseCheckOutcome.UpdateAvailable)
            assertEquals("3.0.0", (second as ReleaseCheckOutcome.UpdateAvailable).remote.versionName)
        }

    @Test
    fun missingReleaseReturnsFailed() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"errorCode":"POS_APK_NOT_FOUND"}"""))
            val outcome = checker().check(server.url("/").toString(), force = true)
            assertTrue(outcome is ReleaseCheckOutcome.Failed)
        }
}
