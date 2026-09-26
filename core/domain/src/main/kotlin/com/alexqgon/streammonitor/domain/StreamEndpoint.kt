package com.alexqgon.streammonitor.domain

fun interface StreamEndpoint {
    suspend fun poll(): List<Int?>
}

/**
 * Signals an endpoint failure that the coordinator can classify for retry.
 *
 * Data-layer adapters translate transport and payload failures to this pure-domain type.
 */
class StreamEndpointException(
    message: String,
    val retryable: Boolean,
    cause: Throwable? = null,
) : Exception(message, cause)
