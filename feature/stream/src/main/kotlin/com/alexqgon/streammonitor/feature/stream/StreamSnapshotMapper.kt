package com.alexqgon.streammonitor.feature.stream

import com.alexqgon.streammonitor.domain.FailureReason
import com.alexqgon.streammonitor.domain.ResultState
import com.alexqgon.streammonitor.domain.StreamLifecycle
import com.alexqgon.streammonitor.domain.StreamSnapshot

/**
 * Maps a domain snapshot onto the UI state of the **current** run.
 *
 * The three counters always come from the incoming snapshot. They report how many entries this run
 * has received, so inheriting them from a run that already finished would make the panel describe
 * a stream the user is no longer watching.
 *
 * Rows are the single exception: while a new run is streaming but has not produced any row yet, the
 * rows carried over by [startingNewRun] stay visible, so retrying after a global failure does not
 * blank the list before new values arrive. As soon as the run emits its own rows, they replace the
 * cached ones, and any terminal lifecycle stops the retention entirely.
 */
internal fun StreamSnapshot.toUiState(previous: StreamUiState): StreamUiState {
    val lifecycle = lifecycle.toUiLifecycle()
    val retainCachedRows = lifecycle == StreamLifecycleUi.Streaming &&
        rows.isEmpty() &&
        previous.sections.isNotEmpty()
    return StreamUiState(
        lifecycle = lifecycle,
        sections = if (retainCachedRows) previous.sections else rows.toSectionUi(),
        numbersReceived = numbersReceived,
        inputsReceived = inputsReceived,
        discardedCount = discardedCount,
        hasSnapshot = previous.hasSnapshot || lifecycle != StreamLifecycleUi.Idle,
    )
}

/**
 * Clears everything the finished run owns, immediately before a new coordinator starts.
 *
 * All three counters restart at zero. They count entries received by the current run, so after a
 * Restart or a Retry the previous totals describe nothing on screen; keeping them would show the
 * old run's figures and then jump backwards once the new run reported its first value.
 *
 * Rows survive only when the previous run ended in [StreamLifecycleUi.Failed]. A global failure must
 * not erase what was already received, so those rows stay on screen until the retry produces its
 * own. A Restart after a clean [StreamLifecycleUi.Completed] run has nothing worth preserving, so
 * its rows are dropped together with its counters.
 */
internal fun StreamUiState.startingNewRun(): StreamUiState = StreamUiState(
    lifecycle = lifecycle,
    sections = if (lifecycle == StreamLifecycleUi.Failed) sections else emptyList(),
    numbersReceived = 0,
    inputsReceived = 0,
    discardedCount = 0,
    hasSnapshot = hasSnapshot,
)

private fun StreamLifecycle.toUiLifecycle(): StreamLifecycleUi = when (this) {
    StreamLifecycle.Idle -> StreamLifecycleUi.Idle
    StreamLifecycle.Streaming -> StreamLifecycleUi.Streaming
    StreamLifecycle.Completed -> StreamLifecycleUi.Completed
    is StreamLifecycle.Failed -> StreamLifecycleUi.Failed
}

private fun List<com.alexqgon.streammonitor.domain.StreamRow>.toSectionUi(): List<SectionUi> =
    groupBy { it.sectionIndex }
        .map { (sectionIndex, rows) ->
            SectionUi(
                sectionNumber = sectionIndex + 1,
                rows = rows.map { row ->
                    val state = row.result
                    StreamRowUi(
                        sourceIndex = row.sourceIndex,
                        sectionNumber = row.sectionIndex + 1,
                        itemNumber = row.itemIndex + 1,
                        state = state.toUiRowState(),
                        resultValue = (state as? ResultState.Ready)?.value.orEmpty(),
                        failure = (state as? ResultState.Failed)?.reason?.toUiFailure(),
                    )
                },
            )
        }

private fun ResultState.toUiRowState(): StreamRowStateUi = when (this) {
    ResultState.NotNeeded -> StreamRowStateUi.NotNeeded
    ResultState.Pending -> StreamRowStateUi.Pending
    is ResultState.Ready -> StreamRowStateUi.Ready
    is ResultState.Failed -> StreamRowStateUi.Failed
}

private fun FailureReason.toUiFailure(): StreamFailureUi = when (this) {
    FailureReason.MISSING_INPUT -> StreamFailureUi.MissingInput
    FailureReason.INVALID_INPUT -> StreamFailureUi.InvalidInput
    FailureReason.COMPUTATION_FAILED -> StreamFailureUi.ComputationFailed
}
