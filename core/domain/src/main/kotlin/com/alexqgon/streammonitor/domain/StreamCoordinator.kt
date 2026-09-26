package com.alexqgon.streammonitor.domain

import java.io.IOException
import java.util.TreeSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll

/**
 * Joins two independently advancing streams by their cumulative source index.
 *
 * Computation is lazy: a result is scheduled only after a valid number with its result bit
 * set and a valid input are both available. This avoids CPU work for rows that never display
 * a result, at the cost of waiting when the number and input arrive at different times.
 */
class StreamCoordinator(
    private val numbers: StreamEndpoint,
    private val inputs: StreamEndpoint,
    private val coordinatorDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val computationDispatcher: CoroutineDispatcher = DEFAULT_COMPUTATION_DISPATCHER,
    private val compute: (UShort) -> String = { ResultComputer.compute(it) },
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val backoffDelay: suspend (Long) -> Unit = { delay(it) },
) {
    fun snapshots(): Flow<StreamSnapshot> = channelFlow {
        val events = Channel<Event>(Channel.RENDEZVOUS)
        val pendingComputations = ArrayDeque<Computation>()
        val sections = Array(SECTION_COUNT) { TreeSet(rowComparator) }
        val rowsBySourceIndex = mutableMapOf<Int, StreamRow>()
        val parsedNumbers = mutableMapOf<Int, ParseResult>()
        val receivedInputs = mutableMapOf<Int, Int?>()
        val scheduled = mutableSetOf<Int>()
        var numberCursor = 0
        var inputCursor = 0
        var discardedCount = 0
        var lifecycle: StreamLifecycle = StreamLifecycle.Idle
        var numbersDone = false
        var inputsDone = false

        suspend fun emitSnapshot() {
            send(
                StreamSnapshot(
                    lifecycle = lifecycle,
                    rows = buildList {
                        sections.forEach(::addAll)
                    },
                    numbersReceived = numberCursor,
                    inputsReceived = inputCursor,
                    discardedCount = discardedCount,
                ),
            )
        }

        fun scheduleIfReady(index: Int) {
            val parsed = parsedNumbers[index] as? ParseResult.Valid ?: return
            if (!parsed.needsResult || index in scheduled) return

            if (index !in receivedInputs) {
                if (inputsDone) {
                    updateRow(
                        sections,
                        rowsBySourceIndex,
                        index,
                        ResultState.Failed(FailureReason.MISSING_INPUT),
                    )
                }
                return
            }

            when (val input = receivedInputs.getValue(index)) {
                null -> updateRow(
                    sections,
                    rowsBySourceIndex,
                    index,
                    ResultState.Failed(FailureReason.MISSING_INPUT),
                )

                !in 0..MAX_UNSIGNED_SHORT -> updateRow(
                    sections,
                    rowsBySourceIndex,
                    index,
                    ResultState.Failed(FailureReason.INVALID_INPUT),
                )

                else -> {
                    scheduled += index
                    updateRow(sections, rowsBySourceIndex, index, ResultState.Pending)
                    pendingComputations.addLast(Computation(index, input.toUShort()))
                }
            }
        }

        fun processNumbers(batch: List<Int?>) {
            batch.forEach { raw ->
                val index = numberCursor++
                val parsed = NumberParser.parse(raw)
                parsedNumbers[index] = parsed
                if (parsed is ParseResult.Invalid) {
                    discardedCount++
                    return@forEach
                }

                val validNumber = parsed as ParseResult.Valid
                val initialState =
                    if (validNumber.needsResult) ResultState.Pending else ResultState.NotNeeded
                insertRow(
                    sections,
                    rowsBySourceIndex,
                    StreamRow(index, validNumber.sectionIndex, validNumber.itemIndex, initialState),
                )
                scheduleIfReady(index)
            }
        }

        fun processInputs(batch: List<Int?>) {
            batch.forEach { input ->
                val index = inputCursor++
                receivedInputs[index] = input
                scheduleIfReady(index)
            }
        }

        fun resolveMissingInputs() {
            rowsBySourceIndex.values
                .asSequence()
                .filter { it.result == ResultState.Pending && it.sourceIndex !in receivedInputs }
                .map(StreamRow::sourceIndex)
                .toList()
                .forEach { index ->
                    updateRow(
                        sections,
                        rowsBySourceIndex,
                        index,
                        ResultState.Failed(FailureReason.MISSING_INPUT),
                    )
                }
        }

        suspend fun drainComputations(): Boolean {
            var scheduledWork = false
            while (pendingComputations.isNotEmpty()) {
                val batch = buildList {
                    repeat(minOf(MAX_COMPUTATION_PARALLELISM, pendingComputations.size)) {
                        add(pendingComputations.removeFirst())
                    }
                }
                val results = coroutineScope {
                    batch.map { computation ->
                        async(computationDispatcher) {
                            val result = try {
                                ResultState.Ready(compute(computation.input))
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                ResultState.Failed(FailureReason.COMPUTATION_FAILED)
                            }
                            ComputationResult(computation.index, result)
                        }
                    }.awaitAll()
                }
                results.forEach { result ->
                    updateRow(sections, rowsBySourceIndex, result.index, result.state)
                }
                scheduledWork = true
                emitSnapshot()
            }
            return scheduledWork
        }

        val numberJob = launch {
            pollUntilDone(StreamKind.NUMBERS, numbers, retryPolicy, backoffDelay, events)
        }
        val inputJob = launch {
            pollUntilDone(StreamKind.INPUTS, inputs, retryPolicy, backoffDelay, events)
        }
        try {
            emitSnapshot()
            lifecycle = StreamLifecycle.Streaming
            emitSnapshot()

            while (true) {
                when (val event = events.receive()) {
                    is Event.Batch -> {
                        when (event.kind) {
                            StreamKind.NUMBERS -> processNumbers(event.values)
                            StreamKind.INPUTS -> processInputs(event.values)
                        }
                        if (!drainComputations()) {
                            emitSnapshot()
                        }
                    }

                    is Event.EndpointFinished -> {
                        when (event.kind) {
                            StreamKind.NUMBERS -> numbersDone = true
                            StreamKind.INPUTS -> {
                                inputsDone = true
                                resolveMissingInputs()
                            }
                        }
                        if (numbersDone && inputsDone) {
                            check(pendingComputations.isEmpty())
                            lifecycle = StreamLifecycle.Completed
                            emitSnapshot()
                            return@channelFlow
                        }
                        emitSnapshot()
                    }

                    is Event.EndpointFailed -> {
                        lifecycle = StreamLifecycle.Failed(event.message)
                        emitSnapshot()
                        return@channelFlow
                    }
                }
            }
        } finally {
            numberJob.cancel()
            inputJob.cancel()
            events.cancel()
            numberJob.join()
            inputJob.join()
        }
    }.flowOn(coordinatorDispatcher).conflate()

    private companion object {
        const val SECTION_COUNT = 4
        const val MAX_UNSIGNED_SHORT = 65_535

        // Four concurrent computations bound CPU work to a small mobile-friendly budget.
        val DEFAULT_COMPUTATION_DISPATCHER =
            Dispatchers.Default.limitedParallelism(MAX_COMPUTATION_PARALLELISM)
    }
}

