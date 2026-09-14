package io.github.alexistrejo.pimienta.pos.data.sync

import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DeviceSessionPolicyTest {
    @Test
    fun refreshNetworkFailureDoesNotRequireReenrollment() {
        assertFalse(DeviceSessionPolicy.refreshFailureRequiresReenrollment(IOException("connection reset")))
        assertFalse(DeviceSessionPolicy.refreshFailureRequiresReenrollment(SocketTimeoutException("timeout")))
    }

    @Test
    fun refreshServerErrorDoesNotRequireReenrollment() {
        assertFalse(DeviceSessionPolicy.refreshFailureRequiresReenrollment(httpException(500)))
        assertFalse(DeviceSessionPolicy.refreshFailureRequiresReenrollment(httpException(503)))
    }

    @Test
    fun refreshAuthFailureRequiresReenrollment() {
        assertTrue(DeviceSessionPolicy.refreshFailureRequiresReenrollment(httpException(401)))
        assertTrue(DeviceSessionPolicy.refreshFailureRequiresReenrollment(httpException(403)))
    }

    @Test
    fun only403OnSyncRequiresImmediateReenrollment() {
        assertTrue(DeviceSessionPolicy.httpResponseRequiresReenrollment(403))
        assertFalse(DeviceSessionPolicy.httpResponseRequiresReenrollment(401))
        assertFalse(DeviceSessionPolicy.httpResponseRequiresReenrollment(500))
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))
}
