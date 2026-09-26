package com.alexqgon.streammonitor.domain

data class StreamRow(
    val sourceIndex: Int,
    val sectionIndex: Int,
    val itemIndex: Int,
    val result: ResultState,
)

data class StreamSnapshot(
    val lifecycle: StreamLifecycle,
    val rows: List<StreamRow>,
    val numbersReceived: Int,
    val inputsReceived: Int,
    val discardedCount: Int,
)

sealed interface StreamLifecycle {
    data object Idle : StreamLifecycle
    data object Streaming : StreamLifecycle
    data object Completed : StreamLifecycle
    data class Failed(val message: String) : StreamLifecycle
}
