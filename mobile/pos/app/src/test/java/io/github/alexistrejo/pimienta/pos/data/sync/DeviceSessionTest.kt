package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * Behavioral tests for the shared [DeviceSession]:
 *   - successful authenticated request attaches Bearer header
 *   - 401 triggers refresh then retries once, persisting the new credentials
 *   - refresh HTTP 401 is classified as reenrollment required
 *   - refresh HTTP 500 is classified as transient
 *   - missing refresh token short-circuits without hitting the network
 *   - already-rotated detection prevents a second refresh race
 */
class DeviceSessionTest {

    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeTokenStore
    private lateinit var session: DeviceSession
    private lateinit var baseUrl: String

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = FakeTokenStore(access = "access-1", refresh = "refresh-1")
        session = DeviceSession(tokens)
        baseUrl = server.url("/").toString()
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun authenticatedRequestAttachesBearerHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(CHANGES_EMPTY_BODY)
            .addHeader("Content-Type", "application/json"))

        val outcome = session.withAuth(baseUrl) { api -> api.changes("cursor-x") }

        assertTrue("expected Success, got $outcome", outcome is AuthenticatedCallOutcome.Success)
        val recorded = server.takeRequest()
        assertEquals("Bearer access-1", recorded.getHeader("Authorization"))
    }

    @Test
    fun missingAccessShortCircuitsWithoutNetwork() = runBlocking {
        tokens.save(access = "", refresh = tokens.refresh()!!)
        tokens.wipeAccessOnly()
        val outcome = session.withAuth(baseUrl) { api -> api.changes("cursor-x") }

        assertEquals(AuthenticatedCallOutcome.MissingAccess, outcome)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unauthorizedTriggersRefreshThenRetryAndPersistsNewCredentials() = runBlocking {
        // 1) original call → 401
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"expired"}""")
            .addHeader("Content-Type", "application/json"))
        // 2) refresh → new tokens
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(tokenPairBody("access-2", "refresh-2"))
            .addHeader("Content-Type", "application/json"))
        // 3) retry → success
        server.enqueue(MockResponse().setResponseCode(200).setBody(CHANGES_EMPTY_BODY)
            .addHeader("Content-Type", "application/json"))

        val outcome = session.withAuth(baseUrl) { api -> api.changes("cursor-x") }

        assertTrue(outcome is AuthenticatedCallOutcome.Success)
        assertEquals(3, server.requestCount)
        // Original request used the stale token.
        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
        // Refresh request carries no Authorization header and posts the stored refresh token.
        val refreshRequest = server.takeRequest()
        assertNull(refreshRequest.getHeader("Authorization"))
        assertTrue(refreshRequest.body.readUtf8().contains("\"refreshToken\":\"refresh-1\""))
        // Retried request uses the freshly rotated access token (dynamic token provider).
        assertEquals("Bearer access-2", server.takeRequest().getHeader("Authorization"))
        // New credentials are persisted.
        assertEquals("access-2", tokens.access())
        assertEquals("refresh-2", tokens.refresh())
    }

    @Test
    fun refreshRejectedWithUnauthorizedReportsReenrollmentRequired() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))  // original
        server.enqueue(MockResponse().setResponseCode(401))  // refresh rejected

        val outcome = session.withAuth(baseUrl) { api -> api.changes("c") }

        assertTrue("expected RefreshFailed, got $outcome", outcome is AuthenticatedCallOutcome.RefreshFailed)
        val failed = outcome as AuthenticatedCallOutcome.RefreshFailed
        assertTrue(failed.requiresReenrollment)
        assertTrue(failed.error is HttpException)
        // Tokens must NOT have been overwritten.
        assertEquals("access-1", tokens.access())
        assertEquals("refresh-1", tokens.refresh())
    }

    @Test
    fun refreshServerErrorIsClassifiedAsTransient() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))  // original
        server.enqueue(MockResponse().setResponseCode(500))  // refresh 5xx

        val outcome = session.withAuth(baseUrl) { api -> api.changes("c") }

        assertTrue(outcome is AuthenticatedCallOutcome.RefreshFailed)
        val failed = outcome as AuthenticatedCallOutcome.RefreshFailed
        assertEquals(false, failed.requiresReenrollment)
    }

    @Test
    fun missingRefreshTokenShortCircuitsOnUnauthorized() = runBlocking {
        tokens.wipeRefreshOnly()
        server.enqueue(MockResponse().setResponseCode(401))  // original

        val outcome = session.withAuth(baseUrl) { api -> api.changes("c") }

        assertEquals(AuthenticatedCallOutcome.MissingRefresh, outcome)
        // Only the original request hit the server; no refresh attempt was made.
        assertEquals(1, server.requestCount)
    }

    @Test
    fun rotateAccessTokenPersistsNewPairAndReportsRotated() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(tokenPairBody("access-9", "refresh-9"))
            .addHeader("Content-Type", "application/json"))

        val outcome = session.rotateAccessToken(baseUrl, staleAccess = "access-1")

        assertEquals(TokenRotationOutcome.Rotated, outcome)
        assertEquals("access-9", tokens.access())
        assertEquals("refresh-9", tokens.refresh())
    }

    @Test
    fun rotateAccessTokenDetectsAlreadyFreshWhenStoreMovedPastStale() = runBlocking {
        // Another runner already rotated access-1 → access-7 on this process.
        tokens.save("access-7", "refresh-7")

        val outcome = session.rotateAccessToken(baseUrl, staleAccess = "access-1")

        assertEquals(TokenRotationOutcome.AlreadyFresh, outcome)
        assertEquals(0, server.requestCount)
        assertEquals("access-7", tokens.access())
    }

    @Test
    fun rotateAccessTokenReturnsMissingRefreshWhenStoreHasNone() = runBlocking {
        tokens.clear()

        val outcome = session.rotateAccessToken(baseUrl, staleAccess = null)

        assertEquals(TokenRotationOutcome.MissingRefresh, outcome)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unauthenticatedApiOmitsAuthorizationHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(tokenPairBody("a", "r"))
            .addHeader("Content-Type", "application/json"))

        session.unauthenticatedApi(baseUrl).refresh(RefreshRequest("rtok"))

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun authenticatedApiReadsTokenDynamicallyPerRequest() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(CHANGES_EMPTY_BODY)
            .addHeader("Content-Type", "application/json"))
        server.enqueue(MockResponse().setResponseCode(200).setBody(CHANGES_EMPTY_BODY)
            .addHeader("Content-Type", "application/json"))

        val api = session.authenticatedApi(baseUrl)
        api.changes("a")
        // Rotate the token out-of-band; the same api instance must pick it up.
        tokens.save("access-dynamic", "refresh-dynamic")
        api.changes("b")

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer access-dynamic", server.takeRequest().getHeader("Authorization"))
    }

    // --- helpers -------------------------------------------------------------

    private class FakeTokenStore(
        private var access: String?,
        private var refresh: String?,
    ) : DeviceTokenStore {
        override fun access(): String? = access
        override fun refresh(): String? = refresh
        override fun save(access: String, refresh: String) {
            this.access = access
            this.refresh = refresh
        }
        override fun clear() { access = null; refresh = null }
        fun wipeAccessOnly() { access = null }
        fun wipeRefreshOnly() { refresh = null }
    }

    companion object {
        private const val CHANGES_EMPTY_BODY =
            """{"schemaVersion":1,"nextCursor":"c-next","operations":[]}"""

        private fun tokenPairBody(access: String, refresh: String): String =
            """{"accessToken":"$access","refreshToken":"$refresh",""" +
                """"accessTokenExpiresInSeconds":900,""" +
                """"refreshTokenExpiresInSeconds":3600,""" +
                """"refreshTokenMaxExpiresInSeconds":86400}"""
    }
}

/** Ensures the [DeviceCredentials] production implementation satisfies the new abstraction. */
@Suppress("unused")
private val tokenStoreContract: (DeviceCredentials) -> DeviceTokenStore = { it }
