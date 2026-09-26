package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.FailureReason
import com.alexqgon.streammonitor.domain.ResultState
import com.alexqgon.streammonitor.domain.StreamCoordinator
import com.alexqgon.streammonitor.domain.StreamLifecycle
import com.alexqgon.streammonitor.domain.StreamSnapshot
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Runs each mock scenario through the real coordinator so the data layer is verified against
 * the domain contract it feeds, not only at the `fetch()` boundary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MockScenarioStreamIntegrationTest {
    @Nested
    inner class HappyPath {
        @Test
        fun `every valid number becomes a row and every invalid one is discarded`() = runTest {
            val snapshot = runScenario(MockScenario.HAPPY_PATH)

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
            snapshot.rows.size shouldBe 32
            snapshot.discardedCount shouldBe 8
        }

        @Test
        fun `no row is left pending once the stream completes`() = runTest {
            val snapshot = runScenario(MockScenario.HAPPY_PATH)

            snapshot.rows.count { it.result == ResultState.Pending } shouldBe 0
        }
    }

    @Nested
    inner class Malformed {
        @Test
        fun `malformed numbers are discarded and their inputs never create rows`() = runTest {
            val snapshot = runScenario(MockScenario.MALFORMED)

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
            snapshot.rows.size shouldBe 5
            snapshot.discardedCount shouldBe 5
        }

        @Test
        fun `a null input fails its row instead of being counted as a discarded number`() = runTest {
            val snapshot = runScenario(MockScenario.MALFORMED)

            snapshot.rows.count {
                it.result == ResultState.Failed(FailureReason.MISSING_INPUT)
            } shouldBe 1
        }

        @Test
        fun `an input outside the supported range fails its row`() = runTest {
            val snapshot = runScenario(MockScenario.MALFORMED)

            snapshot.rows.count {
                it.result == ResultState.Failed(FailureReason.INVALID_INPUT)
            } shouldBe 1
        }
    }

    @Nested
    inner class Unbalanced {
        @Test
        fun `a stream that finishes early still completes with all its rows resolved`() = runTest {
            val snapshot = runScenario(MockScenario.UNBALANCED)

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
            snapshot.numbersReceived shouldBe 6
            snapshot.inputsReceived shouldBe 6
            snapshot.rows.count { it.result is ResultState.Ready } shouldBe 2
        }
    }

    @Nested
    inner class Large {
        @Test
        fun `the performance scenario resolves every result bearing row`() = runTest {
            val snapshot = runScenario(MockScenario.LARGE)

            snapshot.lifecycle shouldBe StreamLifecycle.Completed
            snapshot.rows.count { it.result is ResultState.Ready } shouldBe 16_000
            snapshot.discardedCount shouldBe 4_000
        }
    }

    private suspend fun TestScope.runScenario(scenario: MockScenario): StreamSnapshot {
        val definition = MockScenarios.definition(scenario)
        return StreamCoordinator(
            numbers = NumbersStreamEndpoint(MockNumbersDataSource(definition)),
            inputs = InputsStreamEndpoint(MockInputsDataSource(definition)),
            coordinatorDispatcher = StandardTestDispatcher(testScheduler),
            computationDispatcher = StandardTestDispatcher(testScheduler),
            // The simulated CPU cost of the real computer is irrelevant to stream shape.
            compute = { input -> input.toInt().mod(100).toString().padStart(2, '0') },
        ).snapshots().toList().last()
    }
}
