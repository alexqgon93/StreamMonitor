package com.alexqgon.streammonitor.feature.stream

import com.alexqgon.streammonitor.domain.FailureReason
import com.alexqgon.streammonitor.domain.ResultState
import com.alexqgon.streammonitor.domain.StreamLifecycle
import com.alexqgon.streammonitor.domain.StreamRow
import com.alexqgon.streammonitor.domain.StreamSnapshot
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StreamSnapshotMapperTest {
    @Nested
    inner class Lifecycle {
        @Test
        fun `idle snapshot maps to an empty state without a discarded summary`() {
            snapshot(lifecycle = StreamLifecycle.Idle).toUiState(StreamUiState()) shouldBe
                StreamUiState()
        }

        @Test
        fun `failed snapshot retains rows and exposes a failed lifecycle`() {
            val mapped = snapshot(
                lifecycle = StreamLifecycle.Failed("network"),
                rows = listOf(row(3, ResultState.Ready("03"))),
            ).toUiState(StreamUiState())

            mapped.lifecycle shouldBe StreamLifecycleUi.Failed
            mapped.hasSnapshot shouldBe true
            mapped.sections.single().rows.single().resultValue shouldBe "03"
        }
    }

    @Nested
    inner class Rows {
        @Test
        fun `rows group by their one based section number with all four visual states`() {
            val mapped = snapshot(
                lifecycle = StreamLifecycle.Streaming,
                rows = listOf(
                    row(0, ResultState.NotNeeded),
                    row(1, ResultState.Pending),
                    row(2, ResultState.Ready("48")),
                    row(3, ResultState.Failed(FailureReason.MISSING_INPUT)),
                ),
            ).toUiState(StreamUiState())

            mapped.sections.single().sectionNumber shouldBe 1
            mapped.sections.single().rows.map(StreamRowUi::state) shouldBe listOf(
                StreamRowStateUi.NotNeeded,
                StreamRowStateUi.Pending,
                StreamRowStateUi.Ready,
                StreamRowStateUi.Failed,
            )
            mapped.sections.single().rows.last().failure shouldBe StreamFailureUi.MissingInput
        }
    }

    @Nested
    inner class NewRun {
        @Test
        fun `a restart after a completed run clears both its rows and its counters`() {
            val completed = StreamUiState(
                lifecycle = StreamLifecycleUi.Completed,
                sections = listOf(
                    SectionUi(1, listOf(StreamRowUi(0, 1, 1, StreamRowStateUi.Ready, "03"))),
                ),
                numbersReceived = 40,
                inputsReceived = 40,
                discardedCount = 8,
                hasSnapshot = true,
            )

            val restarted = completed.startingNewRun()

            restarted.sections shouldBe emptyList()
            restarted.numbersReceived shouldBe 0
            restarted.inputsReceived shouldBe 0
            restarted.discardedCount shouldBe 0
        }

        @Test
        fun `a retry after a global failure keeps the cached rows but restarts the counters`() {
            val failed = failedRun()

            val retried = failed.startingNewRun()

            retried.sections shouldBe failed.sections
            retried.numbersReceived shouldBe 0
            retried.inputsReceived shouldBe 0
            retried.discardedCount shouldBe 0
        }

        @Test
        fun `cached rows stay visible until the retried run produces rows of its own`() {
            val retried = failedRun().startingNewRun()

            val mapped = snapshot(lifecycle = StreamLifecycle.Streaming).toUiState(retried)

            mapped.sections shouldBe retried.sections
        }

        @Test
        fun `counters reported while retrying belong to the new run, not to the failed one`() {
            val retried = failedRun().startingNewRun()

            val mapped = StreamSnapshot(
                lifecycle = StreamLifecycle.Streaming,
                rows = emptyList(),
                numbersReceived = 3,
                inputsReceived = 2,
                discardedCount = 3,
            ).toUiState(retried)

            mapped.numbersReceived shouldBe 3
            mapped.inputsReceived shouldBe 2
            mapped.discardedCount shouldBe 3
        }

        @Test
        fun `the first rows of the retried run replace the cached ones`() {
            val retried = failedRun().startingNewRun()

            val mapped = snapshot(
                lifecycle = StreamLifecycle.Streaming,
                rows = listOf(row(7, ResultState.Pending)),
            ).toUiState(retried)

            mapped.sections.single().rows.single().sourceIndex shouldBe 7
        }

        private fun failedRun() = StreamUiState(
            lifecycle = StreamLifecycleUi.Failed,
            sections = listOf(
                SectionUi(1, listOf(StreamRowUi(0, 1, 1, StreamRowStateUi.Ready, "03"))),
            ),
            numbersReceived = 12,
            inputsReceived = 9,
            discardedCount = 2,
            hasSnapshot = true,
        )
    }

    private fun snapshot(
        lifecycle: StreamLifecycle,
        rows: List<StreamRow> = emptyList(),
    ) = StreamSnapshot(
        lifecycle = lifecycle,
        rows = rows,
        numbersReceived = rows.size,
        inputsReceived = rows.size,
        discardedCount = 0,
    )

    private fun row(sourceIndex: Int, result: ResultState) = StreamRow(
        sourceIndex = sourceIndex,
        sectionIndex = 0,
        itemIndex = sourceIndex,
        result = result,
    )
}
