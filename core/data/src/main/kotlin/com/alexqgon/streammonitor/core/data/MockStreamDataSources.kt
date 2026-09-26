package com.alexqgon.streammonitor.core.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Returns balanced streams with exactly 80% valid numbers and 20% invalid numbers.
 */
private fun happyPathScenario(): MockScenarioData {
    val numbers = List(40) { index ->
        if (index % 5 == 4) 24 else listOf(4, 12, 21, 136, 150, 16, 131)[index % 7]
    }
    return MockScenarioData(
        numberBatches = numbers.chunked(10),
        inputBatches = List(4) { batch -> List(10) { batch * 10 + it } },
    )
}

/**
 * Returns 50% valid numbers and 50% malformed entries: nulls, out-of-range values, and
 * item values outside the supported range.
 *
 * 80% of the inputs are usable and 20% are malformed: one `null` and one value above the
 * 16-bit range. Both malformed inputs are deliberately aligned with indices whose number is
 * valid *and* has the result bit set, so the scenario always produces exactly one
 * `Failed(MISSING_INPUT)` row and exactly one `Failed(INVALID_INPUT)` row. A malformed input
 * is a row-level failure and never a discarded number.
 */
private fun malformedScenario(): MockScenarioData = MockScenarioData(
    numberBatches = listOf(
        listOf(4, null, 24, 256, -1),
        listOf(150, 12, 255, 131, 16),
    ),
    inputBatches = listOf(
        listOf(0, 1, 2, 3, 4),
        // Index 5 pairs with 150 and index 8 pairs with 131; both numbers need a result.
        listOf(null, 6, 7, 70_000, 9),
    ),
)

/**
 * Returns a completed numbers stream after one six-value batch while inputs continue in six
 * one-value batches, deliberately making the input stream lag behind.
 *
 * All number values are valid so the scenario isolates stream imbalance from parse failures.
 */
private fun unbalancedScenario(): MockScenarioData = MockScenarioData(
    numberBatches = listOf(listOf(4, 12, 21, 136, 150, 16)),
    inputBatches = (0..5).map { listOf(it) },
)

/**
 * Returns 20,000 values in chunks with exactly 80% valid numbers and 20% invalid numbers.
 *
 * The deliberate 80/20 skew avoids the approximately 19% valid ratio of random bytes and
 * provides sufficient volume for the Phase 5 performance measurement.
 */
private fun largeScenario(): MockScenarioData {
    val numbers = List(LARGE_STREAM_SIZE) { index -> if (index % 5 == 4) 24 else 128 }
    val inputs = List(LARGE_STREAM_SIZE) { it % 65_536 }
    return MockScenarioData(
        numberBatches = numbers.chunked(LARGE_BATCH_SIZE),
        inputBatches = inputs.chunked(LARGE_BATCH_SIZE),
    )
}

internal data class MockScenarioData(
    val numberBatches: List<List<Int?>>,
    val inputBatches: List<List<Int?>>,
)

internal object MockScenarios {
    private val builders = mapOf(
        MockScenario.HAPPY_PATH to ::happyPathScenario,
        MockScenario.MALFORMED to ::malformedScenario,
        MockScenario.UNBALANCED to ::unbalancedScenario,
        MockScenario.LARGE to ::largeScenario,
    )

    // Scenario data is immutable, so it is generated once and shared by every run. The mutable
    // read cursor lives in the data source, which is created fresh for each run instead.
    private val cache = ConcurrentHashMap<MockScenario, MockScenarioData>()

    fun definition(scenario: MockScenario): MockScenarioData =
        cache.getOrPut(scenario) { builders.getValue(scenario).invoke() }
}

/**
 * Replays [MockScenarioData.numberBatches] once and then reports the stream as finished.
 *
 * Exhaustion is terminal: a new run gets a new instance with a fresh cursor, so restarting never
 * depends on a previous run having drained this one.
 */
internal class MockNumbersDataSource(
    scenario: MockScenarioData,
) : NumbersDataSource {
    private val batches = scenario.numberBatches
    private var cursor = 0

    override suspend fun fetch(): List<Int?> =
        if (cursor < batches.size) batches[cursor++] else emptyList()
}

/**
 * Replays [MockScenarioData.inputBatches] once and then reports the stream as finished.
 *
 * Exhaustion is terminal for the same reason as [MockNumbersDataSource].
 */
internal class MockInputsDataSource(
    scenario: MockScenarioData,
) : InputsDataSource {
    private val batches = scenario.inputBatches
    private var cursor = 0

    override suspend fun fetch(): List<Int?> =
        if (cursor < batches.size) batches[cursor++] else emptyList()
}

private const val LARGE_STREAM_SIZE = 20_000
private const val LARGE_BATCH_SIZE = 500
