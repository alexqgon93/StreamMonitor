package com.alexqgon.streammonitor.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The single definition of the stream HTTP client configuration.
 *
 * Both settings are load-bearing for the retry contract owned by [RemoteNumbersDataSource] and
 * [RemoteInputsDataSource]:
 *
 * - `expectSuccess = true` is what turns a non-2xx response into a `ResponseException`. Without
 *   it a 5xx body would be fed to the deserializer and misclassified as a terminal payload
 *   failure, so a transient server error would end the stream on its first attempt.
 * - `ignoreUnknownKeys = true` keeps a server adding a field from becoming a terminal decoding
 *   failure.
 *
 * Production and tests both build their client here so the tests exercise this configuration
 * rather than a reconstruction of it; only the engine differs.
 */
internal fun streamHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}
