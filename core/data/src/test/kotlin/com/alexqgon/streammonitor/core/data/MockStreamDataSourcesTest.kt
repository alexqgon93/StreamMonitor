package com.alexqgon.streammonitor.core.data

import com.alexqgon.streammonitor.domain.NumberParser
import com.alexqgon.streammonitor.domain.ParseResult
import dagger.Lazy
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import javax.inject.Provider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MockStreamDataSourcesTest {
    @Nested
    inner class HappyPath {
        @Test
        fun `provides balanced streams with an eighty percent valid number ratio`() = runTest {
            val scenario = MockScenarios.definition(MockScenario.HAPPY_PATH)
            val numbers = MockNumbersDataSource(scenario).allValues()
            val inputs = MockInputsDataSource(scenario).allValues()

            numbers.size shouldBe 40
            inputs.size shouldBe 40
            numbers.count { NumberParser.parse(it) is ParseResult.Valid } shouldBe 32
            numbers.count { NumberParser.parse(it) is ParseResult.Invalid } shouldBe 8
        }

        @Test
        fun `a source that stopped mid stream does not leak its cursor into the next run`() = runTest {
            val scenario = MockScenarios.definition(MockScenario.HAPPY_PATH)
            val interrupted = MockNumbersDataSource(scenario)
            interrupted.fetch()
            interrupted.fetch()

            val restarted = MockNumbersDataSource(scenario)

            restarted.fetch() shouldBe scenario.numberBatches.first()
            restarted.allValues().size + scenario.numberBatches.first().size shouldBe 40
        }

        @Test
        fun `an exhausted source stays exhausted instead of silently replaying`() = runTest {
            val source = MockNumbersDataSource(MockScenarios.definition(MockScenario.HAPPY_PATH))
            source.allValues()

            source.fetch() shouldBe emptyList()
        }
    }

    @Nested
    inner class Malformed {
        @Test
        fun `contains null out of range and unsupported item values alongside valid values`() = runTest {
            val numbers = MockNumbersDataSource(
                MockScenarios.definition(MockScenario.MALFORMED),
            ).allValues()

            numbers shouldContain null
            numbers shouldContain 256
            numbers shouldContain 24
            numbers.count { NumberParser.parse(it) is ParseResult.Valid } shouldBe 5
            numbers.count { NumberParser.parse(it) is ParseResult.Invalid } shouldBe 5
        }

        @Test
        fun `two of its ten inputs are malformed and sit on result bearing numbers`() = runTest {
            val scenario = MockScenarios.definition(MockScenario.MALFORMED)
            val numbers = MockNumbersDataSource(scenario).allValues()
            val inputs = MockInputsDataSource(scenario).allValues()

            val malformedInputIndices = inputs.withIndex()
                .filter { (_, value) -> value == null || value !in 0..65_535 }
                .map { (index, _) -> index }

            malformedInputIndices.size shouldBe 2
            malformedInputIndices.all { index ->
                (NumberParser.parse(numbers[index]) as? ParseResult.Valid)?.needsResult == true
            } shouldBe true
        }
    }

    @Nested
    inner class Unbalanced {
        @Test
        fun `numbers finish after one batch while inputs continue in six batches`() = runTest {
            val scenario = MockScenarios.definition(MockScenario.UNBALANCED)
            val numbers = MockNumbersDataSource(scenario)
            val inputs = MockInputsDataSource(scenario)

            numbers.fetch().size shouldBe 6
            numbers.fetch() shouldContainExactly emptyList()
            (0..5).map { inputs.fetch().single() } shouldContainExactly listOf(0, 1, 2, 3, 4, 5)
            inputs.fetch() shouldContainExactly emptyList()
        }
    }

    @Nested
    inner class Large {
        @Test
        fun `provides twenty thousand values with an eighty percent valid number ratio`() = runTest {
            val scenario = MockScenarios.definition(MockScenario.LARGE)
            val numbers = MockNumbersDataSource(scenario).allValues()
            val inputs = MockInputsDataSource(scenario).allValues()

            numbers.size shouldBe 20_000
            inputs.size shouldBe 20_000
            numbers.count { NumberParser.parse(it) is ParseResult.Valid } shouldBe 16_000
            numbers.count { NumberParser.parse(it) is ParseResult.Invalid } shouldBe 4_000
        }
    }

    @Nested
    inner class ProviderWiring {
        @Test
        fun `each resolution yields an independent source so a new run never inherits a cursor`() =
            runTest {
                val config = StreamDataSourceConfig(
                    mode = StreamDataSourceMode.MOCK,
                    mockScenario = MockScenario.HAPPY_PATH,
                )
                val noHttpClient = Lazy<HttpClient> {
                    error("a mock source must never build an HTTP client")
                }

                val first = StreamDataSourceModule.provideNumbersDataSource(config, noHttpClient)
                first.fetch()
                val second = StreamDataSourceModule.provideNumbersDataSource(config, noHttpClient)

                second.allValues().size shouldBe 40
            }

        @Test
        fun `the coordinator factory resolves its endpoints once per run, not once per graph`() {
            val scenario = MockScenarios.definition(MockScenario.HAPPY_PATH)
            var numbersResolved = 0
            var inputsResolved = 0
            val factory = StreamDataSourceModule.provideStreamCoordinatorFactory(
                numbers = {
                    numbersResolved++
                    NumbersStreamEndpoint(MockNumbersDataSource(scenario))
                },
                inputs = {
                    inputsResolved++
                    InputsStreamEndpoint(MockInputsDataSource(scenario))
                },
            )

            factory.create()
            factory.create()

            numbersResolved shouldBe 2
            inputsResolved shouldBe 2
        }
    }

    private suspend fun NumbersDataSource.allValues(): List<Int?> = buildList {
        while (true) {
            val batch = fetch()
            if (batch.isEmpty()) return@buildList
            addAll(batch)
        }
    }

    private suspend fun InputsDataSource.allValues(): List<Int?> = buildList {
        while (true) {
            val batch = fetch()
            if (batch.isEmpty()) return@buildList
            addAll(batch)
        }
    }
}
