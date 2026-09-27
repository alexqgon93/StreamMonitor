package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.StreamEndpointException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RemoteStreamDataSourcesTest {
    @Nested
    inner class Numbers {
        @Test
        fun `2xx numbers payload is exposed as raw numbers`() = runTest {
            val source = RemoteNumbersDataSource(clientResponding("""{"numbers":[4,null,150]}"""), URL)

            source.fetch() shouldBe listOf(4, null, 150)
        }

        @Test
        fun `non successful response is surfaced to the caller`() = runTest {
            val source = RemoteNumbersDataSource(
                clientResponding("failure", HttpStatusCode.InternalServerError),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `client response error is classified as non retryable`() = runTest {
            val source = RemoteNumbersDataSource(
                clientResponding("invalid request", HttpStatusCode.BadRequest),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe false
        }

        @Test
        fun `request timeout status is classified as retryable despite being a client error`() = runTest {
            val source = RemoteNumbersDataSource(
                clientResponding("timeout", HttpStatusCode.RequestTimeout),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `rate limiting is classified as retryable despite being a client error`() = runTest {
            val source = RemoteNumbersDataSource(
                clientResponding("slow down", HttpStatusCode.TooManyRequests),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `transport failure is translated into a retryable endpoint failure`() = runTest {
            val source = RemoteNumbersDataSource(clientFailingWith(IOException("socket closed")), URL)

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `an unexpected transport exception never escapes untranslated`() = runTest {
            val source = RemoteNumbersDataSource(
                clientFailingWith(IllegalStateException("engine exploded")),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe false
        }

        @Test
        fun `malformed numbers payload is surfaced to the caller`() = runTest {
            val source = RemoteNumbersDataSource(clientResponding("""{"numbers":"not-an-array"}"""), URL)

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe false
        }
    }

    @Nested
    inner class Inputs {
        @Test
        fun `2xx computation input payload is exposed as raw inputs`() = runTest {
            val source = RemoteInputsDataSource(
                clientResponding("""{"computation_input":[3,48,103]}"""),
                URL,
            )

            source.fetch() shouldBe listOf(3, 48, 103)
        }

        @Test
        fun `non successful response is surfaced to the caller`() = runTest {
            val source = RemoteInputsDataSource(
                clientResponding("failure", HttpStatusCode.BadGateway),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `malformed computation input payload is surfaced to the caller`() = runTest {
            val source = RemoteInputsDataSource(
                clientResponding("""{"computation_input":"not-an-array"}"""),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe false
        }

        @Test
        fun `a syntactically corrupt body is terminal rather than retried`() = runTest {
            val source = RemoteInputsDataSource(clientResponding("{broken-json"), URL)

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe false
        }
    }

    /**
     * These pin [streamHttpClient], the configuration production actually uses. Removing either
     * setting from it must fail here rather than only in production.
     */
    @Nested
    inner class SharedClientConfiguration {
        @Test
        fun `a non 2xx body is rejected as a transport failure instead of being parsed`() = runTest {
            // Without expectSuccess the 5xx body would decode-fail and be classified terminal.
            val source = RemoteNumbersDataSource(
                clientResponding("""{"numbers":[4]}""", HttpStatusCode.ServiceUnavailable),
                URL,
            )

            shouldThrow<StreamEndpointException> { source.fetch() }.retryable shouldBe true
        }

        @Test
        fun `an unknown key added by the server does not fail the stream`() = runTest {
            val source = RemoteNumbersDataSource(
                clientResponding("""{"numbers":[4,null,150],"serverAddedField":"ignored"}"""),
                URL,
            )

            source.fetch() shouldBe listOf(4, null, 150)
        }
    }

    private fun clientResponding(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient = streamHttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        },
    )

    private fun clientFailingWith(failure: Throwable): HttpClient = streamHttpClient(
        MockEngine { throw failure },
    )

    private companion object {
        const val URL = "https://streammonitor.test/stream"
    }
}
