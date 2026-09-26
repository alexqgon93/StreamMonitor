package com.alexqgon.streammonitor.domain

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

@OptIn(ExperimentalCoroutinesApi::class)
class StreamCoordinatorTest {
    @Nested
    inner class Joining {
        @Test
        fun `numbers arriving before inputs eventually produce a ready row`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(150), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(48), emptyList<Int?>())),
            ).toList().last()

            snapshot.rows.single().result shouldBe ResultState.Ready("48")
        }

        @Test
        fun `inputs arriving before numbers are retained by source index`() = runTest {
            val numberGate = CompletableDeferred<Unit>()

            coordinator(
                numbers = ScriptedEndpoint(
                    listOf(listOf(150), emptyList<Int?>()),
                    numberGate,
                ),
                inputs = ScriptedEndpoint(listOf(listOf(48), emptyList<Int?>())),
            ).test {
                awaitItem()
                numberGate.complete(Unit)

                var completed = awaitItem()
                while (completed.lifecycle != StreamLifecycle.Completed) {
                    completed = awaitItem()
                }

                completed.rows.single().result shouldBe ResultState.Ready("48")
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `an input for an invalid number is ignored rather than creating a row`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(24), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(10), emptyList<Int?>())),
            ).toList().last()

            snapshot.rows shouldBe emptyList()
        }

        @Test
        fun `null input becomes a missing input failure rather than a discard`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(150), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(null), emptyList<Int?>())),
            ).toList().last()

            snapshot.rows.single().result shouldBe ResultState.Failed(FailureReason.MISSING_INPUT)
        }

        @Test
        fun `numbers without matching inputs resolve as missing rather than remaining pending`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(150), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
            ).toList().last()

            snapshot.rows.single().result shouldBe ResultState.Failed(FailureReason.MISSING_INPUT)
        }

        @Test
        fun `input outside unsigned short range becomes an invalid input failure`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(128), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(70_000), emptyList<Int?>())),
            ).toList().last()

            snapshot.rows.single().result shouldBe ResultState.Failed(FailureReason.INVALID_INPUT)
        }

        @Test
        fun `one endpoint may finish while the other keeps advancing`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(4), emptyList<Int?>())),
                inputs = ScriptedEndpoint(
                    listOf(listOf(10), listOf(11), listOf(12), emptyList<Int?>()),
                ),
            ).toList().last()

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
        }

        @Test
        fun `both empty endpoints complete the stream`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
            ).toList().last()

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
        }
    }

    @Nested
    inner class Scheduling {
        @Test
        fun `number without result bit never schedules computation`() = runTest {
            val calls = AtomicInteger()
            coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(4), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(10), emptyList<Int?>())),
                compute = {
                    calls.incrementAndGet()
                    "10"
                },
            ).toList()

            calls.get() shouldBe 0
        }

        @Test
        fun `a completed result moves the existing row by its stable identity`() = runTest {
            val secondInputGate = CompletableDeferred<Unit>()

            coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(128, 128), emptyList<Int?>())),
                inputs = ScriptedEndpoint(
                    listOf(listOf(1), listOf(2), emptyList<Int?>()),
                    secondInputGate,
                    gateAfterPolls = 1,
                ),
            ).test {
                var moved = awaitItem()
                while (moved.rows.map { it.sourceIndex } != listOf(1, 0)) {
                    moved = awaitItem()
                }

                moved.rows.map { it.sourceIndex } shouldBe listOf(1, 0)
                secondInputGate.complete(Unit)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `a large computation batch completes without deadlocking workers`() = runTest {
            val size = 500
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(List(size) { 128 }, emptyList<Int?>())),
                inputs = ScriptedEndpoint(
                    listOf((0 until size).toList(), emptyList<Int?>()),
                ),
            ).toList().last()

            snapshot.rows.count { it.result is ResultState.Ready } shouldBe size
        }
    }

    @Nested
    inner class Reliability {
        @Test
        fun `a failed computation does not prevent other scheduled rows from completing`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(listOf(listOf(128, 128), emptyList<Int?>())),
                inputs = ScriptedEndpoint(listOf(listOf(1, 2), emptyList<Int?>())),
                compute = { input ->
                    if (input == 1.toUShort()) error("computation failed")
                    "02"
                },
            ).toList().last()

            snapshot.rows.single { it.sourceIndex == 0 }.result shouldBe
                ResultState.Failed(FailureReason.COMPUTATION_FAILED)
        }

        @Test
        fun `cancelling the collector stops outstanding endpoint polling`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val endpoint = ScriptedEndpoint(
                responses = listOf(listOf(150), emptyList<Int?>()),
                gate = gate,
            )
            val collection = backgroundScope.launch {
                coordinator(
                    numbers = endpoint,
                    inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
                ).collect()
            }

            runCurrent()
            collection.cancelAndJoin()
            advanceUntilIdle()

            endpoint.pollStarts shouldBe 1
        }

        @Test
        fun `transient endpoint failure retries after virtual backoff and recovers`() = runTest {
            val endpoint = ScriptedEndpoint(
                listOf(IOException("temporary"), listOf(4), emptyList<Int?>()),
            )
            val snapshots = async {
                coordinator(
                    numbers = endpoint,
                    inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
                    retryPolicy = RetryPolicy(maxAttempts = 2, initialBackoffMillis = 100),
                ).toList()
            }

            runCurrent()
            endpoint.calls shouldBe 1
            advanceTimeBy(100)
            advanceUntilIdle()

            snapshots.await().last().lifecycle shouldBe StreamLifecycle.Completed
        }

        @Test
        fun `exhausted retryable failures retain rows and expose a failed lifecycle`() = runTest {
            val endpoint = ScriptedEndpoint(
                listOf(listOf(4), IOException("permanent"), IOException("permanent")),
            )
            val snapshot = coordinator(
                numbers = endpoint,
                inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
                retryPolicy = RetryPolicy(maxAttempts = 2),
            ).toList().last()

            assertAll(
                { snapshot.lifecycle shouldBe StreamLifecycle.Failed("permanent") },
                { snapshot.rows shouldContain StreamRow(0, 0, 1, ResultState.NotNeeded) },
            )
        }

        @Test
        fun `non retryable endpoint failures become a failed lifecycle`() = runTest {
            val snapshot = coordinator(
                numbers = ScriptedEndpoint(
                    listOf(StreamEndpointException("malformed", retryable = false)),
                ),
                inputs = ScriptedEndpoint(listOf(emptyList<Int?>())),
            ).toList().last()

            snapshot.lifecycle shouldBe StreamLifecycle.Failed("malformed")
        }
    }

    private fun TestScope.coordinator(
        numbers: StreamEndpoint,
        inputs: StreamEndpoint,
        compute: (UShort) -> String = { input ->
            input.toInt().mod(100).toString().padStart(2, '0')
        },
        retryPolicy: RetryPolicy = RetryPolicy(),
    ) = StreamCoordinator(
        numbers = numbers,
        inputs = inputs,
        coordinatorDispatcher = StandardTestDispatcher(testScheduler),
        computationDispatcher = StandardTestDispatcher(testScheduler),
        compute = compute,
        retryPolicy = retryPolicy,
    ).snapshots()

    private class ScriptedEndpoint(
        private val responses: List<Any>,
        private val gate: CompletableDeferred<Unit>? = null,
        private val gateAfterPolls: Int = 0,
    ) : StreamEndpoint {
        var calls = 0
            private set
        var pollStarts = 0
            private set

        override suspend fun poll(): List<Int?> {
            pollStarts++
            if (calls >= gateAfterPolls) {
                gate?.await()
            }
            return when (val response = responses[calls++]) {
                is Exception -> throw response
                else -> @Suppress("UNCHECKED_CAST") (response as List<Int?>)
            }
        }
    }
}
