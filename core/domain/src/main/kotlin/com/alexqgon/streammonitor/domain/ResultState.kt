package com.alexqgon.streammonitor.domain

sealed interface ResultState {
    data object NotNeeded : ResultState
    data object Pending : ResultState
    data class Ready(val value: String) : ResultState
    data class Failed(val reason: FailureReason) : ResultState
}

enum class FailureReason {
    MISSING_INPUT,
    INVALID_INPUT,
    COMPUTATION_FAILED,
}
