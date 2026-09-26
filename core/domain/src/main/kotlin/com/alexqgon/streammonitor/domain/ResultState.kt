package com.alexqgon.streammonitor.domain

/**
 * The result state also exposes the deterministic secondary sort key used within an item.
 *
 * States are ordered as NotNeeded, Pending, Ready, Failed. NotNeeded is deliberately before
 * Ready, matching the example where "Item 4" precedes "Item 4 result=03". Pending and Failed
 * placement is an application assumption: unresolved rows come before computed rows, while a
 * failed computation is placed last.
 */
sealed interface ResultState {
    val sortOrder: Int

    data object NotNeeded : ResultState {
        override val sortOrder: Int = 0
    }

    data object Pending : ResultState {
        override val sortOrder: Int = 1
    }

    data class Ready(val value: String) : ResultState {
        override val sortOrder: Int = 2
    }

    data class Failed(val reason: FailureReason) : ResultState {
        override val sortOrder: Int = 3
    }
}

enum class FailureReason {
    MISSING_INPUT,
    INVALID_INPUT,
    COMPUTATION_FAILED,
}
