package io.github.alexistrejo.pimienta.pos.data.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

// Verifies that only the retained-cursor conflict triggers a full bootstrap.
class PosSyncErrorPolicyTest {
    @Test
    fun invalidCursorConflictRequiresBootstrap() {
        assertTrue(PosSyncErrorPolicy.requiresBootstrap(exception(409, "POS_SYNC_CURSOR_INVALID")))
    }

    @Test
    fun unrelatedConflictDoesNotRequireBootstrap() {
        assertFalse(PosSyncErrorPolicy.requiresBootstrap(exception(409, "POS_DEVICE_ALREADY_ENROLLED")))
        assertFalse(PosSyncErrorPolicy.requiresBootstrap(exception(500, "POS_SYNC_CURSOR_INVALID")))
    }

    private fun exception(status: Int, errorCode: String): HttpException {
        val body = "{\"errorCode\":\"$errorCode\"}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(status, body))
    }
}
