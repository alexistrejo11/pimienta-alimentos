package io.github.alexistrejo.pimienta.pos.data.sync

import okhttp3.Interceptor
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

internal const val TRACE_ID_HEADER = "X-Trace-Id"

/** IO failure that still carries the request's X-Trace-Id. */
internal class TracedIoException(val traceId: String, cause: IOException) : IOException(cause)

/** Puts a fresh UUID on every API call so logs and Room diagnostics can match. */
internal fun traceIdInterceptor(): Interceptor = Interceptor { chain ->
    val traceId = UUID.randomUUID().toString()
    val request = chain.request().newBuilder()
        .header(TRACE_ID_HEADER, traceId)
        .build()
    try {
        chain.proceed(request)
    } catch (e: IOException) {
        throw TracedIoException(traceId, e)
    }
}

internal fun traceIdOf(error: Throwable): String? = when (error) {
    is HttpException -> error.response()?.raw()?.request?.header(TRACE_ID_HEADER)
    is TracedIoException -> error.traceId
    else -> (error.cause as? TracedIoException)?.traceId
}
