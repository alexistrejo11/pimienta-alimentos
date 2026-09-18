package io.github.alexistrejo.pimienta.pos.data.sync

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Subset of backend ApiErrorResponse used for operator-facing messages. */
@Serializable
data class ApiErrorBody(
    val errorCode: String? = null,
    val message: String? = null,
)

/**
 * Maps HTTP/API failures to short Spanish messages for cashiers.
 * Never surfaces raw "HTTP 400" or English backend copy.
 */
object PosApiUserMessages {
    private val json = Json { ignoreUnknownKeys = true }

    /** Turns any thrown failure into a Spanish line safe to show on screen. */
    fun from(throwable: Throwable): String =
        when (throwable) {
            is HttpException -> fromHttp(throwable)
            is SerializationException ->
                "Los datos del servidor no se pudieron leer. Intenta de nuevo o contacta a un administrador."
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            -> "No hay conexión con el servidor. Revisa la red e intenta de nuevo."
            is IOException -> "No se pudo conectar con el servidor. Revisa la red e intenta de nuevo."
            else -> genericFallback()
        }

    /** Reads errorCode from the response body when present. */
    fun fromHttp(exception: HttpException): String {
        val errorCode = parseErrorCode(exception)
        return messageFor(errorCode, exception.code())
    }

    fun messageFor(errorCode: String?, httpStatus: Int): String {
        val byCode =
            when (errorCode) {
                "POS_ENROLLMENT_CODE_INVALID" ->
                    "El código de enrolamiento no es válido. Verifica que esté bien escrito."
                "POS_ENROLLMENT_CODE_EXPIRED" ->
                    "El código de enrolamiento ya expiró. Pide uno nuevo en la Web Central."
                "POS_ENROLLMENT_CODE_CONSUMED" ->
                    "Este código ya fue usado. Pide uno nuevo en la Web Central."
                "POS_DEVICE_ALREADY_ENROLLED" ->
                    "Esta tablet ya está enrolada."
                "POS_DEVICE_REVOKED" ->
                    "Este dispositivo fue revocado. Contacta a un administrador."
                "POS_DEVICE_NOT_FOUND" ->
                    "No se encontró este dispositivo. Vuelve a enrolar."
                "INVALID_DEVICE_REFRESH_TOKEN" ->
                    "La sesión del dispositivo expiró o no es válida. Vuelve a enrolar."
                "UNAUTHORIZED",
                "AUTHENTICATION_FAILED",
                -> "Sesión inválida o expirada."
                "FORBIDDEN" -> "No tienes permiso para esta acción."
                "VALIDATION_FAILED",
                "CONSTRAINT_VIOLATION",
                "INVALID_ARGUMENT",
                "MALFORMED_PAYLOAD",
                "MISSING_PARAMETER",
                -> "Revisa los datos e intenta de nuevo."
                "HEADQUARTER_NOT_FOUND" ->
                    "La sede del código no está disponible. Contacta a un administrador."
                "ITEM_BARCODE_ALREADY_EXISTS" ->
                    "Ya existe un producto con ese código de barras."
                "POS_SALE_CATEGORY_NOT_FOUND" ->
                    "Esa categoría de venta no existe en esta sede."
                else -> null
            }
        if (byCode != null) return byCode

        return when (httpStatus) {
            in 500..599 -> "Error del servidor. Intenta de nuevo en unos minutos."
            401 -> "Sesión inválida o expirada."
            403 -> "Acceso denegado."
            404 -> "No se encontró lo solicitado."
            409 -> "Ya no se puede usar este código o el dispositivo. Pide ayuda en la Web Central."
            429 -> "Demasiados intentos. Espera un momento e intenta de nuevo."
            in 400..499 -> "No fue posible completar la operación. Revisa los datos e intenta de nuevo."
            else -> genericFallback()
        }
    }

    private fun parseErrorCode(exception: HttpException): String? {
        val raw =
            try {
                exception.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(ApiErrorBody.serializer(), raw).errorCode
        } catch (_: Exception) {
            null
        }
    }

    private fun genericFallback(): String = "No fue posible completar la operación."
}