/**
 * Uses linear backoff because polling already bounds request frequency and needs predictable,
 * short recovery latency; exponential backoff is unnecessary for this small local retry budget.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialBackoffMillis: Long = 10,
) {
    init {
        require(maxAttempts > 0)
        require(initialBackoffMillis >= 0)
    }
}

private const val MAX_COMPUTATION_PARALLELISM = 4

private enum class StreamKind {
    NUMBERS,
    INPUTS,
}

private sealed interface Event {
    data class Batch(val kind: StreamKind, val values: List<Int?>) : Event
    data class EndpointFinished(val kind: StreamKind) : Event
    data class EndpointFailed(val message: String) : Event
}

private data class Computation(val index: Int, val input: UShort)
private data class ComputationResult(val index: Int, val state: ResultState)

private suspend fun pollUntilDone(
    kind: StreamKind,
    endpoint: StreamEndpoint,
    retryPolicy: RetryPolicy,
    delay: suspend (Long) -> Unit,
    events: Channel<Event>,
) {
    var failures = 0
    while (true) {
        try {
            val values = endpoint.poll()
            failures = 0
            if (values.isEmpty()) {
                events.send(Event.EndpointFinished(kind))
                return
            }
            events.send(Event.Batch(kind, values))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            failures++
            if (!failure.isRetryable() || failures >= retryPolicy.maxAttempts) {
                events.send(Event.EndpointFailed(failure.message ?: "Stream endpoint failed"))
                return
            }
            delay(retryPolicy.initialBackoffMillis * failures)
        }
    }
}

private fun Exception.isRetryable(): Boolean =
    this is IOException || (this is StreamEndpointException && retryable)

private val rowComparator = compareBy<StreamRow>(
    { it.itemIndex },
    { it.result.sortOrder },
    { it.sourceIndex },
)

private fun insertRow(
    sections: Array<TreeSet<StreamRow>>,
    rowsBySourceIndex: MutableMap<Int, StreamRow>,
    row: StreamRow,
) {
    sections[row.sectionIndex].add(row)
    rowsBySourceIndex[row.sourceIndex] = row
}

private fun updateRow(
    sections: Array<TreeSet<StreamRow>>,
    rowsBySourceIndex: MutableMap<Int, StreamRow>,
    sourceIndex: Int,
    result: ResultState,
) {
    val current = rowsBySourceIndex[sourceIndex] ?: return
    sections[current.sectionIndex].remove(current)
    insertRow(sections, rowsBySourceIndex, current.copy(result = result))
}
