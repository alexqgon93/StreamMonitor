package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.StreamEndpointException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class RemoteNumbersDataSource(
    private val client: HttpClient,
    private val endpoint: String,
) : NumbersDataSource {
    override suspend fun fetch(): List<Int?> =
        fetchPayload { client.get(endpoint).body<NumbersPayload>().values }
}

internal class RemoteInputsDataSource(
    private val client: HttpClient,
    private val endpoint: String,
) : InputsDataSource {
    override suspend fun fetch(): List<Int?> =
        fetchPayload { client.get(endpoint).body<InputsPayload>().values }
}

@Serializable
internal data class NumbersPayload(
    @SerialName("numbers") val values: List<Int?>,
)

@Serializable
internal data class InputsPayload(
    @SerialName("computation_input") val values: List<Int?>,
)

/**
 * Runs one raw batch fetch and translates **every** transport or payload failure into a
 * [StreamEndpointException] carrying its retryability.
 *
 * This is the single owner of the retry classification: no Ktor, JVM, or serialization
 * exception reaches the domain untranslated, so the coordinator never has to interpret a
 * transport type itself.
 */
private suspend fun fetchPayload(
    fetch: suspend () -> List<Int?>,
): List<Int?> = try {
    fetch()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: ResponseException) {
    val status = error.response.status.value
    throw StreamEndpointException(
        message = "Remote endpoint returned HTTP $status",
        retryable = status.isTransientStatus(),
        cause = error,
    )
} catch (error: ContentConvertException) {
    throw StreamEndpointException(
        message = "Remote payload could not be decoded",
        retryable = false,
        cause = error,
    )
} catch (error: IOException) {
    // Connect, socket, and request timeouts all arrive here.
    throw StreamEndpointException(
        message = error.message ?: "Remote endpoint transport failure",
        retryable = true,
        cause = error,
    )
} catch (error: Exception) {
    throw StreamEndpointException(
        message = error.message ?: "Remote endpoint failure",
        retryable = false,
        cause = error,
    )
}

/**
 * Server errors, request timeouts, and rate limiting are transient; every other status is
 * terminal for this stream.
 */
private fun Int.isTransientStatus(): Boolean =
    this >= HttpStatusCode.InternalServerError.value ||
        this == HttpStatusCode.RequestTimeout.value ||
        this == HttpStatusCode.TooManyRequests.value
