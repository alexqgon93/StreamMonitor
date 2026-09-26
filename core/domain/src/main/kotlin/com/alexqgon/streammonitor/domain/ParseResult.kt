package com.alexqgon.streammonitor.domain

sealed interface ParseResult {
    data class Valid(
        val sectionIndex: Int,
        val itemIndex: Int,
        val needsResult: Boolean,
    ) : ParseResult

    data class Invalid(val reason: InvalidReason) : ParseResult
}

enum class InvalidReason {
    ITEM_OUT_OF_RANGE,
    NULL_VALUE,
    OUT_OF_BYTE_RANGE,
}
