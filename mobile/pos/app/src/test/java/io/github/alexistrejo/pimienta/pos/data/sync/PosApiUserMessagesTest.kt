package io.github.alexistrejo.pimienta.pos.data.sync

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PosApiUserMessagesTest {

    @Test
    fun mapsEnrollmentErrorCodesToSpanish() {
        assertEquals(
            "El código de enrolamiento no es válido. Verifica que esté bien escrito.",
            PosApiUserMessages.messageFor("POS_ENROLLMENT_CODE_INVALID", 400),
        )
        assertEquals(
            "El código de enrolamiento ya expiró. Pide uno nuevo en la Web Central.",
            PosApiUserMessages.messageFor("POS_ENROLLMENT_CODE_EXPIRED", 400),
        )
        assertEquals(
            "Este código ya fue usado. Pide uno nuevo en la Web Central.",
            PosApiUserMessages.messageFor("POS_ENROLLMENT_CODE_CONSUMED", 409),
        )
        assertEquals(
            "Ya existe un producto con ese código de barras.",
            PosApiUserMessages.messageFor("ITEM_BARCODE_ALREADY_EXISTS", 409),
        )
        assertEquals(
            "Esa categoría de venta no existe en esta sede.",
            PosApiUserMessages.messageFor("POS_SALE_CATEGORY_NOT_FOUND", 404),
        )
    }

    @Test
    fun mapsHttpStatusWhenErrorCodeMissing() {
        assertEquals(
            "Error del servidor. Intenta de nuevo en unos minutos.",
            PosApiUserMessages.messageFor(null, 500),
        )
        assertEquals(
            "Sesión inválida o expirada.",
            PosApiUserMessages.messageFor(null, 401),
        )
    }

    @Test
    fun fromHttpReadsErrorCodeFromBody() {
        val body =
            """{"errorCode":"POS_ENROLLMENT_CODE_EXPIRED","message":"The enrollment code has expired."}"""
                .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Unit>(400, body))

        val message = PosApiUserMessages.from(exception)

        assertEquals(
            "El código de enrolamiento ya expiró. Pide uno nuevo en la Web Central.",
            message,
        )
        assertFalse(message.contains("HTTP", ignoreCase = true))
        assertFalse(message.contains("expired", ignoreCase = true))
    }

    @Test
    fun fromNetworkFailuresAreSpanish() {
        assertEquals(
            "No hay conexión con el servidor. Revisa la red e intenta de nuevo.",
            PosApiUserMessages.from(UnknownHostException("api.example")),
        )
        assertEquals(
            "No hay conexión con el servidor. Revisa la red e intenta de nuevo.",
            PosApiUserMessages.from(SocketTimeoutException("timeout")),
        )
    }
}
