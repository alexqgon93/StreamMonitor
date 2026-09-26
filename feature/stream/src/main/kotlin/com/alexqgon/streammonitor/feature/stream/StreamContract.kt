package com.alexqgon.streammonitor.feature.stream

import androidx.compose.runtime.Immutable

@Immutable
data class StreamUiState(
    val lifecycle: StreamLifecycleUi = StreamLifecycleUi.Idle,
    val sections: List<SectionUi> = emptyList(),
    val numbersReceived: Int = 0,
    val inputsReceived: Int = 0,
    val discardedCount: Int = 0,
    val hasSnapshot: Boolean = false,
)

@Immutable
data class SectionUi(
    val sectionNumber: Int,
    val rows: List<StreamRowUi>,
)

@Immutable
data class StreamRowUi(
    val sourceIndex: Int,
    val sectionNumber: Int,
    val itemNumber: Int,
    val state: StreamRowStateUi,
    val resultValue: String = "",
    val failure: StreamFailureUi? = null,
)

enum class StreamLifecycleUi {
    Idle,
    Streaming,
    Completed,
    Failed,
}

enum class StreamRowStateUi {
    NotNeeded,
    Pending,
    Ready,
    Failed,
}

enum class StreamFailureUi {
    MissingInput,
    InvalidInput,
    ComputationFailed,
}

sealed interface StreamIntent {
    data object Start : StreamIntent
    data object Restart : StreamIntent
    data object Retry : StreamIntent
}
